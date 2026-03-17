package com.github.ryanxie0827.omnifeedx.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

// 声明持久化文件名为 OmniFeedXSettings.xml
@State(
    name = "com.github.ryanxie0827.omnifeedx.settings.OmniFeedSettingsState",
    storages = [Storage("OmniFeedXSettings.xml")]
)
class OmniFeedSettingsState : PersistentStateComponent<OmniFeedSettingsState> {

    // 这些变量会被自动持久化
    var apiKey: String = ""
    var apiUrl: String = "https://api.openai.com/v1/chat/completions"
    var modelName: String = "gpt-3.5-turbo"

    // 默认站点列表
    var sites: MutableList<FeedSite> = mutableListOf(
        FeedSite("HackerNews", "https://news.ycombinator.com/", "科技"),
        FeedSite("Amazon Best Sellers", "https://www.amazon.com/Best-Sellers/zgbs", "电商")
    )

    override fun getState(): OmniFeedSettingsState = this

    override fun loadState(state: OmniFeedSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: OmniFeedSettingsState
            get() = ApplicationManager.getApplication().getService(OmniFeedSettingsState::class.java)
    }
}

data class FeedSite(var name: String = "", var url: String = "", var category: String = "")