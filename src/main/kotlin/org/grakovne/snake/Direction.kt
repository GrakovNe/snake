package org.grakovne.snake

enum class Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT;

    companion object {
        fun random(): Direction = values().random()
    }

    fun opposite() = when(this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }

}