package org.grakovne.snake

class Field(private val xSize: Int, private val ySize: Int) {
    private val elements: Array<Array<ElementType>> = Array(xSize) { Array(ySize) { ElementType.EMPTY } }

    init {
        drawBorders()
    }

    fun getCells(): MutableList<Pair<ElementType, Pair<Int, Int>>> {
        val result = mutableListOf<Pair<ElementType, Pair<Int, Int>>>()

        for (i in 0 until xSize) {
            for (j in 0 until ySize) {
                result.add(elements[i][j] to (i to j))
            }
        }

        return result
    }

    fun getCellType(xPos: Int, yPos: Int): ElementType = elements[xPos][yPos]

    fun getWidth() = elements.size
    fun getHeight() = elements[0].size

    fun getRandomFreeCell(): Pair<Int, Int> {
        val freeCells: MutableList<Pair<Int, Int>> = mutableListOf()

        for (i in 0 until xSize) {
            for (j in 0 until ySize) {
                if (elements[i][j] == ElementType.EMPTY) {
                    freeCells.add(i to j)
                }
            }
        }

        return freeCells.random()
    }

    fun update(snake: Snake, food: Food) {
        drawBorders()

        elements[food.x][food.y] = ElementType.FOOD

        snake
            .body
            .forEach {
                elements[it.first][it.second] = ElementType.SNAKE
            }

        elements[snake.body.first().first][snake.body.first().second] = ElementType.SNAKE_HEAD
    }

    private fun drawBorders() {
        for (i in 0 until xSize) {
            for (j in 0 until ySize) {
                when {
                    i == 0 -> {
                        elements[i][j] = ElementType.BORDER
                    }

                    i == xSize - 1 -> {
                        elements[i][j] = ElementType.BORDER
                    }

                    j == 0 -> {
                        elements[i][j] = ElementType.BORDER
                    }

                    j == ySize - 1 -> {
                        elements[i][j] = ElementType.BORDER
                    }

                    else -> elements[i][j] = ElementType.EMPTY
                }
            }
        }
    }

    companion object {
        fun Snake.copy(): Snake {
            val copy = Snake(this.head())

            // Копируем тело змейки
            for (segment in body.drop(1)) {
                copy.body.add(segment.first to segment.second)
            }

            // Копируем статус наличия еды
            copy.hasFood = hasFood

            return copy
        }


    }
}