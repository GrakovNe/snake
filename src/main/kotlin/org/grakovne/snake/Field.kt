package org.grakovne.snake

class Field(private val xSize: Int, private val ySize: Int) {
    private val elements: Array<Array<ElementType>> = Array(xSize) { Array(ySize) { ElementType.EMPTY } }

    init {
        drawBorders()
    }

    fun getCells(): MutableList<Pair<ElementType, BodyItem>> {
        val result = mutableListOf<Pair<ElementType, BodyItem>>()

        for (i in 0 until xSize) {
            for (j in 0 until ySize) {
                result.add(elements[i][j] to BodyItem(i, j))
            }
        }

        return result
    }

    fun getCellType(xPos: Int, yPos: Int): ElementType = elements[xPos][yPos]

    fun getWidth() = elements.size
    fun getHeight() = elements[0].size

    fun update(snake: Snake, food: Food) {
        drawBorders()

        elements[food.x][food.y] = ElementType.FOOD

        try {
            snake
                .body
                .forEach {
                    elements[it.first][it.second] = ElementType.SNAKE
                }
        } catch (ex: Exception) {
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
}