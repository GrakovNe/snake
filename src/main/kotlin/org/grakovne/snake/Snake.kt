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

        body.add(0, newHead)

        if (!hasFood) {
            body.removeAt(body.size - 1)
        } else {
            hasFood = false
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Snake

        if (body != other.body) return false
        return hasFood == other.hasFood
    }

    override fun hashCode(): Int {
        var result = body.hashCode()
        result = 31 * result + hasFood.hashCode()
        return result
    }
}
