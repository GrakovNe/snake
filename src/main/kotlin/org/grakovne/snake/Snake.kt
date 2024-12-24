package org.grakovne.snake

class Snake(point: BodyItem) {
    val body: MutableList<BodyItem> = mutableListOf()
    fun head() = body.first()

    var hasFood = false

    init {
        body.add(BodyItem(point.first, point.second))
    }

    fun willAteSelf(direction: Direction): Boolean {
        val newHead = when (direction) {
            Direction.UP -> BodyItem(head().first - 1, head().second)
            Direction.DOWN -> BodyItem(head().first + 1, head().second)
            Direction.LEFT -> BodyItem(head().first, head().second - 1)
            Direction.RIGHT -> BodyItem(head().first, head().second + 1)
        }
        return body.contains(newHead)
    }

    fun grow() {
        hasFood = true
    }

    fun move(direction: Direction) {
        val newHead = when (direction) {
            Direction.UP -> BodyItem(head().first - 1, head().second)
            Direction.DOWN -> BodyItem(head().first + 1, head().second)
            Direction.LEFT -> BodyItem(head().first, head().second - 1)
            Direction.RIGHT -> BodyItem(head().first, head().second + 1)
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
        if (other !is Snake) return false
        return body == other.body && hasFood == other.hasFood
    }

    override fun hashCode(): Int {
        var result = body.hashCode()
        result = 31 * result + hasFood.hashCode()
        return result
    }
}
