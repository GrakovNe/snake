package org.grakovne.snake

class Snake(point: BodyItem) {
    val body: MutableList<BodyItem> = ArrayList()
    fun head() = body.first()

    var hasFood = false

    init {
        body.add(BodyItem(point.first, point.second))
    }


    fun willAteSelf(direction: Direction): Boolean {
        val headX = body.first().first
        val headY = body.first().second

        val newHead = when (direction) {
            Direction.UP -> BodyItem(headX - 1, headY)
            Direction.DOWN -> BodyItem(headX + 1, headY)
            Direction.LEFT -> BodyItem(headX, headY - 1)
            Direction.RIGHT -> BodyItem(headX, headY + 1)
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
            Direction.UP -> BodyItem(headX - 1, headY)
            Direction.DOWN -> BodyItem(headX + 1, headY)
            Direction.LEFT -> BodyItem(headX, headY - 1)
            Direction.RIGHT -> BodyItem(headX, headY + 1)
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