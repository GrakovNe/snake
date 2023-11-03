package org.grakovne.snake

fun main(args: Array<String>) {

    val strategy = GptStrategy()

    var food = Food(80, 80)
    val field = Field(80, 80)
    val uiKit = UIKit(80, 80)

    var snake = Snake(40 to 40)

    while (food.x == 40 && food.y == 40) {
        food = Food(80, 80)
    }

    field.update(snake, food)
    uiKit.showField(field)


    while (true) {
        snake = Snake(field.getRandomFreeCell())

        while (true) {
            val direction = strategy.getMove(snake, field, food)

            if (snake.willAteSelf(direction)) {
                break
            }

            snake.move(direction)

            if (food.x == snake.head().first && food.y == snake.head().second) {
                snake.grow()
                food = Food(80, 80)
            }

            if (isBorderCell(snake.head(), field) || isBeyondCell(snake.head(), field)) {
                field.update(snake, food)
                uiKit.showField(field)

                break
            }

            field.update(snake, food)
            uiKit.showField(field)

            //Thread.sleep(5)
        }
    }
}

fun isBeyondCell(cell: Pair<Int, Int>, field: Field) = field.getCells().none { it.second == cell }

fun isBorderCell(cell: Pair<Int, Int>, field: Field): Boolean {
    return field.getCellType(cell.first, cell.second) == ElementType.BORDER
}