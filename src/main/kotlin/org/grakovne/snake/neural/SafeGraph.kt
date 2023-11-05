package org.grakovne.snake.neural

import org.grakovne.snake.BodyItem
import org.grakovne.snake.ElementType
import org.grakovne.snake.Field
import org.grakovne.snake.Snake
import java.util.LinkedList

class SafeGraph(private val field: Field) {
    private val width = field.getWidth()
    private val height = field.getHeight()

    fun findSafestPath(start: BodyItem, end: BodyItem, snake: Snake): List<BodyItem> {
        val visited = Array(height) { BooleanArray(width) }
        val queue = LinkedList<Triple<BodyItem, List<BodyItem>, Int>>()

        // Инициализируем очередь начальной точкой и её "безопасностью"
        queue.add(Triple(start, emptyList(), getSafety(start, snake)))

        var safestPath: List<BodyItem> = emptyList()
        var maxSafety = -1

        while (queue.isNotEmpty()) {
            val (current, path, safety) = queue.poll()

            if (current == end) {
                if (safety > maxSafety) {
                    maxSafety = safety
                    safestPath = path
                }
                continue
            }

            val neighbors = getNeighbors(current)
            for (neighbor in neighbors) {
                val x = neighbor.first
                val y = neighbor.second

                val cell = field.getCellType(x, y)
                if (!visited[y][x] && cell != ElementType.BORDER && cell != ElementType.SNAKE && cell != ElementType.SNAKE_HEAD) {
                    visited[y][x] = true
                    val neighborSafety = getSafety(neighbor, snake)
                    if (neighborSafety > maxSafety) {
                        queue.add(Triple(neighbor, path + neighbor, neighborSafety))
                    }
                }
            }
        }

        // Возвращаем наиболее безопасный путь, найденный алгоритмом
        return safestPath
    }

    private fun getSafety(point: BodyItem, snake: Snake): Int {
        return getNeighbors(point).count { neighbor ->
            field.getCellType(neighbor.first, neighbor.second) != ElementType.BORDER &&
                    field.getCellType(neighbor.first, neighbor.second) != ElementType.SNAKE &&
                    field.getCellType(neighbor.first, neighbor.second) != ElementType.SNAKE_HEAD
        }
    }

    private fun getNeighbors(point: BodyItem): List<BodyItem> {
        val x = point.first
        val y = point.second
        val neighbors = mutableListOf<BodyItem>()

        // Add possible neighboring points
        if (x > 0) neighbors.add(BodyItem(x - 1, y))
        if (x < width - 1) neighbors.add(BodyItem(x + 1, y))
        if (y > 0) neighbors.add(BodyItem(x, y - 1))
        if (y < height - 1) neighbors.add(BodyItem(x, y + 1))

        return neighbors
    }
}