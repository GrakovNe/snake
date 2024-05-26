package org.grakovne.snake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.grakovne.snake.neural.GptStrategy

class SnakeRunner(private val size: Int, private val weights: List<Double>) {
    private val strategy = GptStrategy().apply { setWeights(weights) }
    private val field = Field(size, size)
    private val snake = Snake(BodyItem(1, 1))
    private var food = Food(size, size)

    init {
        field.update(snake, food)
    }

    fun run(): Flow<Pair<Field, Snake>> = flow {
        var direction: Direction? = null

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
                } while (snake.body.contains(BodyItem(food.x, food.y)))
            }



            if (isBorderCell(snake.head(), field) || isBeyondCell(snake.head(), field)) {
                throw RuntimeException("Bot is Dead")
            }

            field.update(snake, food)
            emit(Pair(field, snake))
        }
    }

    private fun isBeyondCell(cell: BodyItem, field: Field) =
        field.getCells().none { it.second.let { BodyItem(it.first, it.second) } == cell }

    private fun isBorderCell(cell: BodyItem, field: Field) =
        field.getCellType(cell.first, cell.second) == ElementType.BORDER
}
