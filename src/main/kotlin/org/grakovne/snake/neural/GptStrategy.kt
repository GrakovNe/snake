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

        return safeMoves
            .maxByOrNull { direction ->
                evaluateMove(simulateSnakeMove(snake, direction), food, field)
            } ?: Direction.random()
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

    private fun evaluateCompactness(snake: Snake, field: Field): Int {
        var compactnessScore = 0
        val body = snake.body.toList()

        val edgePenalty = 10 // Устанавливаем штраф за расположение в крайних рядах и столбцах.

        // Получаем размеры поля для проверки крайних рядов и столбцов
        val maxWidth = field.getWidth()
        val maxHeight = field.getHeight()

        // Для каждого сегмента змейки проверяем соседние сегменты и штрафуем за крайние позиции
        for (i in 0 until body.size) {
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

            // Штрафуем сегменты на краях
            if (current.first == 0 || current.first == maxWidth - 1 || current.second == 0 || current.second == maxHeight - 1) {
                compactnessScore -= edgePenalty
            }
        }

        // Поскольку мы хотим предпочесть квадратные формы, вычитаем длину змейки,
        // чтобы дать высший балл более компактным формам.
        return compactnessScore - body.size
    }


    private fun evaluateEnclosureRisk(snake: Snake, field: Field): Int {
        val head = snake.head()
        val accessibleArea = bfsAccessibleArea(head.first, head.second, field)
        val snakeSize = snake.body.size

        // "Степень замкнутости" рассчитывается как отношение размера доступной области к размеру змейки.
        // Чем меньше доступная область по сравнению с размером змейки, тем выше риск замкнуть себя.
        val enclosureRisk = if (accessibleArea.size < snakeSize * 2) {
            (snakeSize * 2 - accessibleArea.size) // Возвращаем отрицательное значение, которое будет вычитаться из общего счета
        } else {
            0 // Нет риска
        }

        // Возвращаем обратное значение риска, так как более высокий риск замкнутости должен уменьшать общий балл хода.
        return -enclosureRisk
    }

    private fun evaluateLinearity(snake: Snake): Int {
        var linearityPenalty = 0

        // Получаем все сегменты змейки
        val body = snake.body.toList()

        // Проходим по всем сегментам змейки
        for (i in 1 until body.size - 1) { // Игнорируем голову и хвост для линейности
            val prev = body[i - 1]
            val curr = body[i]
            val next = body[i + 1]

            // Проверяем, лежат ли предыдущий и следующий сегменты в одной линии с текущим
            if ((prev.first == curr.first && curr.first == next.first) || (prev.second == curr.second && curr.second == next.second)) {
                linearityPenalty++
            }
        }

        // Возвращаем штраф за линейность, отрицательное значение, поскольку мы хотим минимизировать линейность
        return -linearityPenalty
    }



    private fun evaluateMove(snake: Snake, food: Food, field: Field): Int {
        val head = snake.head()
        val safeGraph = SafeGraph(field)

        val linearity = evaluateLinearity(snake)
        val safestPath = safeGraph.findSafestPath(head, BodyItem(food.x, food.y), snake)

        val enclosed = evaluateEnclosingPotential(snake, field)
        val compactness = evaluateCompactness(snake, field)
        val enclosureRisk = evaluateEnclosureRisk(snake, field)

        return when {
            food.x == head.first && food.y == head.second -> Int.MAX_VALUE
            safestPath.isEmpty() -> Int.MIN_VALUE
            else -> field.getWidth() * field.getHeight() - (safestPath.size) - enclosed + (compactness) - enclosureRisk + linearity
        }
    }
}