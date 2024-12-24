package org.grakovne.snake

import kotlinx.coroutines.runBlocking

class Show(size: Int) {
    private val uiKit = UIKit(size, size)

    fun display(runner: SnakeRunner) = runBlocking {
        runner.run().collect { (field, _) ->
            uiKit.showField(field)
        }
    }
}

fun main(args: Array<String>) {
    val size = 60
//    val weights = listOf(
//        1.396377081088321,
//        1.8421232025250636,
//        1.3401581518527927,
//        2.9250468744771045,
//        3.0
//    )
    val weights = listOf(
        1.5375991995990936, 1.3756970286318968, 1.5449809292403884, 1.4912197864049481, 0.4195186675524982
    )

    val runner = SnakeRunner(size, weights)
    val show = Show(size)
    show.display(runner)
}
