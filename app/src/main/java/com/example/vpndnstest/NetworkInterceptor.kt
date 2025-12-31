package com.example.vpndnstest

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Network Interceptor untuk monitor dan log HTTP requests
 * Berguna untuk verify DNS resolution dan track network calls
 */
class NetworkInterceptor : Interceptor {

    companion object {
        private const val TAG = "NetworkInterceptor"
        
        // Statistics
        private var totalRequests = 0
        private var successfulRequests = 0
        private var failedRequests = 0
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        totalRequests++

        // Log request details
        val url = request.url
        val method = request.method
        val host = url.host
        
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "→ Request #$totalRequests")
        Log.d(TAG, "  Method: $method")
        Log.d(TAG, "  Host: $host")
        Log.d(TAG, "  URL: $url")
        
        // Track request start time
        val startTime = System.currentTimeMillis()

        return try {
            // Proceed dengan request
            val response = chain.proceed(request)
            
            // Calculate duration
            val duration = System.currentTimeMillis() - startTime
            
            // Log response
            val code = response.code
            val message = response.message
            val success = response.isSuccessful
            
            if (success) {
                successfulRequests++
            } else {
                failedRequests++
            }
            
            Log.d(TAG, "← Response:")
            Log.d(TAG, "  Code: $code $message")
            Log.d(TAG, "  Success: $success")
            Log.d(TAG, "  Duration: ${duration}ms")
            Log.d(TAG, "  Content-Type: ${response.header("Content-Type")}")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            
            response
            
        } catch (e: Exception) {
            failedRequests++
            
            Log.e(TAG, "✗ Request FAILED:")
            Log.e(TAG, "  Host: $host")
            Log.e(TAG, "  Error: ${e.message}")
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            
            throw e
        }
    }

    /**
     * Get network statistics
     */
    fun getStats(): String {
        val successRate = if (totalRequests > 0) {
            (successfulRequests.toFloat() / totalRequests * 100).toInt()
        } else 0
        
        return """
            Network Statistics:
            Total Requests: $totalRequests
            Successful: $successfulRequests
            Failed: $failedRequests
            Success Rate: $successRate%
        """.trimIndent()
    }

    /**
     * Reset statistics
     */
    fun resetStats() {
        totalRequests = 0
        successfulRequests = 0
        failedRequests = 0
        Log.d(TAG, "Statistics reset")
    }
}
