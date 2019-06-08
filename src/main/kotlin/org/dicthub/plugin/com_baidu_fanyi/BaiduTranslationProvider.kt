package org.dicthub.plugin.com_baidu_fanyi

import org.dicthub.plugin.shared.util.*
import org.w3c.dom.get
import org.w3c.dom.set
import kotlin.browser.localStorage
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json


const val ID = "plugin-com-baidu-fanyi"

const val BASE_URL = "https://fanyi.baidu.com"

class BaiduTranslationProvider constructor(
        private val httpClient: HttpAsyncClient,
        private val renderer: BaiduTranslationRenderer)
    : TranslationProvider {

    override fun id() = ID

    override fun meta() = createMeta(
            name = "Baidu Multi-language Translation",
            description = "Support up to 28 language translation",
            source = "Baidu Fanyi",
            sourceUrl = "https://fanyi.baidu.com/",
            author = "DictHub",
            authorUrl = "https://github.com/willings/DictHub"
    )

    override fun canTranslate(query: Query): Boolean {

        // Skip checking supported lang
        return langCodeMap[query.getFrom()] is String
                && langCodeMap[query.getTo()] is String
                && query.getFrom() != query.getTo()
    }

    override fun translate(query: Query): Promise<String> {
        val translateWithNewToken = fun(resolve: (String) -> Unit) {
            newTokenPromise().then { newToken ->
                saveToken(newToken)
                console.info("Translate and saved new token: $newToken")
                translaeWithToken(newToken, query, true).then(resolve)
            }
        }
        return Promise { resolve, _ ->
            loadCachedToken()?.let { cachedToken ->
                console.info("Translate using cached baidu token: $cachedToken")
                translaeWithToken(cachedToken, query, false).then(resolve).catch {
                    translateWithNewToken(resolve)
                }
            }?: run {
                translateWithNewToken(resolve)
            }
        }
    }

    private val tokenStorageKey = "plugin-baidu-token"
    private fun loadCachedToken(): Token? {
        return localStorage[tokenStorageKey]?.match("(.*):(.*)")?.let { Token(it[1], it[2]) }
    }
    private fun saveToken(token: Token) {
        localStorage[tokenStorageKey] = "${token.token}:${token.gtk}"
    }

    private fun translaeWithToken(token: Token, query: Query, renderError: Boolean): Promise<String> {
        return Promise { resolve, reject ->
            callBaiduApi(query, token).convert { parseTranslationData(it) }.then {
                resolve(renderer.render(it))
            }.catch {
                if (renderError) {
                    resolve(renderFailure(id(), sourceUrl(query), query, it))
                } else {
                    reject(it)
                }
            }
        }
    }

    private fun parseTranslationData(jsonStr: String) = parseTranslationResult(JSON.parse<Json>(jsonStr))

    private fun parseTranslationResult(data: Json): BaiduTranslation {
        val translationResult = data.value<Json>("trans_result") ?: throw TranslationNotFoundException()
        val from = translationResult.value<String>("from") ?: throw TranslationParsingFailureException()
        val to = translationResult.value<String>("to") ?: throw TranslationParsingFailureException()
        val translation = translationResult.value<Array<Json>>("data")?.firstOrNull()
                ?: throw TranslationNotFoundException()
        val symbol = data.value<Json>("dict_result")?.value<Json>("simple_means")?.value<Array<Json>>("symbols")?.firstOrNull()
        val pron = when(to) {
            "en" -> symbol?.value<String>("ph_am") ?: symbol?.value<String>("ph_en")
            "zh" -> symbol?.value<String>("word_symbol")
            else -> null
        }
        val details: List<TranslationDetails>? = when(from) {
            "zh" -> symbol?.value<Array<Json>>("parts")
                    ?.flatMap { it.value<Array<Json>>("means")?.toList() ?: emptyList()}
                    ?.groupBy { it["part"] as? String }
                    ?.map {
                TranslationDetails(
                        poc = it.key ?: "",
                        meanings = it.value.mapNotNull { it["text"] as? String }
                )
            }
            else -> symbol?.value<Array<Json>>("parts")?.map {
                TranslationDetails(
                        poc = it.value<String>("part") ?: "",
                        meanings = it.value<Array<String>>("means")?.toList() ?: emptyList()
                )
            }
        }

        return BaiduTranslation(
                from = from,
                to = to,
                query = translation.value<String>("src") ?: throw TranslationParsingFailureException(),
                translation = translation.value<String>("dst") ?: throw TranslationParsingFailureException(),
                pron = pron,
                details = details ?: emptyList()
        )
    }

    private fun callBaiduApi(query: Query, token: Token) =
        httpClient.post("$BASE_URL/v2transapi", mapOf("Content-Type" to "application/x-www-form-urlencoded"),
                buildFormData(query.getFrom(), query.getTo(), query.getText(), signQuery(query.getText(), token.gtk), token.token))

    private fun buildFormData(from: String, to: String, query: String, sign: String, token: String) =
            "from=${baiduLangCode(from)}&to=${baiduLangCode(to)}&query=$query&simple_means_flag=3&sign=$sign&token=$token"
}

private val langCodeMap = json(
        "en" to "en",
        "ar" to "ara",
        "et" to "est",
        "bg" to "bul",
        "pl" to "pl",
        "da" to "dan",
        "de" to "de",
        "ru" to "ru",
        "fr" to "fra",
        "fi" to "fin",
        "ko" to "kor",
        "nl" to "nl",
        "cs" to "cs",
        "ro" to "rom",
        "pt" to "pt",
        "ja" to "jp",
        "sv" to "swe",
        "sk" to "slo",
        "th" to "th",
        "es" to "spa",
        "el" to "el",
        "hu" to "hu",
        "it" to "it",
        "vi" to "vie",
        "zh-CN" to "zh",
        "zh-TW" to "cht"
)

// private val supportedLang: Json = js("{'zh': ['en','ara','est','bul','pl','dan','de','ru','fra','fin','kor','nl','cs','rom','pt','jp','swe','slo','th','wyw','spa','el','hu','it','yue','cht','vie'],'en': ['zh','ara','est','bul','pl','dan','de','ru','fra','fin','kor','nl','cs','rom','pt','jp','swe','slo','th','wyw','spa','el','hu','it','yue','cht','vie'],'ara': ['zh','en','est','bul','pl','dan','de','ru','fra','fin','kor','nl','cs','rom','pt','jp','swe','slo','th','wyw','spa','el','hu','it','yue','cht','vie'],'est': ['zh','en','ara','bul','pl','dan','de','ru','fra','fin','kor','nl','cs','rom','pt','jp','swe','slo','th','wyw','spa','el','hu','it','yue','cht','vie'],'bul': ['zh','en','ara','est','pl','dan','de','ru','fra','fin','kor','nl','cs','rom','pt','jp','swe','slo','th','wyw','spa','el','hu','it','yue','cht','vie'],'pl': ['zh','en','ara','est','bul','dan','de','ru','fra','fin','kor','nl','cs','rom','pt','jp','swe','slo','th','wyw','spa','el','hu','it','yue','cht','vie'],'dan': ['zh','en','ara','est','bul','pl','de','ru','fra','fin','kor','nl','cs','rom','pt','jp','swe','slo','th','wyw','spa','el','hu','it','yue','cht','vie'],'de': ['zh','en','ara','est','bul','pl','dan','ru','fra','fin','kor','nl','cs','rom','pt','jp','swe','slo','th','wyw','spa','el','hu','it','yue','cht','vie'],'ru': ['zh','en','ara','est','bul','pl','dan','de','fra','fin','kor','nl','cs','rom','pt','jp','swe','slo','th','wyw','spa','el','hu','it','yue','cht','vie'],'fra': ['zh','en','ara','est','bul','pl','dan','de','ru','fin','kor','nl','cs','rom','pt','jp','swe','slo','th','wyw','spa','el','hu','it','yue','cht','vie'],'fin': ['zh','en','ara','est','bul','pl','dan','de','ru','fra','kor','nl','cs','rom','pt','jp','swe','slo','th','wyw','spa','el','hu','it','yue','cht','vie'],'kor': ['zh','en','ara','est','bul','pl','dan','de','ru','fra','fin','nl','cs','rom','pt','jp','swe','slo','th','wyw','spa','el','hu','it','yue','cht','vie'],'nl': ['zh','en','ara','est','bul','pl','dan','de','ru','fra','fin','kor','cs','rom','pt','jp','swe','slo','th','wyw','spa','el','hu','it','yue','cht','vie'],'cs': ['zh','en','ara','est','bul','pl','dan','de','ru','fra','fin','kor','nl','rom','pt','jp','swe','slo','th','wyw','spa','el','hu','it','yue','cht','vie'],'rom': ['zh','en','ara','est','bul','pl','dan','de','ru','fra','fin','kor','nl','cs','pt','jp','swe','slo','th','wyw','spa','el','hu','it','yue','cht','vie'],'pt': ['zh','en','ara','est','bul','pl','dan','de','ru','fra','fin','kor','nl','cs','rom','jp','swe','slo','th','wyw','spa','el','hu','it','yue','cht','vie'],'jp': ['zh','en','ara','est','bul','pl','dan','de','ru','fra','fin','kor','nl','cs','rom','pt','swe','slo','th','wyw','spa','el','hu','it','yue','cht','jpka','vie'],'swe': ['zh','en','ara','est','bul','pl','dan','de','ru','fra','fin','kor','nl','cs','rom','pt','jp','slo','th','wyw','spa','el','hu','it','yue','cht','vie'],'slo': ['zh','en','ara','est','bul','pl','dan','de','ru','fra','fin','kor','nl','cs','rom','pt','jp','swe','th','wyw','spa','el','hu','it','yue','cht','vie'],'th': ['zh','en','ara','est','bul','pl','dan','de','ru','fra','fin','kor','nl','cs','rom','pt','jp','swe','slo','wyw','spa','el','hu','it','yue','cht','vie'],'wyw': ['zh','en','ara','est','bul','pl','dan','de','ru','fra','fin','kor','nl','cs','rom','pt','jp','swe','slo','th','spa','el','hu','it','yue','cht','vie'],'spa': ['zh','en','ara','est','bul','pl','dan','de','ru','fra','fin','kor','nl','cs','rom','pt','jp','swe','slo','th','wyw','el','hu','it','yue','cht','vie'],'el': ['zh','en','ara','est','bul','pl','dan','de','ru','fra','fin','kor','nl','cs','rom','pt','jp','swe','slo','th','wyw','spa','hu','it','yue','cht','vie'],'hu': ['zh','en','ara','est','bul','pl','dan','de','ru','fra','fin','kor','nl','cs','rom','pt','jp','swe','slo','th','wyw','spa','el','it','yue','cht','vie'],'it': ['zh','en','ara','est','bul','pl','dan','de','ru','fra','fin','kor','nl','cs','rom','pt','jp','swe','slo','th','wyw','spa','el','hu','yue','cht','vie'],'yue': ['zh','en','ara','est','bul','pl','dan','de','ru','fra','fin','kor','nl','cs','rom','pt','jp','swe','slo','th','wyw','spa','el','hu','it','cht','vie'],'cht': ['zh','en','ara','est','bul','pl','dan','de','ru','fra','fin','kor','nl','cs','rom','pt','jp','swe','slo','th','wyw','spa','el','hu','it','yue','vie'],'vie': ['zh','en','ara','est','bul','pl','dan','de','ru','fra','fin','kor','nl','cs','rom','pt','jp','swe','slo','th','wyw','spa','el','hu','it','yue','cht']    }")

private fun baiduLangCode(langCode: String) = langCodeMap[langCode] ?: langCode

@Suppress("UNCHECKED_CAST")
private inline fun <T> Json.value(key: String) = this[key] as? T

private fun sourceUrl(q: Query) = "$BASE_URL/#${baiduLangCode(q.getFrom())}/${baiduLangCode(q.getTo())}/${q.getText()}"