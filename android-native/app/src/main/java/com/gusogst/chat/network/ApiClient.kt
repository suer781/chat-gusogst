package com.gusogst.chat.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Retrofit 客户端管理器。
 *
 * 缓存逻辑：
 *   用 ConcurrentHashMap 缓存 baseUrl → Retrofit 的映射，
 *   切换供应商时直接命中缓存，无需重建 OkHttpClient 和 Retrofit 实例。
 *
 *   旧的单槽实现只记住最后一个 baseUrl，切换就重建，浪费。
 */
object ApiClient {
    private val logging = HttpLoggingInterceptor().apply {
        // [Fix-6a] HEADERS级别，避免BODY将Authorization/API Key明文泄露到logcat
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    private const val TIMEOUT_CONNECT_SEC = 30L
    private const val TIMEOUT_READ_SEC = 120L
    private const val TIMEOUT_WRITE_SEC = 30L

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_CONNECT_SEC, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_READ_SEC, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_WRITE_SEC, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    /** baseUrl → Retrofit 实例缓存（线程安全） */
    private val retrofitCache = ConcurrentHashMap<String, Retrofit>()

    /**
     * 获取 [baseUrl] 对应的 ApiService 实例。
     * 缓存命中直接返回，未命中则创建并缓存。
     */
    fun getService(baseUrl: String): ApiService {
        val normalized = baseUrl.trimEnd('/') + "/"
        return retrofitCache.getOrPut(normalized) {
            Retrofit.Builder()
                .baseUrl(normalized)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }.create(ApiService::class.java)
    }

    /** 清理所有缓存（切换账号等场景） */
    fun clearCache() {
        retrofitCache.clear()
    }
}
