package org.grakovne.snake

import kotlinx.coroutines.*
import org.grakovne.snake.neural.GptStrategy
import java.lang.RuntimeException

fun main(args: Array<String>) = runBlocking {
    val size = 60
    val strategy = GptStrategy()
    val totalGames = 5
    val results = IntArray(totalGames)
    val jobs = List(totalGames) { gameIndex ->
        launch {

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

    jobs.forEach { it.join() } // Дождаться окончания всех корутин

    val averageLength = results.average()
    val bestLength = results.maxOrNull() ?: 0
    val worstLength = results.minOrNull() ?: 0

    println("After $totalGames games, average snake length: $averageLength, best length: $bestLength, worst length: $worstLength")
}

// Helper functions and any other classes like Snake, Field, Food, etc.
// должны быть определены здесь или в своих соответствующих файлах.
