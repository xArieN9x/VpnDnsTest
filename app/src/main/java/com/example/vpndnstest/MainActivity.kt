package com.example.vpndnstest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val VPN_REQUEST_CODE = 100
    }

    private lateinit var btnStartVpn: Button
    private lateinit var indicatorStatus: View
    private lateinit var txtStatus: TextView

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra("status") ?: return
            val dns = intent.getStringExtra("dns") ?: ""
            
            updateUI(status == "RUNNING", dns)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        btnStartVpn = findViewById(R.id.btnStartVpn)
        indicatorStatus = findViewById(R.id.indicatorStatus)
        txtStatus = findViewById(R.id.txtStatus)

        // Button click handler
        btnStartVpn.setOnClickListener {
            if (VpnDnsService.isVpnRunning()) {
                stopVpnService()
            } else {
                requestVpnPermission()
            }
        }

        // Register broadcast receiver
        val filter = IntentFilter("VPN_DNS_STATUS")
        registerReceiver(statusReceiver, filter, RECEIVER_NOT_EXPORTED)

        // Update UI berdasarkan status
        updateUI(VpnDnsService.isVpnRunning(), "156.154.70.1")
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            // Need permission - show system dialog
            Log.d(TAG, "Requesting VPN permission")
            startActivityForResult(intent, VPN_REQUEST_CODE)
        } else {
            // Already have permission
            Log.d(TAG, "VPN permission already granted")
            startVpnService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "VPN permission granted")
                startVpnService()
            } else {
                Log.d(TAG, "VPN permission denied")
                updateUI(false, "")
            }
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, VpnDnsService::class.java).apply {
            action = VpnDnsService.ACTION_START_VPN
        }
        ContextCompat.startForegroundService(this, intent)
        Log.d(TAG, "VPN service start command sent")
    }

    private fun stopVpnService() {
        val intent = Intent(this, VpnDnsService::class.java).apply {
            action = VpnDnsService.ACTION_STOP_VPN
        }
        startService(intent)
        Log.d(TAG, "VPN service stop command sent")
    }

    private fun updateUI(isRunning: Boolean, dns: String) {
        runOnUiThread {
            if (isRunning) {
                // VPN ON - Green indicator
                indicatorStatus.setBackgroundColor(getColor(android.R.color.holo_green_light))
                txtStatus.text = "VPN: ON | DNS: $dns"
                btnStartVpn.text = "Stop VPN"
                btnStartVpn.setBackgroundColor(getColor(android.R.color.holo_red_light))
            } else {
                // VPN OFF - Red indicator
                indicatorStatus.setBackgroundColor(getColor(android.R.color.holo_red_light))
                txtStatus.text = "VPN: OFF | DNS: -"
                btnStartVpn.text = "Start VPN DNS"
                btnStartVpn.setBackgroundColor(getColor(android.R.color.holo_green_light))
            }
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(statusReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
        super.onDestroy()
    }
}
