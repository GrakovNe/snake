package org.grakovne.snake

import java.awt.*
import javax.swing.*
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.plot.XYPlot
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection

class UIKit(xSize: Int, ySize: Int) {

    private val squares = Squares()
    private val currentScore = JLabel()
    private val about = JLabel()
    private val series = XYSeries("Snake Length")
    private var steps = 0

    init {
        val frame = JFrame("org.grakovne.Snake")

        val panel = frame.contentPane as JPanel
        panel.layout = null

        frame.setBounds(450, 250, 1000, 100)
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.preferredSize = Dimension(xSize * 10 + 75 + 350, ySize * 10 + 75)
        frame.pack()
        frame.isVisible = true

        squares.setBounds(0, 0, xSize * 10 + 30, ySize * 10 + 30)

        currentScore.setBounds(xSize * 10 + 135, ySize, 200, 50)
        currentScore.isVisible = true
        currentScore.font = currentScore.font.deriveFont(36.0.toFloat())
        currentScore.font = currentScore.font.deriveFont(1)
        currentScore.horizontalAlignment = SwingConstants.CENTER

        about.setBounds(xSize * 10 + 135, 10 * ySize - 10, 200, 50)
        about.isVisible = true
        about.text = "https://github.com/GrakovNe/snake"
        about.font = about.font.deriveFont(11.0.toFloat())
        about.font = about.font.deriveFont(0)
        about.horizontalAlignment = SwingConstants.CENTER

        frame.contentPane.add(squares)
        frame.contentPane.add(currentScore)
        frame.contentPane.add(about)

        val dataset = XYSeriesCollection(series)
        val chart = ChartFactory.createXYLineChart(
            "",
            "Steps",
            "Size",
            dataset,
            PlotOrientation.VERTICAL,
            false,
            true,
            false
        )

        val plot = chart.xyPlot
        plot.backgroundAlpha = 0.0f

        val trans = Color(0xFF, 0xFF, 0xFF, 0)
        chart.backgroundPaint = trans
        plot.backgroundPaint = trans

        val chartPanel = ChartPanel(chart)
        chartPanel.setBounds(xSize * 10 + 35, ySize * 3 + 30, 370, 230)
        chartPanel.isOpaque = true

        frame.contentPane.add(chartPanel)
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
        synchronized(squares) {
            squares.clear()

            for (i in 0 until field.getWidth()) {
                for (j in 0 until field.getHeight()) {
                    squares.addSquare(30 + i * 10, 30 + j * 10, 10, 10, toColor(i, j, field))
                }
            }
        }

        val score = field.getCells().count { it.first == ElementType.SNAKE }
        currentScore.text = String.format("%08d", score)

        steps++
        series.add(steps, score)

        squares.repaint()
    }

    class Squares : JPanel() {
        private val squares: MutableList<Cell> = ArrayList()

        init {
            isDoubleBuffered = true  // Enable double buffering explicitly
        }

        fun addSquare(x: Int, y: Int, width: Int, height: Int, color: Color) {
            val rect = Cell(Rectangle(x, y, width, height), color)
            synchronized(this) {
                squares.add(rect)
            }
        }

        fun clear() {
            synchronized(this) {
                squares.clear()
            }
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)

            val g2 = g as Graphics2D
            synchronized(this) {
                squares.forEach { rect ->
                    g2.color = rect.color
                    g2.fillRect(rect.rectangle.x, rect.rectangle.y, rect.rectangle.width, rect.rectangle.height)
                }
            }
        }
    }

    internal data class Cell(val rectangle: Rectangle, val color: Color)
}
