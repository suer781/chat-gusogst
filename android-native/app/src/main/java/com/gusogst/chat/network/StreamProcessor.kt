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
                        val chunk = gson.fromJson(data, ChatResponse::class.java)
                        val delta = chunk.choices.firstOrNull()?.delta ?: continue
                        // 思考内容
                        delta.reasoning_content?.let { if (it.isNotEmpty()) onThinking(it) }
                        // 正文内容
                        delta.content?.let { if (it.isNotEmpty()) onContent(it) }
                    } catch (e: Exception) {
                        // 解析失败时不打印 SSE 原始数据，避免聊天内容泄漏到 logcat
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
