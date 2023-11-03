package org.grakovne.snake

import java.awt.*
import java.util.*
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants


class UIKit(xSize: Int, ySize: Int) {

    private val squares = Squares()
    private val currentScore = JLabel()

    init {
        val frame = JFrame("org.grakovne.snake.Snake")

        val panel = frame.contentPane as JPanel
        panel.layout = null

        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.preferredSize = Dimension(xSize * 10 + 75 + 230, ySize * 10 + 75)
        frame.pack()
        frame.isVisible = true

        squares.setBounds(0, 0, xSize * 10 + 75, ySize * 10 + 75)

        currentScore.setBounds(xSize * 10 + 75, 360, 200, 100)
        currentScore.isVisible = true
        currentScore.font = currentScore.font.deriveFont(36.0.toFloat())
        currentScore.font = currentScore.font.deriveFont(1)
        currentScore.horizontalAlignment = SwingConstants.LEFT

        frame.contentPane.add(squares)
        frame.contentPane.add(currentScore)
    }

    private fun toColor(xPos: Int, yPos: Int, field: Field): Color {
        return when (field.getCellType(xPos, yPos)) {
            ElementType.EMPTY -> Color.WHITE
            ElementType.SNAKE -> Color.BLACK
            ElementType.BORDER -> Color.GRAY
            ElementType.FOOD -> Color.MAGENTA
            ElementType.SNAKE_HEAD -> Color.RED
        }
    }

    fun showField(field: Field) {
        squares.clear(Color.WHITE)

        for (i in 0 until field.getWidth()) {
            for (j in 0 until field.getHeight()) {
                squares.addSquare(30 + i * 10, 30 + j * 10, 10, 10, toColor(j, i, field))
            }
        }

        val score = field.getCells().count { it.first == ElementType.SNAKE }
        currentScore.text = String.format("%0" + (8 - score.toString().length) + "d%s", 0, score.toString())

        squares.repaint()
    }

    class Squares : JPanel() {
        private val squares: MutableList<Cell> = ArrayList()

        fun addSquare(x: Int, y: Int, width: Int, height: Int, color: Color) {
            val rect = Cell(Rectangle(x, y, width, height), color)
            squares.add(rect)
        }

        fun clear(color: Color) {
            squares.clear()
        }


        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)

            val g2 = g as Graphics2D
            Vector(squares)
                .forEach { rect ->
                    g2.color = rect.color
                    g2.fillRect(rect.rectangle.x, rect.rectangle.y, rect.rectangle.width, rect.rectangle.height)
                }
        }
    }

    internal data class Cell(val rectangle: Rectangle, val color: Color)
}