package org.grakovne.snake

import kotlin.math.abs
import kotlin.math.sqrt

class GptStrategy {

    // base pretrained weights
    var distanceToCenterScoreWeight = 2.0
    var spaceAvailabilityScoreWeight = 1.5
    var wallProximityScoreWeight = 1.0
    var foodDistanceScoreWeight = 2.0

    fun setWeights(weights: List<Double>) {
        distanceToCenterScoreWeight = weights[0]
        spaceAvailabilityScoreWeight = weights[1]
        wallProximityScoreWeight = weights[2]
        foodDistanceScoreWeight = weights[3]
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
        -bfsAccessibleArea(snake.head().first, snake.head().second, field).size

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

        val distanceToCenter = evaluateDistanceToCenter(simulatedSnake, field)
        val spaceAvailability = evaluateSpaceAvailability(simulatedSnake, field)
        val wallProximity = evaluateWallProximity(simulatedSnake, field)
        val foodDistance = evaluateFoodDistance(simulatedSnake, food)

        val fieldSize = field.getWidth() * field.getHeight()
        val distanceToCenterScore = -(distanceToCenterScoreWeight * distanceToCenter)
        val spaceAvailabilityScore = -(spaceAvailabilityScoreWeight * spaceAvailability)
        val wallProximityScore = -(wallProximityScoreWeight * wallProximity)
        val foodDistanceScore = -(foodDistanceScoreWeight * foodDistance)

        return fieldSize +
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

fun isValidMove(snake: Snake, field: Field, direction: Direction): Boolean {
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
            BodyItem(x, y) !in snake.body)
}

fun simulateSnakeMove(snake: Snake, direction: Direction): Snake {
    val newSnake = Snake(snake.head())
    newSnake.body.addAll(snake.body.drop(1))
    newSnake.move(direction)
    return newSnake
}
