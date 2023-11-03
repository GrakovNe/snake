package org.grakovne.snake

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

class GptStrategy {

    private val deltaMap = Direction.values().associateWith { it.toDelta() }

    private val executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    fun getMove(snake: Snake, field: Field, food: Food): Direction {
        // Получаем все возможные ходы, которые валидны
        val availableMoves = Direction.values().filter { isValidMove(snake, field, it) }

        if (availableMoves.isEmpty()) {
            return Direction.UP // Нет доступных ходов
        }

        // Подготавливаем задачи для проверки безопасности каждого хода
        val moveChecks = availableMoves.map { direction ->
            Callable { Pair(direction, isSafeMove(snake, field, direction)) }
        }

        // Запускаем все проверки параллельно
        val futures = moveChecks.map { executorService.submit(it) }

        // Дожидаемся результатов всех задач
        val safeMoves = futures.mapNotNull { future ->
            try {
                future.get()?.takeIf { it.second }?.first // Получаем только те направления, где ход безопасен
            } catch (e: InterruptedException) {
                // На случай, если выполнение было прервано
                Thread.currentThread().interrupt()
                null
            } catch (e: Exception) {
                null
            }
        }

        // Выбираем лучший ход на основе полученных результатов
        return when {
            safeMoves.isNotEmpty() ->
                safeMoves.maxByOrNull { direction ->
                    evaluateMove(simulateSnakeMove(snake, direction), food, field)
                } ?: Direction.UP
            else ->
                availableMoves.maxByOrNull { direction ->
                    compactnessScore(simulateSnakeMove(snake, direction), field)
                } ?: Direction.UP
        }
    }

    private fun filterSafeMoves(futures: List<Future<Pair<Direction, Boolean>>>): List<Direction> {
        return futures.mapNotNull {
            try {
                val (direction, isSafe) = it.get() // Блокирующий вызов
                if (isSafe) direction else null
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun getBestMoveByEvaluation(
        safeMoves: List<Direction>,
        snake: Snake,
        field: Field,
        food: Food
    ): Direction {
        return safeMoves.maxByOrNull { direction ->
            evaluateMove(simulateSnakeMove(snake, direction), food, field)
        } ?: Direction.UP
    }

    private fun getBestMoveByCompactness(
        availableMoves: List<Direction>,
        snake: Snake,
        field: Field
    ): Direction {
        return availableMoves.maxByOrNull { direction ->
            compactnessScore(simulateSnakeMove(snake, direction), field)
        } ?: Direction.UP
    }

    private fun compactnessScore(snake: Snake, field: Field): Int {
        var score = 0

        snake.body.forEach { segment ->
            deltaMap.values.forEach { (dx, dy) ->
                val newX = segment.first + dx
                val newY = segment.second + dy

                if (field.isInBounds(newX, newY) && field.isEmpty(newX, newY)) {
                    score--
                }
            }
        }

        return score
    }


    // Здесь предполагается, что у Field есть методы isInBounds и isEmpty, чтобы упростить код
    private fun Field.isInBounds(x: Int, y: Int) = x in 0 until this.getWidth() && y in 0 until this.getHeight()
    private fun Field.isEmpty(x: Int, y: Int) = this.getCellType(x, y) == ElementType.EMPTY


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
