package utils

import data.NGramModelData
import gram.NGramModel
import java.io.File
import java.io.FileNotFoundException

fun File.loadNGramModel(): NGramModel {
    return load(this.path)
}

fun NGramModel.saveGramModel(path: String) {
    save(path, this)
}

/**
 * 从文件加载模型
 * @param filepath 文件路径
 * @return 加载的模型实例
 */
fun load(filepath: String): NGramModel {
    val data = FileSerializationUtils.readObject<NGramModelData>(filepath)
        ?: throw FileNotFoundException("读取错误: $filepath")

    val model = NGramModel(n = data.n, smoothing = data.smoothing)

    // 将数据转换回可变集合
    data.models.forEach { (nSize, contextMap) ->
        model.models[nSize] = contextMap.mapValues { (_, wordMap) ->
            wordMap.toMutableMap()
        }.toMutableMap()
    }

    model.wordCount.putAll(data.wordCount)
    model.totalWords = data.totalWords
    model.vocabulary.addAll(data.vocabulary)

    return model
}

/**
 * 将模型保存到文件
 * @param filepath 文件路径
 */
fun save(filepath: String, model: NGramModel) {
    val data = NGramModelData(
        n = model.n,
        smoothing = model.smoothing,
        models = model.models,
        wordCount = model.wordCount.toMap(),
        totalWords = model.totalWords,
        vocabulary = model.vocabulary.toSet()
    )
    FileSerializationUtils.writeObject(data, filepath)
}
