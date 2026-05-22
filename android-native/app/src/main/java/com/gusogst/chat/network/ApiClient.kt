package com.gusogst.chat.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val logging = HttpLoggingInterceptor().apply {
        // [Fix-6a] 改为HEADERS级别，避免BODY级别将Authorization/API Key明文泄露到logcat
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    private var currentBaseUrl: String = ""
    private var currentRetrofit: Retrofit? = null

    @Synchronized
    fun getService(baseUrl: String): ApiService {
        val normalized = baseUrl.trimEnd('/') + "/"
        if (normalized != currentBaseUrl) {
            currentBaseUrl = normalized
            currentRetrofit = Retrofit.Builder()
                .baseUrl(normalized)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return currentRetrofit!!.create(ApiService::class.java)
    }
}
