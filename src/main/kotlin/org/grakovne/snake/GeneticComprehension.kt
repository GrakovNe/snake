package org.grakovne.snake

import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import java.io.File
import java.io.FileWriter
import javax.swing.JFrame
import javax.swing.SwingUtilities
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

data class Individual(var weights: List<Double>, var fitness: Double = 0.0)

fun main() {
    val size = 20
    val totalGames = 10
    val populationSize = 50
    val generations = 200000
    val mutationRate = 0.2
    val elitismCount = 3

    val baseWeights: List<Double> = listOf(
        1.396377081088321,
        1.8421232025250636,
        1.3401581518527927,
        2.9250468744771045
    )

    val series = XYSeries("Average Length")
    val dataset = XYSeriesCollection(series)
    val chart = createChart(dataset)
    val chartPanel = ChartPanel(chart)

    SwingUtilities.invokeLater {
        val frame = JFrame("Snake Evolution Progress")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.contentPane.add(chartPanel)
        frame.pack()
        frame.isVisible = true

        Executors.newSingleThreadExecutor().submit {
            // Инициализация популяции
            var population = initializePopulation(populationSize, baseWeights)
            var bestAverageLength = 0.0
            var bestIndividual: Individual? = null

            for (generation in 0 until generations) {
                println("Generation: $generation")

                // Оценка фитнес-функции
                population.forEachIndexed { index, individual ->
                    individual.fitness = evaluateFitness(individual.weights, size, totalGames)
                    println("Individual $index: Fitness = ${individual.fitness}, Weights = ${individual.weights}")
                }

                // Сортировка по фитнесу
                population = population.sortedByDescending { it.fitness }.toMutableList()

                // Вывод лучших результатов текущего поколения
                println("Best fitness: ${population.first().fitness}, Best weights: ${population.first().weights}")

                if (population.all { it.fitness == 0.0 }) {
                    println("All individuals died. Restarting current population.")
                    population.forEach { it.fitness = 0.0 }
                    continue
                }

                val newPopulation = mutableListOf<Individual>()

                // Элитизм: сохранение лучших индивидуумов
                for (i in 0 until elitismCount) {
                    newPopulation.add(population[i])
                    println("Elitism: Preserving individual $i with fitness ${population[i].fitness}")
                }

                // Кроссовер и мутация для создания новых индивидуумов
                while (newPopulation.size < populationSize) {
                    val parent1 = selectParent(population)
                    val parent2 = selectParent(population)
                    val child = crossover(parent1, parent2)
                    mutate(child, mutationRate)
                    newPopulation.add(child)
                    println("New child created: Weights = ${child.weights}")
                }

                val averageLength = newPopulation.map { it.fitness }.average()
                println("Average length of new generation: $averageLength")

                if (averageLength > bestAverageLength) {
                    population = newPopulation
                    bestAverageLength = averageLength
                    bestIndividual = population.first()
                    println("New generation is better or equal to the previous one. Accepting new generation.")
                } else {
                    println("New generation is worse than the previous one. Retrying with the same generation.")
                }

                SwingUtilities.invokeLater {
                    series.add(generation, bestAverageLength)
                }
            }

            bestIndividual?.let {
                println("Best individual weights after $generations generations: ${it.weights}")

                val file = File("best_individual_weights.txt")
                val fileWriter = FileWriter(file, true) // true для добавления в конец файла
                fileWriter.write("Best individual weights after $generations generations: ${it.weights}\n")
                fileWriter.close()
            }
        }
    }
}

fun createChart(dataset: XYSeriesCollection): JFreeChart {
    return ChartFactory.createXYLineChart(
        "Average Snake Length per Generation",
        "Generation",
        "Average Length",
        dataset,
        PlotOrientation.VERTICAL,
        true,
        true,
        false
    )
}

// Инициализация популяции случайными весами
fun initializePopulation(populationSize: Int, baseWeights: List<Double>): MutableList<Individual> {
    val population = mutableListOf<Individual>()
    for (i in 0 until populationSize) {
        val weights = baseWeights.takeIf { it.isNotEmpty() } ?: List(4) { Random.nextDouble(0.0, 3.0) }
        population.add(Individual(weights))
        println("Initialized individual $i with weights $weights")
    }
    return population
}

// Оценка фитнес-функции
fun evaluateFitness(weights: List<Double>, size: Int, totalGames: Int): Double {
    val strategy = GptStrategy()
    strategy.setWeights(weights)

    val results = IntArray(totalGames)
    val executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    for (gameIndex in 0 until totalGames) {
        executorService.submit {
            val field = Field(size, size)
            val snake = Snake(BodyItem(1, 1))
            var food = Food(size, size)

            field.update(snake, food)

            var direction: Direction = strategy.getMove(snake, field, food, null)
            val stateHistory = mutableSetOf<List<BodyItem>>()
            var steps = 0
            val maxSteps = size * size * 2

            gameLoop@ while (true) {
                direction = strategy.getMove(snake, field, food, direction)

                if (snake.willAteSelf(direction) || isOutOfBounds(snake.head(), field)) {
                    results[gameIndex] = snake.body.size
                    println("Game $gameIndex: Snake died with length ${snake.body.size}")
                    break@gameLoop
                }

                snake.move(direction)

                val currentState = snake.body.toList()
                if (stateHistory.contains(currentState) || steps > maxSteps) {
                    results[gameIndex] = snake.body.size
                    println("Game $gameIndex: Snake looped or took too long with length ${snake.body.size}")
                    break@gameLoop
                }
                stateHistory.add(currentState)
                steps++

                if (food.x == snake.head().first && food.y == snake.head().second) {
                    snake.grow()
                    do {
                        food = Food(size, size)
                    } while (snake.body.contains(BodyItem(food.x, food.y)))
                }

                field.update(snake, food)
            }
        }
    }

    executorService.shutdown()
    executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)

    val averageLength = results.average()
    println("Average snake length: $averageLength")
    return averageLength
}

// Селекция родителя для кроссовера
fun selectParent(population: List<Individual>): Individual {
    val tournamentSize = 5
    val tournament = List(tournamentSize) { population[Random.nextInt(population.size)] }
    val selectedParent = tournament.maxByOrNull { it.fitness }!!
    println("Selected parent with fitness ${selectedParent.fitness} for crossover")
    return selectedParent
}

// Кроссовер двух родителей для создания нового индивидума
fun crossover(parent1: Individual, parent2: Individual): Individual {
    val crossoverPoint = Random.nextInt(parent1.weights.size)
    val childWeights =
        parent1.weights.subList(0, crossoverPoint) + parent2.weights.subList(crossoverPoint, parent2.weights.size)
    println("Crossover point: $crossoverPoint, Parent1: ${parent1.weights}, Parent2: ${parent2.weights}, Child: $childWeights")
    return Individual(childWeights)
}

// Мутация индивидума
fun mutate(individual: Individual, mutationRate: Double) {
    val newWeights = individual.weights.toMutableList()
    newWeights.indices.forEach { i ->
        if (Random.nextDouble() < mutationRate) {
            newWeights[i] = Random.nextDouble(0.0, 10.0)
            println("Mutated gene $i to ${newWeights[i]}")
        }
    }
    individual.weights = newWeights
}

// Проверка выхода за пределы поля
fun isOutOfBounds(head: BodyItem, field: Field): Boolean {
    return head.first !in 0 until field.getWidth() || head.second !in 0 until field.getHeight()
}
