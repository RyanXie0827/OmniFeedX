package com.github.ryanxie0827.omnifeedx.ui

import com.github.ryanxie0827.omnifeedx.api.AIService
import com.github.ryanxie0827.omnifeedx.settings.OmniFeedSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import okhttp3.Call
import org.cef.callback.CefStringVisitor
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*

class OmniFeedToolWindowFactory : ToolWindowFactory {

    // 声明为类成员变量，确保 sendChatRequest 可以访问
    private var jbCefBrowser: JBCefBrowser? = null
    private var currentAiCall: Call? = null

    private val mdParser = Parser.builder().build()
    private val htmlRenderer = HtmlRenderer.builder().build()

    private var fullChatMarkdown = "👋 **OmniFeedX 已就绪**\n\n请点击上方【⚙️ 配置】后开始。"
    private var currentStreamResponse = ""
    private lateinit var chatDisplayPane: JEditorPane

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val settings = OmniFeedSettingsState.instance
        val mainPanel = JPanel(BorderLayout())

        // 1. 工具栏 (包含配置按钮)
        val actionToolbar = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            val configBtn = JButton("⚙️ 配置 AI")
            val anaBtn = JButton("🤖 分析网页")
            val clearBtn = JButton("🗑️ 清空")

            configBtn.addActionListener {
                if (AISettingsDialog().showAndGet()) {
                    fullChatMarkdown = "✅ **配置已保存并自动持久化**"
                    updateChatUI()
                }
            }

            anaBtn.addActionListener {
                jbCefBrowser?.cefBrowser?.getText(object : CefStringVisitor {
                    override fun visit(text: String?) {
                        text?.let { sendChatRequest("分析当前网页内容", true, it) }
                    }
                })
            }

            clearBtn.addActionListener {
                AIService.clearHistory()
                fullChatMarkdown = "🧹 对话已清空"
                currentStreamResponse = ""
                updateChatUI()
            }

            add(configBtn); add(anaBtn); add(clearBtn)
        }

        // 2. 站点列表
        val listModel = DefaultListModel<String>()
        settings.sites.forEach { listModel.addElement("🏷️ [${it.category}] ${it.name}") }
        val feedList = JBList(listModel)

        // 3. 浏览器初始化
        val browserPanel = JPanel(BorderLayout())
        if (JBCefApp.isSupported()) {
            jbCefBrowser = JBCefBrowser(settings.sites.firstOrNull()?.url ?: "https://www.google.com")
            browserPanel.add(jbCefBrowser!!.component, BorderLayout.CENTER)
        }

        // 4. 聊天区
        chatDisplayPane = JEditorPane().apply {
            contentType = "text/html"
            isEditable = false
            text = renderMarkdownToHtml(fullChatMarkdown)
        }

        // 5. 聊天输入
        val inputField = JBTextField().apply { emptyText.text = "按 Enter 发送..." }
        inputField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    val txt = inputField.text.trim()
                    if (txt.isNotEmpty()) {
                        inputField.text = ""
                        sendChatRequest(txt, false, txt)
                    }
                }
            }
        })

        // 6. 拼装布局
        val chatPanel = JPanel(BorderLayout()).apply {
            add(JBScrollPane(chatDisplayPane), BorderLayout.CENTER)
            add(inputField, BorderLayout.SOUTH)
        }

        val topSplit = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, JBScrollPane(feedList), browserPanel).apply { dividerLocation = 180 }
        val finalSplit = JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, chatPanel).apply { dividerLocation = 400 }

        mainPanel.add(actionToolbar, BorderLayout.NORTH)
        mainPanel.add(finalSplit, BorderLayout.CENTER)

        toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(mainPanel, "发现", false))

        feedList.addListSelectionListener {
            if (!it.valueIsAdjusting && feedList.selectedIndex != -1) {
                jbCefBrowser?.loadURL(settings.sites[feedList.selectedIndex].url)
            }
        }
    }

    private fun sendChatRequest(display: String, isPage: Boolean, raw: String) {
        if (currentStreamResponse.isNotEmpty()) {
            fullChatMarkdown += currentStreamResponse
            AIService.appendAssistantResponse(currentStreamResponse)
            currentStreamResponse = ""
        }
        fullChatMarkdown += "\n\n---\n**🧑 你：** $display\n\n**🤖 AI：** "
        updateChatUI()

        currentAiCall = AIService.chatStream(raw, isPage, {}, { token ->
            currentStreamResponse += token
            ApplicationManager.getApplication().invokeLater { updateChatUI() }
        }, { err ->
            ApplicationManager.getApplication().invokeLater {
                fullChatMarkdown += "\n\n❌ **失败:** $err"
                updateChatUI()
            }
        })
    }

    private fun updateChatUI() {
        chatDisplayPane.text = renderMarkdownToHtml(fullChatMarkdown + currentStreamResponse)
        chatDisplayPane.caretPosition = chatDisplayPane.document.length
    }

    private fun renderMarkdownToHtml(md: String): String {
        val html = htmlRenderer.render(mdParser.parse(md))
        return "<html><body style='font-family:sans-serif;font-size:12px;padding:10px;'>$html</body></html>"
    }
}