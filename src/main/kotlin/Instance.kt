import org.dicthub.plugin.com_baidu_fanyi.BaiduTranslationProvider
import org.dicthub.plugin.com_baidu_fanyi.BaiduTranslationRenderer
import org.dicthub.plugin.shared.util.AjaxHttpClient

@JsName("create_plugin_com_baidu_fanyi")
fun create_plugin_com_baidu_fanyi(): BaiduTranslationProvider {

    return BaiduTranslationProvider(AjaxHttpClient, BaiduTranslationRenderer())
}
