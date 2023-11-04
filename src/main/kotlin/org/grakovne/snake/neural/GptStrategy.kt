package org.grakovne.snake.neural

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
    private val fieldAccessibilityCache = ConcurrentHashMap<BodyItem, Set<BodyItem>>()

    fun getMove(snake: Snake, field: Field, food: Food, previousDirection: Direction?): Direction {
        val availableMoves = Direction
            .values()
            .filter { it != previousDirection?.opposite() }
            .filter { isValidMove(snake, field, it) }

        if (availableMoves.isEmpty()) {
            return Direction.random()
        }

        val safeMoves = runBlocking {
            withContext(Dispatchers.Default) {
                availableMoves.map { direction ->
                    async {
                        Pair(direction, isSafeMove(snake, field, direction))
                    }
                }.awaitAll()
                    .filter { it.second }
                    .map { it.first }
            }
        }

        return when {
            safeMoves.isNotEmpty() ->
                safeMoves.maxByOrNull { direction ->
                    evaluateMove(simulateSnakeMove(snake, direction), food, field, direction)
                } ?: Direction.random()

            else ->
                availableMoves
                    .maxByOrNull { direction ->
                        compactnessScore(simulateSnakeMove(snake, direction), field)
                    } ?: Direction.random()
        }
    }

    private fun evaluateEnclosingPotential(snake: Snake, field: Field): Int {
        val head = snake.head()

        var enclosingPotential = 0
        deltaMap.values.forEach { (dx, dy) ->
            val neighborX = head.first + dx
            val neighborY = head.second + dy

            if (field.isInBounds(neighborX, neighborY)) {
                val neighbor = BodyItem(neighborX, neighborY)
                if (field.isEmpty(neighborX, neighborY) && !snake.body.contains(neighbor)) {
                    val area = bfsAccessibleArea(neighborX, neighborY, field)
                    if (area.size <= snake.body.size) {
                        enclosingPotential += area.size
                    }
                }
            }
        }

        // Возвращаем обратное значение, потому что меньшее количество "дыр" - лучше
        return -enclosingPotential
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

    private fun getAccessibleAreaCached(startX: Int, startY: Int, field: Field, snake: Snake) =
        fieldAccessibilityCache.computeIfAbsent(BodyItem(startX, startY)) { bfsAccessibleArea(startX, startY, field) }


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


    private fun bfsAccessibleArea(startX: Int, startY: Int, field: Field): Set<BodyItem> {
        // Предполагаем, что у нас есть заранее определенные максимальные размеры поля
        val maxWidth = field.getWidth()
        val maxHeight = field.getHeight()
        val visited = Array(maxWidth) { BooleanArray(maxHeight) }
        val accessibleArea = mutableSetOf<BodyItem>()
        val queue = ArrayDeque<BodyItem>()

        val start = BodyItem(startX, startY)
        queue.add(start)
        visited[startX][startY] = true
        accessibleArea.add(start)

        val directions = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1) // UP, DOWN, LEFT, RIGHT

        while (queue.isNotEmpty()) {
            val item = queue.removeFirst()

            for ((dx, dy) in directions) {
                val newX = item.first + dx
                val newY = item.second + dy

                if (newX in 0 until maxWidth && newY in 0 until maxHeight && field.getCellType(
                        newX,
                        newY
                    ) == ElementType.EMPTY && !visited[newX][newY]
                ) {
                    val newItem = BodyItem(newX, newY)
                    queue.add(newItem)
                    visited[newX][newY] = true
                    accessibleArea.add(newItem)
                }
            }
        }

        return accessibleArea
    }

    private fun trapPotentialAfterEating(snake: Snake, field: Field): Int {
        val headAfterEating = snake.head()
        val accessibleAreaAfterEating =
            getAccessibleAreaCached(headAfterEating.first, headAfterEating.second, field, snake)

        // Проверяем, останется ли достаточно места для движения
        val remainingArea = accessibleAreaAfterEating.size - snake.body.size
        return if (remainingArea < 0) Int.MIN_VALUE / 2 // Назначаем большое штрафное значение, если змейка заперла себя
        else 0 // Без штрафа, если достаточно места
    }


    private fun evaluateMove(snake: Snake, food: Food, field: Field, direction: Direction): Int {
        val head = snake.head()
        val safeGraph = SafeGraph(field)

        val simulatedMove = simulateSnakeMove(snake, direction)

        val safestPath = safeGraph.findSafestPath(head, BodyItem(food.x, food.y), snake)

        val compactness = compactnessScore(simulatedMove, field)
        val enclosed = evaluateEnclosingPotential(simulatedMove, field)
        val trapPotential = trapPotentialAfterEating(simulatedMove, field)

        return when {
            food.x == head.first && food.y == head.second -> Int.MAX_VALUE
            safestPath.isEmpty() -> Int.MIN_VALUE
            else -> field.getWidth() * field.getHeight() - (2 * safestPath.size) + (0.5 * compactness).toInt() + enclosed + trapPotential
        }
    }
}