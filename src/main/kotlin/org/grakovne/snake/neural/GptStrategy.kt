package org.grakovne.snake.neural

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.grakovne.snake.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class GptStrategy {

    private val fieldAccessibilityCache = ConcurrentHashMap<BodyItem, Set<BodyItem>>()

    // Веса для различных оценочных функций
    private var freeSpaceWeight: Double = 1.0
    private var pathToFoodWeight: Double = 1.0
    private var survivabilityWeight: Double = 1.0
    private var wallProximityWeight: Double = 1.0
    private var compactnessWeight: Double = 1.0
    private var fieldPartitioningRiskWeight: Double = 1.0
    private var squarenessWeight: Double = 1.0

    fun setWeights(weights: List<Double>) {
        if (weights.size != 7) throw IllegalArgumentException("Weights list must have exactly 5 elements")
        freeSpaceWeight = weights[0]
        pathToFoodWeight = weights[1]
        survivabilityWeight = weights[2]
        wallProximityWeight = weights[3]
        compactnessWeight = weights[4]
        compactnessWeight = weights[5]
        squarenessWeight = weights[6]
    }

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

        return safeMoves
            .maxByOrNull { direction ->
                evaluateMove(snake, food, field, direction)
            }
            ?: availableMoves
                .maxByOrNull { direction ->
                    evaluateSpaceAvailability(simulateSnakeMove(snake, direction), field)
                }
            ?: Direction.random()
    }

    private fun evaluateSpaceAvailability(snake: Snake, field: Field): Int =
        bfsAccessibleArea(snake.head().first, snake.head().second, field).size

    private fun getAccessibleAreaCached(startX: Int, startY: Int, field: Field, snake: Snake): Set<BodyItem> =
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
        val maxWidth = field.getWidth()
        val maxHeight = field.getHeight()
        val visited = Array(maxWidth) { BooleanArray(maxHeight) }
        val accessibleArea = mutableSetOf<BodyItem>()
        val queue = ArrayDeque<BodyItem>()

        val start = BodyItem(startX, startY)
        queue.add(start)
        visited[startX][startY] = true
        accessibleArea.add(start)

        val directions = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

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

    private fun evaluateSquareness(snake: Snake): Int {
        val minX = snake.body.minOf { it.first }
        val maxX = snake.body.maxOf { it.first }
        val minY = snake.body.minOf { it.second }
        val maxY = snake.body.maxOf { it.second }

        val width = maxX - minX + 1
        val height = maxY - minY + 1

        // Оцениваем отклонение от квадрата
        val squareness = -abs(width - height)
        return squareness
    }

    // Новая метрика: количество свободных клеток вокруг головы змейки
    private fun evaluateFreeSpaceAroundHead(snake: Snake, field: Field): Int {
        val head = snake.head()
        val directions = listOf(
            Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)
        )
        var freeSpaceCount = 0
        for ((dx, dy) in directions) {
            val newX = head.first + dx
            val newY = head.second + dy
            if (field.isInBounds(newX, newY) && field.isEmpty(newX, newY) && !snake.body.contains(
                    BodyItem(
                        newX,
                        newY
                    )
                )
            ) {
                freeSpaceCount++
            }
        }
        return freeSpaceCount
    }

    // Новая метрика: проверка наличия пути к еде и оценка расстояния до неё
    private fun evaluatePathToFood(snake: Snake, field: Field, food: Food): Int {
        val head = snake.head()
        val pathToFood = bfsAccessibleArea(food.x, food.y, field)
        return if (pathToFood.contains(head)) {
            val distanceToFood = abs(head.first - food.x) + abs(head.second - food.y)
            -distanceToFood
        } else {
            Int.MIN_VALUE
        }
    }

    // Новая метрика: оценка выживаемости змейки
    private fun evaluateSurvivability(snake: Snake, field: Field): Int {
        val head = snake.head()
        val accessibleArea = bfsAccessibleArea(head.first, head.second, field)
        val snakeSize = snake.body.size

        val enclosureRisk = if (accessibleArea.size < snakeSize * 2) {
            (snakeSize * 2 - accessibleArea.size)
        } else {
            0
        }

        return -enclosureRisk
    }

    // Метрика: близость к стенам
    private fun evaluateWallProximity(snake: Snake, field: Field): Int {
        val head = snake.head()
        val maxWidth = field.getWidth()
        val maxHeight = field.getHeight()

        val proximity = listOf(
            head.first,
            head.second,
            maxWidth - head.first,
            maxHeight - head.second
        ).minOrNull() ?: 0

        return -proximity
    }

    // Метрика: компактность змейки
    private fun evaluateCompactness(snake: Snake): Int {
        var compactnessScore = 0
        val body = snake.body.toList()

        for (i in body.indices) {
            val current = body[i]
            val neighbors = listOf(
                BodyItem(current.first - 1, current.second),
                BodyItem(current.first + 1, current.second),
                BodyItem(current.first, current.second - 1),
                BodyItem(current.first, current.second + 1)
            )

            var localCompactness = 0
            for (neighbor in neighbors) {
                if (neighbor in body) {
                    localCompactness++
                }
            }
            compactnessScore += localCompactness
        }
        return compactnessScore
    }

    private fun evaluateFieldPartitioningRisk(snake: Snake, field: Field, direction: Direction): Int {
        val head = snake.head()
        val accessibleAreaBefore = bfsAccessibleArea(head.first, head.second, field)

        val simulatedSnake = simulateSnakeMove(snake, direction)

        val accessibleAreaAfter = bfsAccessibleArea(simulatedSnake.head().first, simulatedSnake.head().second, field)

        return if (accessibleAreaBefore.size > accessibleAreaAfter.size) {
            accessibleAreaBefore.size - accessibleAreaAfter.size
        } else {
            0
        }
    }

    // Обновление функции оценки движения
    private fun evaluateMove(snake: Snake, food: Food, field: Field, direction: Direction): Double {
        val simulatedSnake = simulateSnakeMove(snake, direction)

        val freeSpaceAroundHead = evaluateFreeSpaceAroundHead(simulatedSnake, field)
        val pathToFood = evaluatePathToFood(simulatedSnake, field, food)
        val survivability = evaluateSurvivability(simulatedSnake, field)
        val wallProximity = evaluateWallProximity(simulatedSnake, field)
        val compactness = evaluateCompactness(simulatedSnake)
        val fieldPartitioningRisk = evaluateFieldPartitioningRisk(snake, field, direction)
        val squareness = evaluateSquareness(simulatedSnake)

        return freeSpaceWeight * freeSpaceAroundHead +
                pathToFoodWeight * pathToFood +
                survivabilityWeight * survivability +
                wallProximityWeight * wallProximity +
                compactnessWeight * compactness +
                squarenessWeight * squareness +
                fieldPartitioningRiskWeight * fieldPartitioningRisk
    }
}
