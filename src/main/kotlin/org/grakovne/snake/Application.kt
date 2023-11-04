package org.grakovne.snake

import org.grakovne.snake.neural.GptStrategy
import java.lang.RuntimeException

fun main(args: Array<String>) {
    val size = 15
    val strategy = GptStrategy()
    val field = Field(size, size)
    val uiKit = UIKit(size, size)

    var snake = Snake(1 to 1)
    var food = Food(size, size)


    field.update(snake, food)
    uiKit.showField(field)

    val minimalStepTime = 10

    gameLoop@ while (true) {
        snake = Snake(field.getRandomFreeCell())
        var direction: Direction = strategy.getMove(snake, field, food, null)

        while (true) {
            direction = strategy.getMove(snake, field, food, direction)

            if (snake.willAteSelf(direction)) {
                throw RuntimeException("Bot is Dead")
            }

            snake.move(direction)

            if (food.x == snake.head().first && food.y == snake.head().second) {
                snake.grow()
                do {
                    food = Food(size, size)
                } while (snake.body.contains(food.x to food.y))
            }

            val startTime = System.currentTimeMillis()

            field.update(snake, food)
            uiKit.showField(field)

            val elapsedTime = System.currentTimeMillis() - startTime

            if (elapsedTime < minimalStepTime) {
                Thread.sleep(minimalStepTime - elapsedTime)
            }

            if (isBorderCell(snake.head(), field) || isBeyondCell(snake.head(), field)) {
                continue@gameLoop
            }
        }
    }
}

fun isBeyondCell(cell: Pair<Int, Int>, field: Field) = field.getCells().none { it.second == cell }
fun isBorderCell(cell: Pair<Int, Int>, field: Field) = field.getCellType(cell.first, cell.second) == ElementType.BORDER