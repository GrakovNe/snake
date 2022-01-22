package org.grakovne.snake

import kotlin.random.Random

class RandomSnakeStrategy {

    fun getMove(snake: Snake, field: Field, food: Food): Direction {
        val foodDirection = whereFood(snake, food)

        val head = snake.head()

        val nextField = when (foodDirection) {
            Direction.UP -> field.getCellType(head.first, head.second - 1)
            Direction.DOWN -> field.getCellType(head.first, head.second + 1)
            Direction.LEFT -> field.getCellType(head.first - 1, head.second)
            Direction.RIGHT -> field.getCellType(head.first + 1, head.second)
        }

        return foodDirection
    }

    private fun isDirectionSafe(nextField: ElementType): Boolean {
        return when (nextField) {
            ElementType.EMPTY -> true
            ElementType.SNAKE_HEAD -> false
            ElementType.SNAKE -> false
            ElementType.BORDER -> false
            ElementType.FOOD -> true
        }
    }

    private fun whereFood(snake: Snake, food: Food): Direction {
        if (snake.head().first > food.x) {
            return Direction.UP
        }

        if (snake.head().first < food.x) {
            return Direction.DOWN
        }

        if (snake.head().second < food.y) {
            return Direction.RIGHT
        }

        if (snake.head().second > food.y) {
            return Direction.LEFT
        }

        return randomDirection(snake)
    }

    private fun randomDirection(snake: Snake): Direction {
        var direction = randomDirection()

        while (!snake.mayDirection(direction)) {
            direction = randomDirection()
        }

        return direction
    }

    private fun Snake.mayDirection(direction: Direction): Boolean {
        if (this.body.size == 1) {
            return true
        }

        return when (direction) {
            Direction.UP -> mayUp()
            Direction.DOWN -> mayDown()
            Direction.LEFT -> mayLeft()
            Direction.RIGHT -> mayRight()
        }
    }

    private fun Snake.mayLeft(): Boolean {
        val head = body[0]
        val tail = body[1]

        return tail.second - head.second != 1
    }


    private fun Snake.mayRight(): Boolean {
        val head = body[0]
        val tail = body[1]

        return tail.second - head.second != 1
    }

    private fun Snake.mayUp(): Boolean {
        val head = body[0]
        val tail = body[1]

        return tail.first - head.first != -1
    }

    private fun Snake.mayDown(): Boolean {
        val head = body[0]
        val tail = body[1]

        return tail.first - head.first != 1
    }


    private fun randomDirection(): Direction = Random
        .nextInt(1, 5)
        .let {
            when (it) {
                1 -> Direction.UP
                2 -> Direction.RIGHT
                3 -> Direction.LEFT
                4 -> Direction.DOWN
                else -> Direction.RIGHT
            }
        }
}