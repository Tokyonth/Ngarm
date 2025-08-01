package org.example

import gram.TextPredictor

import java.io.File

private const val MODEL_ORDER = 3
private const val PREDICTOR_NUM = 5

val dataFile = File("src/main/resources/data/pod.txt")
val modelFile = File("src/main/resources/model/pod.ser")

fun main() {
    val data = if (modelFile.exists()) {
        null
    } else {
        dataFile.bufferedReader().use { br ->
            br.readLines()
        }
    }
    val predictor = TextPredictor(modelFile.path, n = MODEL_ORDER, sampleTexts = data)

    var inputFlag = true
    while (inputFlag) {
        println("请输入: ")
        val inputLine = readlnOrNull() ?: ""
        if (inputLine == "exit") {
            inputFlag = false
        } else {
            predictor.predict(inputLine, PREDICTOR_NUM)
        }
    }
}
