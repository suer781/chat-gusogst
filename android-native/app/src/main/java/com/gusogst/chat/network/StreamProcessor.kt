package com.gusogst.chat.network

import com.google.gson.Gson
import com.gusogst.chat.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import java.io.BufferedReader
import java.io.InputStreamReader

class StreamProcessor {
    private val gson = Gson()

    fun processStream(
        responseBody: ResponseBody,
        onThinking: (String) -> Unit,
        onContent: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: continue
                if (l.startsWith("data: ")) {
                    val data = l.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    try {
                        val json = com.google.gson.JsonParser.parseString(data).asJsonObject
                        // OpenAI 格式: choices[0].delta.content
                        val choices = json.getAsJsonArray("choices")
                        if (choices != null && choices.size() > 0) {
                            val delta = choices[0].asJsonObject.getAsJsonObject("delta")
                            delta?.get("reasoning_content")?.asString?.let { if (it.isNotEmpty()) onThinking(it) }
                            delta?.get("content")?.asString?.let { if (it.isNotEmpty()) onContent(it) }
                        }
                        // Ollama 格式: message.content
                        val message = json.getAsJsonObject("message")
                        if (message != null) {
                            message.get("content")?.asString?.let { if (it.isNotEmpty()) onContent(it) }
                        }
                        // 简单格式: content 字段
                        if (choices == null && message == null) {
                            json.get("content")?.asString?.let { if (it.isNotEmpty()) onContent(it) }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("StreamProcessor", "SSE parse error", e)
                    }
                }
            }
            onComplete()
        } catch (e: Exception) {
            onError(e.message ?: "Stream error")
        } finally {
            responseBody.close()
        }
    }
}
