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
                        // [Fix-7] 打印SSE解析错误日志，方便排查模型返回格式不兼容等问题
                        android.util.Log.w("StreamProcessor", "Failed to parse SSE chunk: $data", e)
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
