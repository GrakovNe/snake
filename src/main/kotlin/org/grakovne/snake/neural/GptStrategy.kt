package org.grakovne.snake.neural

import org.grakovne.snake.BodyItem
import org.grakovne.snake.Direction
import org.grakovne.snake.ElementType
import org.grakovne.snake.Field
import org.grakovne.snake.Food
import org.grakovne.snake.Snake
import org.grakovne.snake.isValidMove
import org.grakovne.snake.simulateSnakeMove
import java.util.concurrent.ConcurrentHashMap
import kotlin.streams.toList

class GptStrategy {

    private val deltaMap = Direction.values().associateWith { it.toDelta() }
    private val fieldAccessibilityCache = ConcurrentHashMap<Pair<Int, Int>, Set<Pair<Int, Int>>>()

    fun getMove(snake: Snake, field: Field, food: Food, previousDirection: Direction?): Direction {
        val availableMoves = Direction
            .values()
            .filter { it != previousDirection?.opposite() }
            .filter { isValidMove(snake, field, it) }

        if (availableMoves.isEmpty()) {
            return Direction.random()
        }

        val safeMoves = availableMoves.parallelStream()
            .map { direction -> Pair(direction, isSafeMove(snake, field, direction)) }
            .filter { it.second }
            .map { it.first }
            .toList()

        return when {
            safeMoves.isNotEmpty() ->
                safeMoves.maxByOrNull { direction ->
                    evaluateMove(simulateSnakeMove(snake, direction), food, field, direction)
                } ?: Direction.random()

            else ->
                availableMoves
                    .maxByOrNull { direction ->
                        compactnessScore(simulateSnakeMove(snake, direction), field, direction)
                    } ?: Direction.random()
        }
    }

    private fun evaluateEnclosingPotential(snake: Snake, field: Field, direction: Direction): Int {
        val simulatedSnake = simulateSnakeMove(snake, direction)
        val head = simulatedSnake.head()

        var enclosingPotential = 0
        deltaMap.values.forEach { (dx, dy) ->
            val neighborX = head.first + dx
            val neighborY = head.second + dy

            if (field.isInBounds(neighborX, neighborY)) {
                val neighbor = BodyItem(neighborX, neighborY)
                if (field.isEmpty(neighborX, neighborY) && !simulatedSnake.body.contains(neighbor)) {
                    val area = bfsAccessibleArea(neighborX, neighborY, field, simulatedSnake)
                    if (area.size <= snake.body.size) {
                        enclosingPotential += area.size
                    }
                }
            }
        }

        // Возвращаем обратное значение, потому что меньшее количество "дыр" - лучше
        return -enclosingPotential
    }


    private fun compactnessScore(snake: Snake, field: Field, direction: Direction): Int {
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

    private fun getAccessibleAreaCached(startX: Int, startY: Int, field: Field, snake: Snake) =
        fieldAccessibilityCache.computeIfAbsent(startX to startY) { bfsAccessibleArea(startX, startY, field, snake) }


    private fun Field.isInBounds(x: Int, y: Int) = x in 0 until this.getWidth() && y in 0 until this.getHeight()
    private fun Field.isEmpty(x: Int, y: Int) = this.getCellType(x, y) == ElementType.EMPTY


    private fun Direction.toDelta(): Pair<Int, Int> = when (this) {
        Direction.UP -> Pair(-1, 0)
        Direction.DOWN -> Pair(1, 0)
        Direction.LEFT -> Pair(0, -1)
        Direction.RIGHT -> Pair(0, 1)
    }

    private val safeMoveCache = ConcurrentHashMap<BodyItem, Boolean>()

    private fun isSafeMove(snake: Snake, field: Field, direction: Direction): Boolean {
        val simulatedSnake = simulateSnakeMove(snake, direction)
        val head = simulatedSnake.head()

        return safeMoveCache.computeIfAbsent(head) {
            val accessibleArea = getAccessibleAreaCached(head.first, head.second, field, simulatedSnake)

            val longTermSurvivability = accessibleArea.any { area ->
                getAccessibleAreaCached(area.first, area.second, field, simulatedSnake).size > snake.body.size
            }

            accessibleArea.size > snake.body.size && longTermSurvivability
        }
    }



    private fun bfsAccessibleArea(startX: Int, startY: Int, field: Field, snake: Snake): Set<Pair<Int, Int>> {
        val visited = HashSet<BodyItem>()
        val queue = ArrayDeque<BodyItem>()

        val start = BodyItem(startX, startY)
        queue.add(start)
        visited.add(start)

        while (queue.isNotEmpty()) {
            val item = queue.removeFirst()

            val x = item.first
            val y = item.second

            Direction.values().forEach { direction ->
                val (newX, newY) = when (direction) {
                    Direction.UP -> x - 1 to y
                    Direction.DOWN -> x + 1 to y
                    Direction.LEFT -> x to y - 1
                    Direction.RIGHT -> x to y + 1
                }

                val new = BodyItem(newX, newY)
                if (newX in 0 until field.getWidth() &&
                    newY in 0 until field.getHeight() &&
                    field.getCellType(newX, newY) != ElementType.BORDER &&
                    field.getCellType(newX, newY) != ElementType.SNAKE &&
                    field.getCellType(newX, newY) != ElementType.SNAKE_HEAD &&
                    new !in visited
                ) {
                    queue.add(new)
                    visited.add(new)
                }
            }
        }

        return visited.map { Pair(it.first, it.second) }.toSet()
    }

    private fun evaluateMove(snake: Snake, food: Food, field: Field, direction: Direction): Int {
        val head = snake.head()
        val safeGraph = SafeGraph(field)

        val simulatedMove = simulateSnakeMove(snake, direction)

        val safestPath = safeGraph.findSafestPath(head, BodyItem(food.x, food.y), snake)

        val compactness = compactnessScore(simulatedMove, field, direction)
        val enclosed = evaluateEnclosingPotential(simulatedMove, field, direction)

        val bestPath = safestPath

        return when {
            food.x == head.first && food.y == head.second -> Int.MAX_VALUE
            bestPath.isEmpty() -> Int.MIN_VALUE
            else -> field.getWidth() * field.getHeight() - bestPath.size + (0.5 * compactness).toInt() + enclosed
        }
    }
}