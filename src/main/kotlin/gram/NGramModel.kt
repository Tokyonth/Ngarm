package gram

/**
 * N元语法模型类
 * 用于基于统计的文本预测
 */
class NGramModel(
    val n: Int = 3,
    val smoothing: Double = 0.1
) {

    /** 不同n元语法模型（一元、二元、三元语法） */
    val models: MutableMap<Int, MutableMap<List<String>, MutableMap<String, Int>>> = mutableMapOf()

    /** 词频统计，用于一元语法概率计算 */
    val wordCount: MutableMap<String, Int> = mutableMapOf()

    /** 总词数 */
    var totalWords: Int = 0

    /** 词汇表 */
    val vocabulary: MutableSet<String> = mutableSetOf()

    /**
     * 文本预处理和分词
     * @param text 输入文本
     * @return 分词后的词列表
     */
    private fun preprocessText(text: String): List<String> {
        // 转换为小写并替换标点符号为空格
        val cleanText = text.lowercase()
            .replace(Regex("[^\\w\\s']"), " ")

        // 分词并过滤空字符串
        return cleanText.split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
    }

    /**
     * 从词列表生成n元语法
     * @param words 词列表
     * @param n n元语法的大小
     * @return n元语法列表
     */
    private fun buildNgrams(words: List<String>, n: Int): List<List<String>> {
        val ngrams = mutableListOf<List<String>>()
        for (i in 0..words.size - n) {
            ngrams.add(words.subList(i, i + n))
        }
        return ngrams
    }

    /**
     * 使用给定文本训练模型
     * @param text 训练文本
     */
    fun train(text: String) {
        val words = preprocessText(text)
        if (words.isEmpty()) return

        // 更新词汇表和词频统计
        vocabulary.addAll(words)
        words.forEach { word ->
            wordCount[word] = wordCount.getOrDefault(word, 0) + 1
        }
        totalWords += words.size

        // 训练不同大小的n元语法模型（2到n）
        for (i in 2..n) {
            val ngrams = buildNgrams(words, i)

            // 为当前n元语法大小创建或更新模型
            models.computeIfAbsent(i) { mutableMapOf() }

            // 统计n元语法出现次数
            ngrams.forEach { ngram ->
                val context = ngram.dropLast(1) // 除最后一个词外的所有词
                val word = ngram.last() // 最后一个词

                val contextMap = models[i]!!.computeIfAbsent(context) { mutableMapOf() }
                contextMap[word] = contextMap.getOrDefault(word, 0) + 1
            }
        }
    }

    /**
     * 根据上下文预测下一个词
     * @param context 上下文字符串
     * @param numPredictions 返回的预测数量
     * @return 预测词及其概率的列表
     */
    fun predictNextWord(context: String, numPredictions: Int = 3): List<Pair<String, Double>> {
        val words = preprocessText(context)

        // 如果上下文中没有有效词，返回最常见的词
        if (words.isEmpty()) {
            return wordCount.entries
                .sortedByDescending { it.value }
                .take(numPredictions)
                .map {
                    val total = if (totalWords > 0) totalWords.toDouble() else 1.0
                    it.key to (it.value.toDouble() / total)
                }
        }

        val candidates = mutableMapOf<String, Double>()

        // 尝试使用适合当前上下文的最大n元语法模型
        for (nSize in minOf(n, words.size + 1) downTo 2) {
            val contextTuple = words.takeLast(nSize - 1)

            // 如果在模型中找到了这个上下文
            val counter = models[nSize]?.get(contextTuple)
            if (counter != null) {
                val total = counter.values.sum()
                val vocabSize = vocabulary.size

                // 使用平滑技术计算概率
                counter.forEach { (word, count) ->
                    val prob = (count + smoothing) / (total + smoothing * vocabSize)
                    candidates[word] = candidates.getOrDefault(word, 0.0) + prob
                }
            }

            // 如果找到足够的预测，跳出循环
            if (candidates.size >= numPredictions) {
                break
            }
        }

        // 如果预测不够，回退到一元语法
        if (candidates.size < numPredictions) {
            val remaining = numPredictions - candidates.size
            val total = if (totalWords > 0) totalWords.toDouble() else 1.0
            val vocabSize = vocabulary.size.takeIf { it > 0 } ?: 1

            wordCount.entries
                .sortedByDescending { it.value }
                .asSequence()
                .filter { it.key !in candidates }
                .take(remaining)
                .forEach { (word, count) ->
                    val prob = (count + smoothing) / (total + smoothing * vocabSize)
                    candidates[word] = prob
                }
        }

        // 返回概率最高的预测
        return candidates.entries
            .sortedByDescending { it.value }
            .take(numPredictions)
            .map { it.key to it.value }
    }

}
