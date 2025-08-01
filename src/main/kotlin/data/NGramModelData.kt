package data

import java.io.*

/**
 * N元语法模型数据类
 */
data class NGramModelData(
    val n: Int,
    val smoothing: Double,
    val models: Map<Int, Map<List<String>, Map<String, Int>>>,
    val wordCount: Map<String, Int>,
    val totalWords: Int,
    val vocabulary: Set<String>
) : Serializable
