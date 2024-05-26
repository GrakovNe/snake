package org.grakovne.snake

import java.awt.*
import java.util.*
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.BorderFactory

class UIKit(xSize: Int, ySize: Int) {

    private val squares = Squares()
    private val currentScore = JLabel()
    private val about = JLabel()

    init {
        val frame = JFrame("org.grakovne.Snake")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.layout = BorderLayout()
        frame.preferredSize = Dimension(xSize * 10 + 75 + 230, ySize * 10 + 75)

        val mainPanel = JPanel(BorderLayout())
        val scorePanel = JPanel(BorderLayout())
        val aboutPanel = JPanel(BorderLayout())

        currentScore.horizontalAlignment = SwingConstants.CENTER
        currentScore.font = currentScore.font.deriveFont(36.0f).deriveFont(Font.BOLD)
        scorePanel.add(currentScore, BorderLayout.CENTER)

        about.text = "https://github.com/GrakovNe/snake"
        about.horizontalAlignment = SwingConstants.CENTER
        about.font = about.font.deriveFont(11.0f).deriveFont(Font.PLAIN)
        aboutPanel.add(about, BorderLayout.SOUTH)

        val rightPanel = JPanel(BorderLayout())
        rightPanel.preferredSize = Dimension(200, ySize * 10 + 75)
        rightPanel.add(scorePanel, BorderLayout.NORTH)
        rightPanel.add(aboutPanel, BorderLayout.SOUTH)

        mainPanel.add(squares, BorderLayout.CENTER)
        mainPanel.add(rightPanel, BorderLayout.EAST)

        frame.add(mainPanel)
        frame.pack()
        frame.isVisible = true
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
        squares.clear()

        for (i in 0 until field.getWidth()) {
            for (j in 0 until field.getHeight()) {
                squares.addSquare(30 + i * 10, 30 + j * 10, 10, 10, toColor(i, j, field))
            }
        }

        val score = field.getCells().count { it.first == ElementType.SNAKE }
        currentScore.text = String.format("%08d", score)

        squares.repaint()
    }

    class Squares : JPanel() {
        private val squares: MutableList<Cell> = ArrayList()

        init {
            border = BorderFactory.createLineBorder(Color.BLACK)
        }

        fun addSquare(x: Int, y: Int, width: Int, height: Int, color: Color) {
            val rect = Cell(Rectangle(x, y, width, height), color)
            squares.add(rect)
        }

        fun clear() {
            squares.clear()
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)

            val g2 = g as Graphics2D
            squares.forEach { rect ->
                g2.color = rect.color
                g2.fillRect(rect.rectangle.x, rect.rectangle.y, rect.rectangle.width, rect.rectangle.height)
            }
        }
    }

    internal data class Cell(val rectangle: Rectangle, val color: Color)
}
