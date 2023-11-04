package org.grakovne.snake

import org.grakovne.snake.neural.GptStrategy
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


fun main(args: Array<String>) {
    val size = 60
    val strategy = GptStrategy()
    val totalGames = 5
    val results = IntArray(totalGames)

    val executorService = Executors.newWorkStealingPool()

    for (gameIndex in 0 until totalGames) {
        executorService.submit {
            val field = Field(size, size)
            val snake = Snake(BodyItem(1, 1))
            var food = Food(size, size)

            field.update(snake, food)

            var direction: Direction = strategy.getMove(snake, field, food, null)

            gameLoop@ while (true) {
                direction = strategy.getMove(snake, field, food, direction)

                if (snake.willAteSelf(direction)) {
                    results[gameIndex] = snake.body.size
                    println("Yet another snake is dead. Score: ${snake.body.size}")
                    break@gameLoop
                }

                snake.move(direction)

                if (food.x == snake.head().first && food.y == snake.head().second) {
                    snake.grow()
                    do {
                        food = Food(size, size)
                    } while (snake.body.contains(BodyItem(food.x, food.y)))
                }

                field.update(snake, food)

                if (isBorderCell(snake.head(), field) || isBeyondCell(snake.head(), field)) {
                    continue@gameLoop
                }

            }
        }
    }

    executorService.shutdown() // Завершаем работу пула потоков
    try {
        executorService.awaitTermination(
            Long.MAX_VALUE,
            TimeUnit.NANOSECONDS
        ) // Ждем, пока все задачи не будут выполнены
    } catch (e: InterruptedException) {
        println("Threads interrupted: ${e.message}")
    }

    val sortedResults = results.sorted()
    val tenPercentCount = totalGames / 10
    val filteredResults = sortedResults.drop(tenPercentCount).dropLast(tenPercentCount)

    val averageLength = filteredResults.average()
    val bestLength = filteredResults.maxOrNull() ?: 0
    val worstLength = filteredResults.minOrNull() ?: 0

    println("After $totalGames, average snake length: $averageLength, best length: $bestLength, worst length: $worstLength")

}
