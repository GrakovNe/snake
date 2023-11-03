package org.grakovne.snake


class GptStrategy {

    fun getMove(snake: Snake, field: Field, food: Food): Direction {
        if (snake.body.size == field.getWidth() * field.getHeight() - 1) {
            // The snake has filled the entire field, game over.
            throw IllegalStateException("No moves available, the snake has filled the entire field.")
        }

        return moveToFood(snake, field, food)
    }

    fun moveToFood(snake: Snake, field: Field, food: Food): Direction {
        return getBestMove(snake, field, food, 0)?.first ?: throw IllegalStateException("No moves available")
    }

    private fun getBestMove(snake: Snake, field: Field, food: Food, depth: Int): Pair<Direction, Int>? {
        if (depth == 11) return null

        return Direction
            .values()
            .asSequence()
            .filter { direction -> !snake.willAteSelf(direction) && !willCollideWithBorder(snake, field, direction) }
            .mapNotNull { direction ->
                val simulatedSnake = simulateSnakeMove(snake, direction)
                val score = evaluateMove(simulatedSnake, food, field)

                val nextMove = getBestMove(simulatedSnake, field, food, depth + 1)
                val nextScore = nextMove?.second ?: 0

                if (isDeadEnd(simulatedSnake, field)) null else direction to (score + nextScore)
            }
            .maxByOrNull { it.second }
    }

    private fun simulateSnakeMove(snake: Snake, direction: Direction): Snake {
        val newSnake = Snake(snake.head())
        newSnake.body.addAll(snake.body)
        newSnake.move(direction)
        return newSnake
    }

    private fun evaluateMove(snake: Snake, food: Food, field: Field): Int {
        val head = snake.head()
        val foodDistance = Math.abs(food.x - head.first) + Math.abs(food.y - head.second)
        return (field.getWidth() * field.getHeight()) - foodDistance // A simple heuristic
    }

    private fun willCollideWithBorder(snake: Snake, field: Field, direction: Direction): Boolean {
        val head = snake.head()
        return when (direction) {
            Direction.UP -> field.getCellType(head.first - 1, head.second) == ElementType.BORDER
            Direction.DOWN -> field.getCellType(head.first + 1, head.second) == ElementType.BORDER
            Direction.LEFT -> field.getCellType(head.first, head.second - 1) == ElementType.BORDER
            Direction.RIGHT -> field.getCellType(head.first, head.second + 1) == ElementType.BORDER
        }
    }

    private fun isDeadEnd(snake: Snake, field: Field): Boolean {
        val head = snake.head()
        val directions = listOf(
            head.first - 1 to head.second,
            head.first + 1 to head.second,
            head.first to head.second - 1,
            head.first to head.second + 1
        )

        return directions.count { (x, y) ->
            field.getCellType(x, y) != ElementType.BORDER && (x to y !in snake.body)
        } <= 1
    }

}