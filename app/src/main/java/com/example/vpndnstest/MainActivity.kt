package com.example.vpndnstest

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var indicatorStatus: View
    private lateinit var txtStatus: TextView
    private lateinit var btnTestDns: Button
    private lateinit var btnTestHttp: Button
    private lateinit var txtLogs: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        indicatorStatus = findViewById(R.id.indicatorStatus)
        txtStatus = findViewById(R.id.txtStatus)
        btnTestDns = findViewById(R.id.btnTestDns)
        btnTestHttp = findViewById(R.id.btnTestHttp)
        txtLogs = findViewById(R.id.txtLogs)

        // Button handlers
        btnTestDns.setOnClickListener {
            testDnsConnection()
        }

        btnTestHttp.setOnClickListener {
            testHttpRequest()
        }

        // Initial UI
        updateUI(false, "Ready to test")
    }

    /**
     * Test DNS server connectivity
     */
    private fun testDnsConnection() {
        updateUI(false, "Testing DNS server...")
        appendLog("Testing DNS: 156.154.70.1...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = OkHttpClientHelper.testDnsConnection()

                withContext(Dispatchers.Main) {
                    if (success) {
                        updateUI(true, "DNS Server: OK")
                        appendLog("✓ DNS server reachable!")
                    } else {
                        updateUI(false, "DNS Server: FAILED")
                        appendLog("✗ DNS server unreachable")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateUI(false, "DNS Test Error")
                    appendLog("✗ Error: ${e.message}")
                }
            }
        }
    }

    /**
     * Test HTTP request dengan custom DNS
     */
    private fun testHttpRequest() {
        updateUI(false, "Testing HTTP request...")
        appendLog("Making HTTP request via custom DNS...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClientHelper.getClient()
                
                // Test request ke Google
                val request = Request.Builder()
                    .url("https://www.google.com")
                    .build()

                val startTime = System.currentTimeMillis()
                val response = client.newCall(request).execute()
                val duration = System.currentTimeMillis() - startTime

                val success = response.isSuccessful
                val code = response.code

                withContext(Dispatchers.Main) {
                    if (success) {
                        updateUI(true, "HTTP: OK (${duration}ms)")
                        appendLog("✓ HTTP request successful!")
                        appendLog("  Code: $code")
                        appendLog("  Duration: ${duration}ms")
                        
                        // Show stats
                        val stats = OkHttpClientHelper.getStats()
                        appendLog("\n$stats")
                    } else {
                        updateUI(false, "HTTP: FAILED")
                        appendLog("✗ HTTP request failed (code: $code)")
                    }
                }

                response.close()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateUI(false, "HTTP Error")
                    appendLog("✗ Error: ${e.message}")
                }
            }
        }
    }

    /**
     * Update UI indicator and status
     */
    private fun updateUI(success: Boolean, status: String) {
        runOnUiThread {
            if (success) {
                indicatorStatus.setBackgroundColor(getColor(android.R.color.holo_green_light))
            } else {
                indicatorStatus.setBackgroundColor(getColor(android.R.color.holo_orange_light))
            }
            txtStatus.text = status
        }
    }

    /**
     * Append log message
     */
    private fun appendLog(message: String) {
        runOnUiThread {
            val current = txtLogs.text.toString()
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())
            
            txtLogs.text = "$current\n[$timestamp] $message"
            
            // Auto scroll to bottom
            txtLogs.post {
                val scrollAmount = txtLogs.layout?.getLineTop(txtLogs.lineCount) ?: 0
                if (scrollAmount > txtLogs.height) {
                    txtLogs.scrollTo(0, scrollAmount - txtLogs.height)
                }
            }
        }
    }
}
