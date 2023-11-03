package org.grakovne.snake

class GptStrategy {

    fun getMove(snake: Snake, field: Field, food: Food): Direction {
        require(snake.body.size < field.getWidth() * field.getHeight() - 1) {
            "No moves available, the snake has filled the entire field."
        }

        val availableMoves = Direction.values().filter { isValidMove(snake, field, it) }

        return if (availableMoves.isEmpty()) {
            // Если доступных ходов нет, выбираем любое доступное направление,
            // чтобы избежать застревания внутри хвоста
            val randomDirection = Direction.values().random()
            if (isValidMove(snake, field, randomDirection)) {
                randomDirection
            } else {
                // Если случайное направление недопустимо, выбираем первое доступное
                availableMoves.first()
            }
        } else {
            availableMoves.maxByOrNull { evaluateMove(simulateSnakeMove(snake, it), food, field) }!!
        }
    }

    private fun isValidMove(snake: Snake, field: Field, direction: Direction): Boolean {
        val head = snake.head()
        val (x, y) = when (direction) {
            Direction.UP -> Pair(head.first - 1, head.second)
            Direction.DOWN -> Pair(head.first + 1, head.second)
            Direction.LEFT -> Pair(head.first, head.second - 1)
            Direction.RIGHT -> Pair(head.first, head.second + 1)
        }

        return (x in 0 until field.getWidth() && y in 0 until field.getHeight() &&
                field.getCellType(x, y) != ElementType.BORDER &&
                field.getCellType(x, y) != ElementType.SNAKE &&
                Pair(x, y) !in snake.body)
    }

    private fun simulateSnakeMove(snake: Snake, direction: Direction): Snake {
        val newSnake = Snake(snake.head())
        newSnake.body.addAll(snake.body)
        newSnake.move(direction)
        return newSnake
    }

    private fun evaluateMove(snake: Snake, food: Food, field: Field): Int {
        val head = snake.head()
        val graph = Graph(field)
        val shortestPath = graph.findShortestPath(head, Pair(food.x, food.y))

        return if (food.x == head.first && food.y == head.second) {
            Int.MAX_VALUE
        } else if (shortestPath.isEmpty()) {
            0
        } else {
            field.getWidth() * field.getHeight() - shortestPath.size
        }
    }
}
