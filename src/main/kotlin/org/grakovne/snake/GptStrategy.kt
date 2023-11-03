package org.grakovne.snake

class GptStrategy {

    fun getMove(snake: Snake, field: Field, food: Food): Direction {
        val availableMoves = Direction.values().filter { isValidMove(snake, field, it) }

        // Фильтруем доступные ходы, исключая те, которые создают замкнутое пространство
        val safeMoves = availableMoves.filter { isSafeMove(snake, field, it) }

        return if (safeMoves.isEmpty()) {
            availableMoves.randomOrNull() ?: Direction.UP // Если нет безопасных ходов, выбираем случайно из доступных
        } else {
            safeMoves.maxByOrNull { evaluateMove(simulateSnakeMove(snake, it), food, field) } ?: Direction.UP
        }
    }

    // Функция для проверки, не создаёт ли ход замкнутое пространство
    private fun isSafeMove(snake: Snake, field: Field, direction: Direction): Boolean {
        val simulatedSnake = simulateSnakeMove(snake, direction)
        val head = simulatedSnake.head()

        // Запускаем BFS, чтобы увидеть, сколько клеток доступно после хода
        val accessibleArea = bfsAccessibleArea(head, field, simulatedSnake)

        return accessibleArea.size > (snake.body.size * 2) + 1
    }

    // BFS для вычисления размера доступной зоны
    private fun bfsAccessibleArea(start: Pair<Int, Int>, field: Field, snake: Snake): Set<Pair<Int, Int>> {
        val visited = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()

        queue.add(start)
        visited.add(start)

        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()

            Direction.values().forEach { direction ->
                val (newX, newY) = when (direction) {
                    Direction.UP -> Pair(x - 1, y)
                    Direction.DOWN -> Pair(x + 1, y)
                    Direction.LEFT -> Pair(x, y - 1)
                    Direction.RIGHT -> Pair(x, y + 1)
                }

                if (newX in 0 until field.getWidth() &&
                    newY in 0 until field.getHeight() &&
                    field.getCellType(newX, newY) != ElementType.BORDER &&
                    field.getCellType(newX, newY) != ElementType.SNAKE &&
                    Pair(newX, newY) !in snake.body &&
                    Pair(newX, newY) !in visited
                ) {

                    queue.add(Pair(newX, newY))
                    visited.add(Pair(newX, newY))
                }
            }
        }

        return visited
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

