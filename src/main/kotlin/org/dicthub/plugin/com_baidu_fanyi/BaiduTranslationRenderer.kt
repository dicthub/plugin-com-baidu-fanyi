package org.dicthub.plugin.com_baidu_fanyi

import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import org.dicthub.plugin.shared.util.builtinSourceIcon
import org.dicthub.plugin.shared.util.renderSource

class BaiduTranslationRenderer {

    fun render(t: BaiduTranslation): String {

        val stringBuilder = StringBuilder()
        val container = stringBuilder.appendHTML()

        container.div(classes = "t-result") {

            div(classes = "alert alert-info") {
                em(classes = "translation-lang") {
                    +"[${t.from}]"
                }
                strong {
                    +t.query
                }
                +" "
                span(classes = "translation-pronunciation") {
                    em {
                        t.pron?.let { +"[$it]" }
                    }
                }
                span(classes = "translation-voice") {
                    audio {
                        src = voiceUrl(t.from, t.query)
                    }
                }
            }

            ul(classes = "list-group") {
                li(classes = "list-group-item") {
                    em(classes = "translation-lang") {
                        +"[${t.to}]"
                    }
                    +t.translation

                    if (t.details.isNotEmpty()) {
                        val detailId = "baiduDetail"
                        a(classes = "btn btn-light btn-sm mb-2", href = "#$detailId") {
                            role = "button"
                            attributes["data-toggle"] = "collapse"
                            +"\uD83D\uDCDA"
                        }
                        div(classes = "collapse") {
                            id = detailId
                            ul(classes = "list-group") {
                                t.details.forEach { translationDetail ->
                                    li(classes = "list-group-item small") {
                                        i(classes = "translation-poc") { +translationDetail.poc }
                                        span(classes = "translation-primary") { +translationDetail.meanings.joinToString("; ") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        renderSource(container, sourceUrl(t), builtinSourceIcon(ID))

        return stringBuilder.toString()
    }

    private fun sourceUrl(t: BaiduTranslation) = "$BASE_URL/#${t.from}/${t.to}/${t.query}"

    private fun voiceUrl(lang: String, text: String) = "$BASE_URL/gettts?spd=3&source=web&lan=$lang&text=$text"
}