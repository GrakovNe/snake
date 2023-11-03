package org.grakovne.snake

import org.grakovne.snake.Field.Companion.copy
import java.util.PriorityQueue
import kotlin.math.abs

class GptStrategy {

    fun getMove(snake: Snake, field: Field, food: Food): Direction {
        val headX = snake.head().first
        val headY = snake.head().second

        val availableMoves = mutableListOf<Direction>()

        // Проверяем доступные направления
        if (field.getCellType(headX - 1, headY) != ElementType.BORDER && !snake.willAteSelf(Direction.UP)) {
            availableMoves.add(Direction.UP)
        }
        if (field.getCellType(headX + 1, headY) != ElementType.BORDER && !snake.willAteSelf(Direction.DOWN)) {
            availableMoves.add(Direction.DOWN)
        }
        if (field.getCellType(headX, headY - 1) != ElementType.BORDER && !snake.willAteSelf(Direction.LEFT)) {
            availableMoves.add(Direction.LEFT)
        }
        if (field.getCellType(headX, headY + 1) != ElementType.BORDER && !snake.willAteSelf(Direction.RIGHT)) {
            availableMoves.add(Direction.RIGHT)
        }

        // Если доступных направлений нет, то игра закончена
        if (availableMoves.isEmpty()) {
            return Direction.DOWN
        }

        // Создаем список для хранения путей и их оценок
        val paths = mutableListOf<Pair<Direction, Int>>()

        // Прогнозируем несколько ходов вперед
        val lookaheadDepth = 3  // Количество ходов вперед для прогноза

        for (direction in availableMoves) {
            var simulationSnake = snake.copy()  // Создаем копию змейки для симуляции

            // Симулируем движение змейки на несколько ходов вперед
            for (i in 0 until lookaheadDepth) {
                val nextX = when (direction) {
                    Direction.UP -> simulationSnake.head().first - 1
                    Direction.DOWN -> simulationSnake.head().first + 1
                    else -> simulationSnake.head().first
                }

                val nextY = when (direction) {
                    Direction.LEFT -> simulationSnake.head().second - 1
                    Direction.RIGHT -> simulationSnake.head().second + 1
                    else -> simulationSnake.head().second
                }

                if (nextX < 0 || nextX >= field.getWidth() || nextY < 0 || nextY >= field.getHeight()) {
                    break
                }

                // Если следующий ход столкнется со стеной или хвостом, прекращаем симуляцию
                if (field.getCellType(nextX, nextY) == ElementType.BORDER || simulationSnake.willAteSelf(direction)) {
                    break
                }

                // Если следующий ход приводит к еде, учитываем это в оценке
                if (nextX == food.x && nextY == food.y) {
                    simulationSnake.grow()
                }

                // Двигаем змейку
                simulationSnake.move(direction)
            }

            // Оценка пути (с учетом и до еды, и до хвоста в перспективе)
            val distanceToFood = abs(simulationSnake.head().first - food.x) + abs(simulationSnake.head().second - food.y)
            val tailDistance = simulationSnake.body.size - 1  // Длина хвоста в перспективе

            val totalDistance = distanceToFood + tailDistance

            paths.add(direction to totalDistance)
        }

        // Сортируем направления по оценкам (по возрастанию)
        val sortedPaths = paths.sortedBy { it.second }

        // Возвращаем наилучшее направление (с наименьшей оценкой)
        return sortedPaths.first().first
    }


}