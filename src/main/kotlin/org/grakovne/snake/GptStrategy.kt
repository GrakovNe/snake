package org.grakovne.snake

import java.util.PriorityQueue

class GptStrategy {

    fun getMove(snake: Snake, field: Field, food: Food): Direction {
        val headX = snake.head().first
        val headY = snake.head().second

        // Функция, которая проверяет, можно ли двигаться в заданном направлении
        fun canMove(direction: Direction): Boolean {
            val newHeadX = when (direction) {
                Direction.UP -> headX - 1
                Direction.DOWN -> headX + 1
                Direction.LEFT -> headX
                Direction.RIGHT -> headX
            }
            val newHeadY = when (direction) {
                Direction.UP -> headY
                Direction.DOWN -> headY
                Direction.LEFT -> headY - 1
                Direction.RIGHT -> headY + 1
            }

            // Проверяем, не столкнется ли змейка со стеной
            if (newHeadX < 0 || newHeadX >= field.getWidth() || newHeadY < 0 || newHeadY >= field.getHeight()) {
                return false
            }

            // Проверяем, не столкнется ли змейка с самой собой
            val newHead = newHeadX to newHeadY
            if (snake.body.contains(newHead)) {
                return false
            }

            return true
        }

        // Возможные направления движения
        val possibleDirections = mutableListOf<Direction>()

        // Добавляем направления, в которые можно двигаться
        if (canMove(Direction.UP)) {
            possibleDirections.add(Direction.UP)
        }
        if (canMove(Direction.DOWN)) {
            possibleDirections.add(Direction.DOWN)
        }
        if (canMove(Direction.LEFT)) {
            possibleDirections.add(Direction.LEFT)
        }
        if (canMove(Direction.RIGHT)) {
            possibleDirections.add(Direction.RIGHT)
        }

        // Если есть еда, выбираем направление к ней
        if (possibleDirections.isNotEmpty()) {
            val foodX = food.x
            val foodY = food.y
            val minDistanceDirection = possibleDirections.minByOrNull {
                when (it) {
                    Direction.UP -> Math.abs(headX - 1 - foodX) + Math.abs(headY - foodY)
                    Direction.DOWN -> Math.abs(headX + 1 - foodX) + Math.abs(headY - foodY)
                    Direction.LEFT -> Math.abs(headX - foodX) + Math.abs(headY - 1 - foodY)
                    Direction.RIGHT -> Math.abs(headX - foodX) + Math.abs(headY + 1 - foodY)
                }
            }
            return minDistanceDirection ?: possibleDirections.random()
        }

        // Если нет еды, выбираем любое доступное направление
        return possibleDirections.random()
    }

}