package org.grakovne.snake

class GptStrategy {

    fun getMove(snake: Snake, field: Field, food: Food): Direction {
        val availableMoves = Direction.values().filter { isValidMove(snake, field, it) }
        val safeMoves = availableMoves.filter { isSafeMove(snake, field, it) }

        return if (safeMoves.isEmpty()) {
            availableMoves.randomOrNull() ?: Direction.UP
        } else {
            safeMoves.maxByOrNull { evaluateMove(simulateSnakeMove(snake, it), food, field) } ?: Direction.UP
        }
    }

    private fun isSafeMove(snake: Snake, field: Field, direction: Direction): Boolean {
        val simulatedSnake = simulateSnakeMove(snake, direction)
        val accessibleArea = bfsAccessibleArea(simulatedSnake.head().first, simulatedSnake.head().second, field, simulatedSnake)

        val longTermSurvivability = accessibleArea.any { area ->
            bfsAccessibleArea(area.first, area.second, field, simulatedSnake).size > simulatedSnake.body.size
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
