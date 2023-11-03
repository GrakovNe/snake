package org.grakovne.snake

enum class Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT;

    companion object {
        fun random(): Direction = values().random()
    }
}