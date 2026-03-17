package com.github.ryanxie0827.omnifeedx.api

import com.github.ryanxie0827.omnifeedx.settings.OmniFeedSettingsState
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object AIService {
    // 设置 60 秒超时，解决网络波动导致的超时问题
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val chatHistory = mutableListOf<Pair<String, String>>()

    fun clearHistory() { chatHistory.clear() }
    fun appendAssistantResponse(content: String) { chatHistory.add(Pair("assistant", content)) }

    fun chatStream(
        userInput: String,
        isPageContext: Boolean = false,
        onStart: () -> Unit,
        onToken: (String) -> Unit,
        onError: (String) -> Unit
    ): Call {
        val settings = OmniFeedSettingsState.instance
        val contentToSend = if (isPageContext) "请分析以下内容：\n\n${userInput.take(8000)}" else userInput
        chatHistory.add(Pair("user", contentToSend))

        val messagesArray = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("role", "system")
                addProperty("content", "你是一个资深分析师，请用 Markdown 格式精炼回答。")
            })
            chatHistory.forEach { (role, content) ->
                add(JsonObject().apply {
                    addProperty("role", role)
                    addProperty("content", content)
                })
            }
        }

        val jsonBody = JsonObject().apply {
            addProperty("model", settings.modelName)
            add("messages", messagesArray)
            addProperty("stream", true)
        }

        val request = Request.Builder()
            .url(settings.apiUrl)
            .addHeader("Authorization", "Bearer ${settings.apiKey}")
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!call.isCanceled()) onError("请求失败或超时: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    onError("API 错误: ${response.code}\n请确认 Key 和路径是否正确。")
                    return
                }
                onStart()
                val source = response.body?.source() ?: return
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val data = line.substring(6).trim()
                        if (data == "[DONE]") break
                        try {
                            val jsonObject = gson.fromJson(data, JsonObject::class.java)
                            val token = jsonObject.getAsJsonArray("choices")[0].asJsonObject
                                .getAsJsonObject("delta").get("content").asString
                            onToken(token)
                        } catch (e: Exception) {}
                    }
                }
            }
        })
        return call
    }
}