package com.github.ryanxie0827.omnifeedx.ui

import com.github.ryanxie0827.omnifeedx.settings.OmniFeedSettingsState
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent

class AISettingsDialog : DialogWrapper(true) {
    private val state = OmniFeedSettingsState.instance

    private val apiKeyField = JBTextField(state.apiKey)
    private val apiUrlField = JBTextField(state.apiUrl)
    private val modelNameField = JBTextField(state.modelName)

    init {
        title = "OmniFeedX AI 配置"
        init()
    }

    override fun createCenterPanel(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("API Key:", apiKeyField)
            .addLabeledComponent("API URL:", apiUrlField)
            .addLabeledComponent("模型名称:", modelNameField)
            .panel
    }

    override fun doOKAction() {
        // 持久化保存
        state.apiKey = apiKeyField.text.trim()
        state.apiUrl = apiUrlField.text.trim()
        state.modelName = modelNameField.text.trim()
        super.doOKAction()
    }
}