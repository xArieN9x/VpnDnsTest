package com.example.vpndnstest

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Helper object untuk create OkHttpClient dengan custom DNS
 * Singleton pattern untuk reuse client
 */
object OkHttpClientHelper {

    private const val TAG = "OkHttpClientHelper"
    
    // Custom DNS resolver instance
    private val customDnsResolver = CustomDnsResolver("156.154.70.1")
    
    // Network interceptor instance
    private val networkInterceptor = NetworkInterceptor()

    /**
     * Get OkHttpClient dengan custom DNS resolver
     */
    fun getClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .dns(customDnsResolver)                    // Custom DNS!
            .addNetworkInterceptor(networkInterceptor)  // Logging
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Get DNS resolver instance (untuk testing)
     */
    fun getDnsResolver(): CustomDnsResolver = customDnsResolver

    /**
     * Get network interceptor (untuk stats)
     */
    fun getInterceptor(): NetworkInterceptor = networkInterceptor

    /**
     * Test DNS connection
     */
    fun testDnsConnection(): Boolean {
        return customDnsResolver.testDnsServer()
    }

    /**
     * Get combined statistics
     */
    fun getStats(): String {
        return """
            ${customDnsResolver.getStats()}
            
            ${networkInterceptor.getStats()}
        """.trimIndent()
    }

    /**
     * Clear DNS cache
     */
    fun clearCache() {
        customDnsResolver.clearCache()
    }
}
