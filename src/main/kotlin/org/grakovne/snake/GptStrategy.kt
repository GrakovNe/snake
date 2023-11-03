package org.grakovne.snake

class GptStrategy {

    fun getMove(snake: Snake, field: Field, food: Food): Direction {
        val availableMoves = Direction.values().filter { isValidMove(snake, field, it) }

        if (availableMoves.isEmpty()) {
            return Direction.UP // Если нет доступных ходов, делаем что угодно, чтобы продолжить игру
        }

        val safeMoves = availableMoves.filter { isSafeMove(snake, field, it) }

        // Если нет безопасных ходов, выбираем ход, который максимизирует компактность
        if (safeMoves.isEmpty()) {
            return availableMoves.maxByOrNull { compactnessScore(simulateSnakeMove(snake, it), field) } ?: Direction.UP
        }

        // Если есть безопасные ходы, выбираем лучший на основе оценки хода
        return safeMoves.maxByOrNull { evaluateMove(simulateSnakeMove(snake, it), food, field) } ?: Direction.UP
    }

    private fun compactnessScore(snake: Snake, field: Field): Int {
        // Начинаем с максимально возможного счета, который будет уменьшаться за каждую свободную клетку рядом с змейкой
        var score = Int.MAX_VALUE

        for (segment in snake.body) {
            for (direction in Direction.values()) {
                val (dx, dy) = direction.toDelta()
                val newX = segment.first + dx
                val newY = segment.second + dy

                // Убедимся, что новая позиция находится в пределах поля
                if (newX in 0 until field.getWidth() && newY in 0 until field.getHeight()) {
                    // Уменьшаем счет, если рядом есть свободное пространство
                    if (field.getCellType(newX, newY) == ElementType.EMPTY) {
                        score--
                    }
                }
            }
        }

        // Возвращаем обратное значение, так как мы хотим максимизировать счет (чем меньше свободных клеток, тем лучше)
        return -score
    }

    // Вспомогательная функция для направления
    private fun Direction.toDelta(): Pair<Int, Int> = when (this) {
        Direction.UP -> Pair(-1, 0)
        Direction.DOWN -> Pair(1, 0)
        Direction.LEFT -> Pair(0, -1)
        Direction.RIGHT -> Pair(0, 1)
    }



    private fun isSafeMove(snake: Snake, field: Field, direction: Direction): Boolean {
        val simulatedSnake = simulateSnakeMove(snake, direction)
        val accessibleArea = bfsAccessibleArea(simulatedSnake.head().first, simulatedSnake.head().second, field, simulatedSnake)

        val longTermSurvivability = accessibleArea.any { area ->
            bfsAccessibleArea(area.first, area.second, field, simulatedSnake).size > (snake.body.size * 2) + 1
        }

        return accessibleArea.size > (snake.body.size * 2) + 1 && longTermSurvivability
    }

    private fun bfsAccessibleArea(startX: Int, startY: Int, field: Field, snake: Snake): Set<Pair<Int, Int>> {
        val visited = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()

        queue.add(startX to startY)
        visited.add(startX to startY)

        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()

            Direction.values().forEach { direction ->
                val (newX, newY) = when (direction) {
                    Direction.UP -> x - 1 to y
                    Direction.DOWN -> x + 1 to y
                    Direction.LEFT -> x to y - 1
                    Direction.RIGHT -> x to y + 1
                }

                if (newX in 0 until field.getWidth() &&
                    newY in 0 until field.getHeight() &&
                    field.getCellType(newX, newY) != ElementType.BORDER &&
                    field.getCellType(newX, newY) != ElementType.SNAKE &&
                    Pair(newX, newY) !in snake.body &&
                    Pair(newX, newY) !in visited
                ) {
                    queue.add(newX to newY)
                    visited.add(newX to newY)
                }
            }
        }

        return visited
    }
}

private fun isValidMove(snake: Snake, field: Field, direction: Direction): Boolean {
    val head = snake.head()
    val (x, y) = when (direction) {
        Direction.UP -> head.first - 1 to head.second
        Direction.DOWN -> head.first + 1 to head.second
        Direction.LEFT -> head.first to head.second - 1
        Direction.RIGHT -> head.first to head.second + 1
    }

    return (x in 0 until field.getWidth() && y in 0 until field.getHeight() &&
            field.getCellType(x, y) != ElementType.BORDER &&
            field.getCellType(x, y) != ElementType.SNAKE &&
            Pair(x, y) !in snake.body)
}

private fun simulateSnakeMove(snake: Snake, direction: Direction): Snake {
    val newSnake = Snake(snake.head())
    newSnake.body.addAll(snake.body.drop(1))
    newSnake.move(direction)
    return newSnake
}

private fun evaluateMove(snake: Snake, food: Food, field: Field): Int {
    val head = snake.head()
    val graph = Graph(field)
    val shortestPath = graph.findShortestPath(head, Pair(food.x, food.y), snake, field)

    return when {
        food.x == head.first && food.y == head.second -> Int.MAX_VALUE
        shortestPath.isEmpty() -> 0
        else -> field.getWidth() * field.getHeight() - shortestPath.size
    }
}
