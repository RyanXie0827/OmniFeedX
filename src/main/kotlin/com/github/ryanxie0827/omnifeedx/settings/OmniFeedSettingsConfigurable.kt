package com.github.ryanxie0827.omnifeedx.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Messages
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class OmniFeedSettingsConfigurable : Configurable {
    private var myMainPanel: JPanel? = null
    private val apiKeyField = JBTextField()
    private val apiUrlField = JBTextField()

    // 使用 IntelliJ 的 CollectionListModel 来管理动态列表
    private val listModel = CollectionListModel<FeedSite>()
    private val siteList = JBList(listModel)

    override fun getDisplayName(): String = "OmniFeedX AI 设置"

    override fun createComponent(): JComponent {
        // 设置列表展示样式
        siteList.installCellRenderer { site ->
            com.intellij.ui.components.JBLabel("[${site.category}] ${site.name} - ${site.url}")
        }

        // 创建带 + 和 - 按钮的面板
        val decorator = ToolbarDecorator.createDecorator(siteList)
            .setAddAction {
                val name = Messages.showInputDialog("输入站点名称:", "新增站点", null) ?: return@setAddAction
                val url = Messages.showInputDialog("输入站点 URL:", "新增站点", null) ?: return@setAddAction
                val category = Messages.showInputDialog("输入分类:", "新增站点", null) ?: "默认"
                listModel.add(FeedSite(name, url, category))
            }
            .setRemoveAction {
                val index = siteList.selectedIndex
                if (index != -1) listModel.remove(index)
            }
            .disableUpDownActions()

        val listPanel = JPanel(BorderLayout())
        listPanel.add(decorator.createPanel(), BorderLayout.CENTER)

        myMainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("API Key: ", apiKeyField)
            .addLabeledComponent("API URL: ", apiUrlField)
            .addSeparator()
            .addLabeledComponent("已配置站点列表: ", listPanel, 10, true)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return myMainPanel!!
    }

    override fun isModified(): Boolean {
        val settings = OmniFeedSettingsState.instance
        return apiKeyField.text != settings.apiKey ||
                apiUrlField.text != settings.apiUrl ||
                listModel.items != settings.sites
    }

    override fun apply() {
        val settings = OmniFeedSettingsState.instance
        settings.apiKey = apiKeyField.text
        settings.apiUrl = apiUrlField.text
        // 将 UI 列表的数据同步到持久化内存
        settings.sites = listModel.items.toMutableList()
    }

    override fun reset() {
        val settings = OmniFeedSettingsState.instance
        apiKeyField.text = settings.apiKey
        apiUrlField.text = settings.apiUrl
        // 将持久化数据还原到 UI 列表
        listModel.removeAll()
        settings.sites.forEach { listModel.add(it.copy()) }
    }

    override fun disposeUIResources() {
        myMainPanel = null
    }
}