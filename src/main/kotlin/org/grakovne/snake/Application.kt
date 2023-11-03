package org.grakovne.snake

import java.lang.RuntimeException

fun main(args: Array<String>) {
    val strategy = GptStrategy()
    val field = Field(80, 80)
    val uiKit = UIKit(80, 80)

    var snake = Snake(40 to 40)
    var food = Food(80, 80)


    field.update(snake, food)
    uiKit.showField(field)

    val minimalStepTime = 10

    gameLoop@ while (true) {
        snake = Snake(field.getRandomFreeCell())

        while (true) {
            val direction = strategy.getMove(snake, field, food)
            if (snake.willAteSelf(direction)) {
                throw RuntimeException("Bot is Dead")
            }

            snake.move(direction)

            if (food.x == snake.head().first && food.y == snake.head().second) {
                snake.grow()
                do {
                    food = Food(80, 80)
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