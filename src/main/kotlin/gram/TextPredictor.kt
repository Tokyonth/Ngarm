package gram

import utils.loadNGramModel
import utils.saveGramModel
import java.io.File

/**
 * 预测器主类
 * 提供基于统计学习的文本预测功能
 */
class TextPredictor(
    modelPath: String,
    n: Int = 3,
    sampleTexts: List<String>? = null
) {

    /** N元语法模型实例 */
    private var model: NGramModel

    private val modelFile = File(modelPath)

    /** 用户输入历史记录 */
    private val userHistory: MutableList<String> = mutableListOf()

    init {
        println("预测器初始化中，模型路径：$modelPath")

        // 创建新模型或加载现有模型
        when {
            modelFile.exists() -> {
                println("从 $modelPath 加载现有模型...")
                try {
                    model = modelFile.loadNGramModel()
                    println("模型从 $modelPath 加载成功")
                    println(getModelInfo())
                } catch (e: Exception) {
                    println("加载模型时出错：${e.message}")
                    e.printStackTrace()
                    model = NGramModel(n = n)
                    println("加载失败后创建了新模型作为备用")
                }
            }

            else -> {
                println("创建新的N元语法模型，n=$n")
                model = NGramModel(n = n)
                // 如果提供了示例文本且正在创建新模型，则进行预训练
                sampleTexts?.let { trainSample(it) }
            }
        }
    }

    private fun trainSample(texts: List<String>) {
        println("使用 ${texts.size} 个示例文本进行预训练")
        texts.forEachIndexed { index, text ->
            println("训练示例文本 ${index + 1}/${texts.size}，长度：${text.length}")
            model.train(text)
        }
        println("预训练完成。模型统计：词汇量=${model.vocabulary.size}，总词数=${model.totalWords}")

        // 保存初始模型
        println("保存初始模型中...")
        saveModel()
    }

    /**
     * 将用户输入添加到历史记录中以便后续训练
     * @param text 用户输入的文本
     */
    fun addToHistory(text: String) {
        println("添加到历史记录：'$text'")
        userHistory.add(text)

        // 记录当前历史状态
        println("当前历史记录大小：${userHistory.size}/100")

        // 如果历史记录过长，进行训练并清空
        if (userHistory.size >= 100) {
            println("历史记录达到阈值（100条）。开始训练模型...")
            trainOnHistory()
        }
    }

    /**
     * 使用收集的用户历史记录训练模型
     */
    private fun trainOnHistory() {
        if (userHistory.isEmpty()) {
            println("没有历史记录可供训练。跳过训练。")
            return
        }

        // 合并所有历史记录并进行训练
        val allText = userHistory.joinToString(" ")
        println("使用合并的历史文本进行训练，长度：${allText.length}")
        model.train(allText)
        println("训练完成。更新后的模型统计：词汇量=${model.vocabulary.size}，总词数=${model.totalWords}")

        // 保存更新后的模型
        println("训练后保存模型中...")
        saveModel()

        // 训练后清空历史记录
        val historyLength = userHistory.size
        userHistory.clear()
        println("训练后清空了 $historyLength 条历史记录")
    }

    /**
     * 根据上下文预测下一个词
     * @param context 上下文文本
     * @param numPredictions 预测数量
     * @return 预测结果列表
     */
    fun predict(context: String, numPredictions: Int = 3): List<Pair<String, Double>> {
        println("为上下文预测下一个词：'$context'")
        println("预测生成数量：$numPredictions")
        val predictions = model.predictNextWord(context, numPredictions)
        println("预测结果：")
        predictions.forEach {
            println("预测：" + it.first + " 概率：" + it.second)
        }
        return predictions
    }

    /**
     * 将模型保存到指定路径
     */
    private fun saveModel() {
        try {
            println("尝试将模型保存到 ${modelFile.path}")

            // 确保目录存在
            val modelDir = modelFile.parentFile
            if (modelDir != null && !modelDir.exists()) {
                println("创建目录：${modelDir.absolutePath}")
                modelDir.mkdirs()
            }

            // 保存模型
            model.saveGramModel(modelFile.path)

            // 验证文件是否创建成功
            if (modelFile.exists()) {
                println("模型成功保存到 ${modelFile.absolutePath}")
                println(getModelInfo())
            } else {
                println("警告：保存尝试后在 ${modelFile.path} 未找到模型文件")
            }
        } catch (e: Exception) {
            println("保存模型时出错：${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 获取当前模型的统计信息
     * @return 包含模型统计信息的字符串
     */
    fun getModelInfo(): String {
        val fileSize = modelFile.length()
        return "模型统计信息：\n" +
                "大小：$fileSize 字节 \n" +
                "词汇量：${model.vocabulary.size} \n" +
                "总词数：${model.totalWords} \n" +
                "历史记录：${userHistory.size} 条 \n" +
                "N元语法大小：${model.n}"
    }

    /**
     * 清空用户历史记录
     */
    fun clearHistory() {
        val clearedCount = userHistory.size
        userHistory.clear()
        println("已清空 $clearedCount 条历史记录")
    }

    /**
     * 手动触发基于当前历史的训练
     * @return 是否训练成功
     */
    fun forceTraining(): Boolean {
        return try {
            trainOnHistory()
            true
        } catch (e: Exception) {
            println("强制训练时出错：${e.message}")
            e.printStackTrace()
            false
        }
    }

}
