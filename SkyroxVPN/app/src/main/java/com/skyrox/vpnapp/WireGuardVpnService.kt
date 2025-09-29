package com.skyrox.vpnapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch



//package com.example.vpnapp

//import android.app.*
//import android.content.Context
//import android.content.Intent
//import android.net.ConnectivityManager
//import android.net.NetworkCapabilities
//import android.net.VpnService
//import android.os.Build
//import android.os.IBinder
//import android.util.Log
//import androidx.core.app.NotificationCompat
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch




import kotlinx.coroutines.*




class WireGuardVpnService : VpnService() {

    private var isVpnRunning = false
    private var wireGuardManager: WireGuardManager? = null

    override fun onCreate() {
        super.onCreate()
        wireGuardManager = WireGuardManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(1, createNotification())
        }

        CoroutineScope(Dispatchers.IO).launch {
            delay(1000) // Даем системе время обновить статус сети
            if (!isVpnActive() && !isVpnRunning) {
                Log.d("WireGuardVpnService", "Запускаем VPN...")
                startVpn()
            } else {
                Log.d("WireGuardVpnService", "VPN уже работает, повторный запуск не требуется")
            }
        }

        return START_STICKY
    }



    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("WireGuardVpnService", "Приложение закрыто, отключаем VPN")

        CoroutineScope(Dispatchers.IO).launch {
            if (wireGuardManager?.hasActiveTunnel() == true) {
                Log.d("WireGuardVpnService", "Отключаем VPN перед остановкой сервиса")
                wireGuardManager?.disconnectTunnel()
                isVpnRunning = false
            }
            stopSelf()
            Log.d("WireGuardVpnService", "VPN выключен и сервис остановлен")
        }
    }




    override fun onDestroy() {
        super.onDestroy()
        Log.d("WireGuardVpnService", "Сервис уничтожен, отключаем VPN")
        stopVpn()
    }

    private fun startVpn() {
        CoroutineScope(Dispatchers.IO).launch {
            wireGuardManager?.let {
                val success = it.connectTunnel("ваш WireGuard конфиг")
                isVpnRunning = success
                if (!success) {
                    Log.e("WireGuardVpnService", "Ошибка подключения к VPN")
                }
            }
        }
    }

    private fun stopVpn() {
        CoroutineScope(Dispatchers.IO).launch {
            if (wireGuardManager?.hasActiveTunnel() == true) {
                wireGuardManager?.disconnectTunnel()
                isVpnRunning = false
                Log.d("WireGuardVpnService", "VPN выключен")
            }
            stopForeground(true)
            stopSelf()
            Log.d("WireGuardVpnService", "Сервис остановлен")
        }
    }


    private fun isVpnActive(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    private fun createNotification(): Notification {
        val channelId = "vpn_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("VPN работает")
            .setContentText("Подключение активно")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .build()
    }
}






//_______________________________________________________


//class WireGuardVpnService : VpnService() {
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        return START_STICKY
//    }
//
//    override fun onBind(intent: Intent?): IBinder? {
//        return super.onBind(intent)
//    }
//
//    override fun onRevoke() {
//        stopVpn()
//    }
//
//    override fun onTaskRemoved(rootIntent: Intent?) {
//        Log.d("WireGuardVpnService", "Приложение закрыто, отключаем VPN")
//        stopVpn()
//        stopSelf()
//    }
//
//    private fun stopVpn() {
//        val wireGuardManager = WireGuardManager(this)
//        CoroutineScope(Dispatchers.IO).launch {
//            wireGuardManager.disconnectTunnel()
//        }
//    }
//}
