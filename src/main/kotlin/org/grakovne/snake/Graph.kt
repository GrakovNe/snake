package org.grakovne.snake

import java.util.LinkedList
import java.util.Queue

class Graph(private val field: Field) {
    private val width = field.getWidth()
    private val height = field.getHeight()

    fun findShortestPath(start: Pair<Int, Int>, end: Pair<Int, Int>): List<Pair<Int, Int>> {
        val visited = Array(height) { BooleanArray(width) }
        val queue = LinkedList<Pair<Pair<Int, Int>, List<Pair<Int, Int>>>>()

        queue.add(Pair(start, emptyList()))

        while (queue.isNotEmpty()) {
            val (current, path) = queue.poll()

            if (current == end) {
                return path
            }

            val neighbors = getNeighbors(current)
            for (neighbor in neighbors) {
                val (x, y) = neighbor
                if (!visited[y][x] && field.getCellType(x, y) != ElementType.BORDER) {
                    visited[y][x] = true
                    queue.add(Pair(neighbor, path + neighbor))
                }
            }
        }

        // If no path found, return an empty path
        return emptyList()
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
