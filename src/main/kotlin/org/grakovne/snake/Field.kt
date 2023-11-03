package org.grakovne.snake

class Field(private val xSize: Int, private val ySize: Int) {
    private val elements: Array<Array<ElementType>> = Array(xSize) { Array(ySize) { ElementType.EMPTY } }

    init {
        drawBorders()
    }

    fun setCellType(x: Int, y: Int, elementType: ElementType) {
        if (x in 0 until xSize && y in 0 until ySize) {
            elements[x][y] = elementType
        } else {
            throw IllegalArgumentException("Coordinates ($x, $y) are out of bounds.")
        }
    }

    fun copy(): Field {
        val newField = Field(xSize, ySize)

        for (i in 0 until xSize) {
            for (j in 0 until ySize) {
                newField.elements[i][j] = elements[i][j]
            }
        }

        return newField
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
        fun Field.isCellFree(cell: Pair<Int, Int>): Boolean {
            val (x, y) = cell
            return x in 0 until this.getWidth() &&
                    y in 0 until this.getHeight() &&
                    this.getCellType(x, y) != ElementType.BORDER &&
                    this.getCellType(x, y) != ElementType.SNAKE
        }

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