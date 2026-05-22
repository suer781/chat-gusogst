package com.gusogst.chat.network

import com.gusogst.chat.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("/v1/chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") auth: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>

    @POST("/v1/chat/completions")
    @Streaming
    suspend fun chatCompletionsStream(
        @Header("Authorization") auth: String,
        @Body request: ChatRequest
    ): Response<ResponseBody>

    @GET("/v1/models")
    suspend fun listModels(
        @Header("Authorization") auth: String
    ): Response<ResponseBody>
}
