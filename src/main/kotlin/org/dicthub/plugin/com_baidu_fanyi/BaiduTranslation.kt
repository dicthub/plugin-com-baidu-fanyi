package org.dicthub.plugin.com_baidu_fanyi

data class BaiduTranslation(
        val from: String,
        val to: String,
        val query: String,
        val translation: String,
        val pron: String? = null,
        val details: List<TranslationDetails>
)

data class TranslationDetails(
        val poc: String,
        val meanings: List<String>
)