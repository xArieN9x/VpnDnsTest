package com.example.vpndnstest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Simple VPN Service untuk test DNS change
 * Single DNS: 156.154.70.1
 */
class VpnDnsService : VpnService() {

    companion object {
        private const val TAG = "VpnDnsService"
        private const val NOTIFICATION_ID = 999
        private const val CHANNEL_ID = "vpn_dns_channel"
        
        // Single DNS untuk test
        private const val DNS_SERVER = "156.154.70.1"
        
        // Actions
        const val ACTION_START_VPN = "com.example.vpndnstest.START_VPN"
        const val ACTION_STOP_VPN = "com.example.vpndnstest.STOP_VPN"
        
        // Status
        private val isRunning = AtomicBoolean(false)
        
        fun isVpnRunning(): Boolean = isRunning.get()
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var notificationManager: NotificationManager? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_VPN -> {
                Log.d(TAG, "Starting VPN with DNS: $DNS_SERVER")
                startVpn()
            }
            ACTION_STOP_VPN -> {
                Log.d(TAG, "Stopping VPN")
                stopVpn()
            }
            else -> stopSelf()
        }

        return START_STICKY
    }

    private fun startVpn() {
        try {
            // Close existing jika ada
            vpnInterface?.close()

            // Build VPN config
            val builder = Builder()
                .setSession("VPN DNS Test")
                .setMtu(1500)
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer(DNS_SERVER)
                .setBlocking(false)

            // Establish VPN
            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                isRunning.set(true)
                showNotification()
                broadcastStatus(true)
                Log.d(TAG, "VPN established successfully")
            } else {
                Log.e(TAG, "Failed to establish VPN")
                stopSelf()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN: ${e.message}")
            stopSelf()
        }
    }

    private fun stopVpn() {
        try {
            isRunning.set(false)
            
            vpnInterface?.close()
            vpnInterface = null
            
            notificationManager?.cancel(NOTIFICATION_ID)
            broadcastStatus(false)
            
            Log.d(TAG, "VPN stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping VPN: ${e.message}")
        } finally {
            stopForeground(true)
            stopSelf()
        }
    }

    private fun showNotification() {
        notificationManager = getSystemService(NotificationManager::class.java)

        // Create channel untuk Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN DNS Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "VPN DNS Test Service"
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }

        // Intent untuk buka app
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VPN DNS Active")
            .setContentText("DNS: $DNS_SERVER")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "Foreground notification shown")
    }

    private fun broadcastStatus(running: Boolean) {
        sendBroadcast(Intent("VPN_DNS_STATUS").apply {
            putExtra("status", if (running) "RUNNING" else "STOPPED")
            putExtra("dns", if (running) DNS_SERVER else "")
        })
    }

    override fun onRevoke() {
        Log.d(TAG, "VPN revoked by system/user")
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroying")
        stopVpn()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
