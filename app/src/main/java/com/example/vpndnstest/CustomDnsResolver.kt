package com.example.vpndnstest

import android.util.Log
import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Custom DNS Resolver untuk override system DNS
 * Guna OkHttp DNS interface
 * Target DNS: 156.154.70.1 (Neustar)
 */
class CustomDnsResolver(
    private val customDnsServer: String = "156.154.70.1"
) : Dns {

    companion object {
        private const val TAG = "CustomDnsResolver"
        
        // Cache untuk speed up repeated queries
        private val dnsCache = mutableMapOf<String, List<InetAddress>>()
        
        // Statistics
        private var totalQueries = 0
        private var cacheHits = 0
        private var cacheMisses = 0
    }

    /**
     * Main DNS lookup function
     * Override dari Dns interface
     */
    override fun lookup(hostname: String): List<InetAddress> {
        totalQueries++
        Log.d(TAG, "DNS Query for: $hostname")

        // Check cache first
        dnsCache[hostname]?.let {
            cacheHits++
            Log.d(TAG, "Cache HIT for $hostname (${it.size} addresses)")
            return it
        }

        cacheMisses++
        Log.d(TAG, "Cache MISS for $hostname - performing lookup via $customDnsServer")

        return try {
            // Perform DNS lookup via custom server
            val addresses = performCustomDnsLookup(hostname)
            
            if (addresses.isNotEmpty()) {
                // Cache hasil
                dnsCache[hostname] = addresses
                
                Log.d(TAG, "Resolved $hostname to ${addresses.size} address(es):")
                addresses.forEach { addr ->
                    Log.d(TAG, "  → ${addr.hostAddress}")
                }
                
                addresses
            } else {
                // Fallback ke system DNS kalau custom gagal
                Log.w(TAG, "Custom DNS failed for $hostname, using system DNS")
                fallbackToSystemDns(hostname)
            }
            
        } catch (e: UnknownHostException) {
            Log.e(TAG, "DNS lookup failed for $hostname: ${e.message}")
            // Last resort: system DNS
            fallbackToSystemDns(hostname)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during DNS lookup: ${e.message}")
            e.printStackTrace()
            fallbackToSystemDns(hostname)
        }
    }

    /**
     * Perform DNS lookup menggunakan custom DNS server
     * Guna SimpleResolver dari dnsjava library
     */
    private fun performCustomDnsLookup(hostname: String): List<InetAddress> {
        return try {
            // Simple approach: Guna InetAddress dengan custom DNS
            // Note: Android default akan guna system DNS,
            // tapi kita force resolve via our custom server
            
            // Build DNS query manually (simplified)
            val resolver = org.xbill.DNS.SimpleResolver(customDnsServer)
            resolver.setTimeout(java.time.Duration.ofSeconds(3))
            
            val lookup = org.xbill.DNS.Lookup(hostname, org.xbill.DNS.Type.A)
            lookup.setResolver(resolver)
            
            val records = lookup.run()
            
            if (records != null && records.isNotEmpty()) {
                records.mapNotNull { record ->
                    if (record is org.xbill.DNS.ARecord) {
                        record.address
                    } else null
                }
            } else {
                emptyList()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Custom DNS query failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Fallback ke system DNS kalau custom DNS gagal
     */
    private fun fallbackToSystemDns(hostname: String): List<InetAddress> {
        return try {
            Log.d(TAG, "Using system DNS for $hostname")
            InetAddress.getAllByName(hostname).toList()
        } catch (e: Exception) {
            Log.e(TAG, "System DNS also failed for $hostname")
            throw UnknownHostException("Unable to resolve $hostname")
        }
    }

    /**
     * Clear DNS cache
     */
    fun clearCache() {
        dnsCache.clear()
        Log.d(TAG, "DNS cache cleared")
    }

    /**
     * Get cache statistics
     */
    fun getStats(): String {
        val hitRate = if (totalQueries > 0) {
            (cacheHits.toFloat() / totalQueries * 100).toInt()
        } else 0
        
        return """
            DNS Statistics:
            Total Queries: $totalQueries
            Cache Hits: $cacheHits
            Cache Misses: $cacheMisses
            Hit Rate: $hitRate%
            Cached Domains: ${dnsCache.size}
        """.trimIndent()
    }

    /**
     * Verify DNS server reachable
     */
    fun testDnsServer(): Boolean {
        return try {
            val resolver = org.xbill.DNS.SimpleResolver(customDnsServer)
            resolver.setTimeout(java.time.Duration.ofSeconds(2))
            
            // Test query to google.com
            val lookup = org.xbill.DNS.Lookup("google.com", org.xbill.DNS.Type.A)
            lookup.setResolver(resolver)
            
            val records = lookup.run()
            val success = records != null && records.isNotEmpty()
            
            Log.d(TAG, "DNS server test: ${if (success) "SUCCESS" else "FAILED"}")
            success
            
        } catch (e: Exception) {
            Log.e(TAG, "DNS server test failed: ${e.message}")
            false
        }
    }
}
