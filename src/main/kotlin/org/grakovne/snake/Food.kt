package org.grakovne.snake

import kotlin.random.Random

class Food(private val xSize: Int, private val ySize: Int) {
    val x: Int = Random.nextInt(1, xSize - 1)
    val y: Int = Random.nextInt(1, ySize - 1)
}