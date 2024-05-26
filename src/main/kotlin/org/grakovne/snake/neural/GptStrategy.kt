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

    // base pretrained weights
    var enclosedScoreWeight = 0.7
    var compactnessScoreWeight = 3.0
    var enclosureRiskScoreWeight = 1.5
    var linearityScoreWeight = 1.0
    var distanceToCenterScoreWeight = 2.0
    var spaceAvailabilityScoreWeight = 1.5
    var wallProximityScoreWeight = 1.0
    var foodDistanceScoreWeight = 2.0

    fun setWeights(weights: List<Double>) {
        if (weights.size != 8) throw IllegalArgumentException("Weights list must have exactly 9 elements")
        enclosedScoreWeight = weights[0]
        compactnessScoreWeight = weights[1]
        enclosureRiskScoreWeight = weights[2]
        linearityScoreWeight = weights[3]
        distanceToCenterScoreWeight = weights[4]
        spaceAvailabilityScoreWeight = weights[5]
        wallProximityScoreWeight = weights[6]
        foodDistanceScoreWeight = weights[7]
    }

    fun getMove(snake: Snake, field: Field, food: Food, previousDirection: Direction?): Direction {
        val availableMoves = Direction
            .values()
            .filter { it != previousDirection?.opposite() }
            .filter { isValidMove(snake, field, it) }

        if (availableMoves.isEmpty()) {
            return Direction.random()
        }

        return availableMoves
            .maxByOrNull { direction -> evaluateMove(snake, food, field, direction) }
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

        val edgePenalty = 10

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

            if (localCompactness > 1) {
                compactnessScore += localCompactness
            }

            if (current.first == 0 || current.first == maxWidth - 1 || current.second == 0 || current.second == maxHeight - 1) {
                compactnessScore -= edgePenalty
            }
        }
        return compactnessScore * 2 - body.size
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

    private fun evaluateMove(snake: Snake, food: Food, field: Field, direction: Direction): Double {
        val simulatedSnake = simulateSnakeMove(snake, direction)

        val linearity = evaluateLinearity(simulatedSnake)
        val enclosed = evaluateEnclosingPotential(simulatedSnake, field)
        val compactness = evaluateCompactness(simulatedSnake, field)
        val enclosureRisk = evaluateEnclosureRisk(simulatedSnake, field)
        val distanceToCenter = evaluateDistanceToCenter(simulatedSnake, field)
        val spaceAvailability = evaluateSpaceAvailability(simulatedSnake, field)
        val wallProximity = evaluateWallProximity(simulatedSnake, field)
        val foodDistance = evaluateFoodDistance(simulatedSnake, food)

        val fieldSize = field.getWidth() * field.getHeight()
        //val compactnessScore = (compactnessScoreWeight * compactness)
        val distanceToCenterScore = -(distanceToCenterScoreWeight * distanceToCenter)
        val spaceAvailabilityScore = (spaceAvailabilityScoreWeight * spaceAvailability)
        val wallProximityScore = -(wallProximityScoreWeight * wallProximity)
        val foodDistanceScore = -(foodDistanceScoreWeight * foodDistance)

        return fieldSize +
                //enclosedScore +
                //compactnessScore +
                //enclosureRiskScore +
                //linearityScore +
                distanceToCenterScore +
                spaceAvailabilityScore +
                wallProximityScore +
                foodDistanceScore
    }

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

        return proximity
    }

    private fun evaluateFoodDistance(snake: Snake, food: Food): Int {
        val head = snake.head()
        return abs(head.first - food.x) + abs(head.second - food.y)
    }
}
