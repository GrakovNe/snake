package org.grakovne.snake.neural

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.grakovne.snake.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.sqrt

class GptStrategy {

    private val deltaMap = Direction.values().associateWith { it.toDelta() }
    private val fieldAccessibilityCache = ConcurrentHashMap<BodyItem, Set<BodyItem>>()

    // Веса для различных оценочных функций
    private var enclosedWeight: Double = 1.0
    private var compactnessWeight: Double = 1.0
    private var enclosureRiskWeight: Double = 1.0
    private var linearityWeight: Double = 1.0
    private var distanceToCenterWeight: Double = 1.0
    private var spaceAvailabilityWeight: Double = 1.0
    private var freeAroundSpaceWeight: Double = 1.0
    private var foodDistanceWeight: Double = 1.0
    private var trapRiskWeight: Double = 1.0

    fun setWeights(weights: List<Double>) {
        if (weights.size != 9) throw IllegalArgumentException("Weights list must have exactly 9 elements")
        enclosedWeight = weights[0]
        compactnessWeight = weights[1]
        enclosureRiskWeight = weights[2]
        linearityWeight = weights[3]
        distanceToCenterWeight = weights[4]
        spaceAvailabilityWeight = weights[5]
        freeAroundSpaceWeight = weights[6]
        foodDistanceWeight = weights[7]
        trapRiskWeight = weights[8]
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
        return -enclosingPotential
    }

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

    private fun evaluateCompactness(snake: Snake, field: Field): Int {
        var compactnessScore = 0
        val body = snake.body.toList()
        val maxWidth = field.getWidth()
        val maxHeight = field.getHeight()

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

    private fun evaluateEnclosureRisk(snake: Snake, field: Field): Int {
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

    private fun evaluateLinearity(snake: Snake): Int {
        var linearityPenalty = 0
        val body = snake.body.toList()

        for (i in 1 until body.size - 1) {
            val prev = body[i - 1]
            val curr = body[i]
            val next = body[i + 1]

            if ((prev.first == curr.first && curr.first == next.first) || (prev.second == curr.second && curr.second == next.second)) {
                linearityPenalty++
            }
        }
        return -linearityPenalty
    }

    private fun evaluateDistanceToCenter(snake: Snake, field: Field): Int {
        val centerX = field.getWidth() / 2.0
        val centerY = field.getHeight() / 2.0
        val head = snake.head()
        val distanceX = head.first - centerX
        val distanceY = head.second - centerY
        return sqrt(distanceX * distanceX + distanceY * distanceY).toInt()
    }


    private fun evaluateMove(snake: Snake, food: Food, field: Field, direction: Direction): Int {
        val head = snake.head()
        val simulatedSnake = simulateSnakeMove(snake, direction)

        val linearity = evaluateLinearity(snake)
        val enclosed = evaluateEnclosingPotential(snake, field)
        val compactness = evaluateCompactness(snake, field)
        val enclosureRisk = evaluateEnclosureRisk(snake, field)
        val distanceToCenter = evaluateDistanceToCenter(snake, field)
        val spaceAvailability = evaluateSpaceAvailability(simulatedSnake, field)
        val freeAroundSpace = evaluateFreeSpaceAroundHead(simulatedSnake, field)
        val foodDistance = evaluateFoodDistance(simulatedSnake, food)
        val trapRisk = evaluateTrapRisk(snake, field, food)

        return when {
            food.x == head.first && food.y == head.second -> Int.MAX_VALUE
            else -> {
                val fieldSize = field.getWidth() * field.getHeight()
                val enclosedScore = -(enclosedWeight * enclosed).toInt()
                val compactnessScore = (compactnessWeight * compactness).toInt()
                val enclosureRiskScore = -(enclosureRiskWeight * enclosureRisk).toInt()
                val linearityScore = (linearityWeight * linearity).toInt()
                val distanceToCenterScore = -(distanceToCenterWeight * distanceToCenter).toInt()
                val spaceAvailabilityScore = (spaceAvailabilityWeight * spaceAvailability).toInt()
                val wallProximityScore = -(freeAroundSpaceWeight * freeAroundSpace).toInt()
                val foodDistanceScore = -(foodDistanceWeight * foodDistance).toInt()
                val trapRiskScore = -(trapRiskWeight * trapRisk).toInt()

                fieldSize +
                        enclosedScore +
                        compactnessScore +
                        enclosureRiskScore +
                        linearityScore +
                        distanceToCenterScore +
                        spaceAvailabilityScore +
                        wallProximityScore +
                        trapRiskScore +
                        foodDistanceScore

            }
        }
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
            if (field.isInBounds(newX, newY) && field.isEmpty(newX, newY) && !snake.body.contains(BodyItem(newX, newY))) {
                freeSpaceCount++
            }
        }
        return freeSpaceCount
    }

    private fun evaluateFoodDistance(snake: Snake, food: Food): Int {
        val head = snake.head()
        return abs(head.first - food.x) + abs(head.second - food.y)
    }

    private fun evaluateTrapRisk(snake: Snake, field: Field, food: Food): Int {
        val head = snake.head()
        val foodAccessibleArea = bfsAccessibleArea(food.x, food.y, field)
        val headAccessibleArea = bfsAccessibleArea(head.first, head.second, field)

        return if (!foodAccessibleArea.containsAll(headAccessibleArea)) {
            Int.MAX_VALUE
        } else {
            0
        }
    }
}
