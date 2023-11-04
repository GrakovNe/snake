package org.grakovne.snake.neural

import org.grakovne.snake.ElementType
import org.grakovne.snake.Field
import org.grakovne.snake.Snake
import java.util.LinkedList

class SafeGraph(private val field: Field) {
    private val width = field.getWidth()
    private val height = field.getHeight()

    fun findSafestPath(start: Pair<Int, Int>, end: Pair<Int, Int>, snake: Snake): List<Pair<Int, Int>> {
        val visited = Array(height) { BooleanArray(width) }
        val queue = LinkedList<Triple<Pair<Int, Int>, List<Pair<Int, Int>>, Int>>()

        // Инициализируем очередь начальной точкой и её "безопасностью"
        queue.add(Triple(start, emptyList(), getSafety(start, snake)))

        var safestPath: List<Pair<Int, Int>> = emptyList()
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
                val (x, y) = neighbor
                if (!visited[y][x] && field.getCellType(x, y) != ElementType.BORDER && Pair(x, y) !in snake.body) {
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

    private fun getSafety(point: Pair<Int, Int>, snake: Snake): Int {
        // Мера безопасности может быть, например, количеством свободных соседних клеток
        return getNeighbors(point).count { neighbor ->
            field.getCellType(neighbor.first, neighbor.second) != ElementType.BORDER && neighbor !in snake.body
        }
    }

    private fun getNeighbors(point: Pair<Int, Int>): List<Pair<Int, Int>> {
        val (x, y) = point
        val neighbors = mutableListOf<Pair<Int, Int>>()

        // Add possible neighboring points
        if (x > 0) neighbors.add(Pair(x - 1, y))
        if (x < width - 1) neighbors.add(Pair(x + 1, y))
        if (y > 0) neighbors.add(Pair(x, y - 1))
        if (y < height - 1) neighbors.add(Pair(x, y + 1))

        return neighbors
    }
}