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
    val size = 10
    val totalGames = 15
    val populationSize = 20
    val generations = 1000 // Увеличено количество поколений
    val mutationRate = 0.05

    val baseWeights: List<Double> = listOf(
        2.0,
        1.5,
        1.0,
        2.0,
        3.0
    )

    val series = XYSeries("Best Snake Length")
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
            var population = initializePopulation(populationSize, baseWeights)
            var globalBestFitness = 0.0
            var globalBestIndividual: Individual? = null
            var adaptiveMutationRate = mutationRate

            for (generation in 0 until generations) {
                println("Generation: $generation")

                // Оценка фитнеса
                population.forEachIndexed { index, individual ->
                    individual.fitness = evaluateFitness(individual.weights, size, totalGames)
                    println("Individual $index: Fitness = ${individual.fitness}, Weights = ${individual.weights}")
                }

                // Диагностика диапазона фитнеса
                val minFitness = population.minOf { it.fitness }
                val maxFitness = population.maxOf { it.fitness }
                val avgFitness = population.map { it.fitness }.average()
                println("Min Fitness = $minFitness, Max Fitness = $maxFitness, Avg Fitness = $avgFitness")

                // Обновление глобального лучшего индивида
                if (population.first().fitness > globalBestFitness) {
                    globalBestFitness = population.first().fitness
                    globalBestIndividual = population.first()
                    println("New global best fitness: $globalBestFitness")
                } else {
                    println("No improvement in this generation. Best fitness remains: $globalBestFitness")
                }

                // Обновление графика
                SwingUtilities.invokeLater {
                    series.add(generation.toDouble(), globalBestFitness)
                }

                // Адаптивная мутация при замедлении прогресса
                if (generation > 100 && generation % 50 == 0) {
                    adaptiveMutationRate = (adaptiveMutationRate + 0.01).coerceAtMost(0.2)
                    println("Increasing mutation rate to $adaptiveMutationRate")
                }

                // Создание нового поколения
                val newPopulation = mutableListOf<Individual>()

                // Элитизм: сохраняем глобально лучшего индивида
                newPopulation.add(globalBestIndividual ?: population.first())

                // Генерация новых индивидов через кроссовер и мутацию
                while (newPopulation.size < populationSize) {
                    val parent1 = selectParent(population)
                    val parent2 = selectParent(population)
                    val child = crossover(parent1, parent2)
                    mutate(child, adaptiveMutationRate)
                    newPopulation.add(child)
                }

                // Замена популяции
                val newPopulationFitness = newPopulation.map { it.fitness }.average()
                if (newPopulationFitness >= avgFitness) {
                    population = newPopulation
                    println("New generation accepted. Average fitness: $newPopulationFitness")
                } else {
                    println("New generation rejected. Keeping previous generation.")
                }
            }

            // Сохранение лучшего индивида
            globalBestIndividual?.let {
                println("Best individual weights after $generations generations: ${it.weights}")

                val file = File("best_individual_weights.txt")
                FileWriter(file, true).use { writer ->
                    writer.write("Best individual weights after $generations generations: ${it.weights}\n")
                }
            }
        }
    }
}

fun createChart(dataset: XYSeriesCollection): JFreeChart {
    return ChartFactory.createXYLineChart(
        "Best Snake Length per Generation",
        "Generation",
        "Best Length",
        dataset,
        PlotOrientation.VERTICAL,
        true,
        true,
        false
    )
}

fun initializePopulation(populationSize: Int, baseWeights: List<Double>): MutableList<Individual> {
    val population = mutableListOf<Individual>()
    for (i in 0 until populationSize) {
        val weights = List(baseWeights.size) { Random.nextDouble(0.0, 3.0) }
        population.add(Individual(weights))
        println("Initialized individual $i with weights $weights")
    }
    return population
}

fun evaluateFitness(weights: List<Double>, size: Int, totalGames: Int): Double {
    val strategy = GptStrategy()
    strategy.setWeights(weights)

    val results = DoubleArray(totalGames)
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
                    results[gameIndex] = snake.body.size.toDouble()
                    println("Game $gameIndex: Snake died with length ${snake.body.size}")
                    break@gameLoop
                }

                snake.move(direction)

                val currentState = snake.body.toList()
                if (stateHistory.contains(currentState) || steps > maxSteps) {
                    results[gameIndex] = snake.body.size.toDouble()
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

fun selectParent(population: List<Individual>): Individual {
    val tournamentSize = 5
    val tournament = List(tournamentSize) { population[Random.nextInt(population.size)] }
    return tournament.maxByOrNull { it.fitness }!!
}

fun crossover(parent1: Individual, parent2: Individual): Individual {
    val crossoverPoint = Random.nextInt(parent1.weights.size)
    val childWeights = parent1.weights.subList(0, crossoverPoint) + parent2.weights.subList(crossoverPoint, parent2.weights.size)
    return Individual(childWeights)
}

fun mutate(individual: Individual, mutationRate: Double) {
    val newWeights = individual.weights.toMutableList()
    newWeights.indices.forEach { i ->
        if (Random.nextDouble() < mutationRate) {
            val mutationAmount = Random.nextDouble(-1.0, 1.0)
            newWeights[i] = (newWeights[i] + mutationAmount).coerceIn(0.0, 10.0)
        }
    }
    individual.weights = newWeights
}

fun isOutOfBounds(head: BodyItem, field: Field): Boolean {
    return head.first !in 0 until field.getWidth() || head.second !in 0 until field.getHeight()
}
