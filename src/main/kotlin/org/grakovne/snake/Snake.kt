package org.grakovne.snake

import java.util.LinkedList

class Snake(point: Pair<Int, Int>) {
    val body: MutableList<Pair<Int, Int>> = LinkedList()
    fun head() = body.first()

    var hasFood = false

    init {
        body.add(point.first to point.second)
        body.add(point.first to point.second + 1)
    }


    fun willAteSelf(direction: Direction): Boolean {
        val headX = body.first().first
        val headY = body.first().second

        val newHead = when (direction) {
            Direction.UP -> headX - 1 to headY
            Direction.DOWN -> headX + 1 to headY
            Direction.LEFT -> headX to headY - 1
            Direction.RIGHT -> headX to headY + 1
        }

        return body.any { it == newHead }
    }

    fun grow() {
        hasFood = true
    }

    fun move(direction: Direction) {
        val headX = body.first().first
        val headY = body.first().second

        val newHead = when (direction) {
            Direction.UP -> headX - 1 to headY
            Direction.DOWN -> headX + 1 to headY
            Direction.LEFT -> headX to headY - 1
            Direction.RIGHT -> headX to headY + 1


        }

        body.drop(1)

        if (!hasFood) {
            body.removeAt(body.size - 1)
        } else {
            hasFood = false
        }

        body.add(0, newHead)
    }

}