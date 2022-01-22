package org.grakovne.snake

class Snake(point: Pair<Int, Int>) {
    val body: MutableList<Pair<Int, Int>> = mutableListOf()
    fun head() = body.first()

    private var hasFood = false

    init {
        body.add(point.first to point.second)
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