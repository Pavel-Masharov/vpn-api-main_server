package com.skyrox.vpnapp

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.core.content.ContextCompat
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.StringReader

class WireGuardManager(private val context: Context) {

    companion object {
        private var backend: GoBackend? = null
        private var tunnel: Tunnel? = null
    }

    suspend fun connectTunnel(configString: String): Boolean {
        Log.d("WireGuardManager", "Запуск VPN с конфигом: ${configString.take(100)}...")

        val intent = VpnService.prepare(context.applicationContext)
        if (intent != null) {
            Log.d("WireGuardManager", "Запрашиваем разрешение на VPN")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                Log.d("WireGuardManager", "Разрешение уже есть, запускаем VPN сервис")
                startVpnService()

                if (backend == null) {
                    backend = GoBackend(context.applicationContext)
                }

                if (tunnel == null) {
                    tunnel = object : Tunnel {
                        override fun getName() = "vpn_tunnel"
                        override fun onStateChange(state: Tunnel.State) {
                            Log.d("WireGuardManager", "Состояние туннеля: $state")
                        }
                    }
                }

                val config = Config.parse(BufferedReader(StringReader(configString)))
                backend!!.setState(tunnel!!, Tunnel.State.UP, config)
                Log.d("WireGuardManager", "VPN туннель успешно запущен!")
                true
            } catch (e: Exception) {
                Log.e("WireGuardManager", "Ошибка подключения к WireGuard", e)
                false
            }
        }
    }

    suspend fun disconnectTunnel() {
        withContext(Dispatchers.IO) {
            if (backend == null || tunnel == null) {
                Log.w("WireGuardManager", "disconnectTunnel вызван, но Backend или Tunnel уже null")
                return@withContext
            }

            try {
                backend!!.setState(tunnel!!, Tunnel.State.DOWN, null)
                Log.d("WireGuardManager", "VPN туннель отключен!")
            } catch (e: Exception) {
                Log.e("WireGuardManager", "Ошибка отключения WireGuard", e)
            } finally {
                backend = null
                tunnel = null
            }
        }
    }

    fun startVpnService() {
        val serviceIntent = Intent(context.applicationContext, WireGuardVpnService::class.java)
        ContextCompat.startForegroundService(context.applicationContext, serviceIntent)
        Log.d("WireGuardManager", "VPN сервис запущен")
    }

    fun hasActiveTunnel(): Boolean {
        return backend != null && tunnel != null
    }
}




//class WireGuardManager(private val context: Context) {
//    private var backend: GoBackend? = null
//    private var tunnel: Tunnel? = null
//
//
//    suspend fun connectTunnel(configString: String): Boolean {
//        Log.d("WireGuardManager", "Запуск VPN с конфигом: ${configString.take(100)}...")
//
//        val intent = VpnService.prepare(context.applicationContext)
//        if (intent != null) {
//            Log.d("WireGuardManager", "Запрашиваем разрешение на VPN")
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//            context.startActivity(intent)
//            return false // VPN не запущен, ждем разрешения
//        }
//
//        return withContext(Dispatchers.IO) {
//            try {
//                Log.d("WireGuardManager", "Разрешение уже есть, запускаем VPN сервис")
//                startVpnService()
//
//                backend = GoBackend(context.applicationContext)
//                tunnel = object : Tunnel {
//                    override fun getName() = "vpn_tunnel"
//                    override fun onStateChange(state: Tunnel.State) {
//                        Log.d("WireGuardManager", "Состояние туннеля: $state")
//                    }
//                }
//
//                val config = Config.parse(BufferedReader(StringReader(configString)))
//                if (backend != null && tunnel != null) {
//                    backend!!.setState(tunnel!!, Tunnel.State.UP, config)
//                    Log.d("WireGuardManager", "VPN туннель успешно запущен!")
//                    true // Успешное подключение
//                } else {
//                    Log.e("WireGuardManager", "Ошибка: Backend или Tunnel не инициализированы!")
//                    false // Ошибка
//                }
//            } catch (e: Exception) {
//                Log.e("WireGuardManager", "Ошибка подключения к WireGuard", e)
//                false // Ошибка
//            }
//        }
//    }
//
//    suspend fun disconnectTunnel() {
//        withContext(Dispatchers.IO) {
//            if (backend == null || tunnel == null) {
//                Log.w("WireGuardManager", "disconnectTunnel вызван, но Backend или Tunnel уже null")
//                return@withContext
//            }
//
//            try {
//                backend!!.setState(tunnel!!, Tunnel.State.DOWN, null)
//                Log.d("WireGuardManager", "VPN туннель отключен!")
//            } catch (e: Exception) {
//                Log.e("WireGuardManager", "Ошибка отключения WireGuard", e)
//            } finally {
//                backend = null
//                tunnel = null
//            }
//        }
//    }
//
//
//
//    fun startVpnService() {
//        val serviceIntent = Intent(context.applicationContext, WireGuardVpnService::class.java)
//        ContextCompat.startForegroundService(context.applicationContext, serviceIntent)
//        Log.d("WireGuardManager", "VPN сервис запущен")
//    }
//
//
//    fun hasActiveTunnel(): Boolean {
//        return backend != null && tunnel != null
//    }
//
//}


//_________________________________________

//class WireGuardManager(private val context: Context) {
//    private var backend: GoBackend? = null
//    private var tunnel: Tunnel? = null
//    private var webSocketProxy: WebSocketProxy? = null
//
//    suspend fun connectTunnel(configString: String) {
//        Log.d("WireGuardManager", "Запуск WebSocket-прокси...")
//
//        webSocketProxy = WebSocketProxy("wss://38.180.178.58:443/mysecret")
//        webSocketProxy?.startProxy {
//            Log.d("WireGuardManager", "✅ WebSocket подключен! Запускаем VPN...")
//
//            CoroutineScope(Dispatchers.IO).launch {
//                startWireGuard(configString)
//            }
//        }
//    }
//
//    private suspend fun startWireGuard(configString: String) {
//        Log.d("WireGuardManager", "Запуск VPN через WebSocket...")
//
//        val intent = VpnService.prepare(context.applicationContext)
//        if (intent != null) {
//            Log.d("WireGuardManager", "⚠️ Запрашиваем разрешение на VPN")
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//            context.startActivity(intent)
//            return
//        }
//
//        withContext(Dispatchers.IO) {
//            try {
//                startVpnService()
//
//                backend = GoBackend(context.applicationContext)
//                tunnel = object : Tunnel {
//                    override fun getName() = "vpn_tunnel"
//                    override fun onStateChange(state: Tunnel.State) {
//                        Log.d("WireGuardManager", "Состояние туннеля: $state")
//                    }
//                }
//
//                    //val fixedConfig = configString.replace("Endpoint = 94.140.143.193:22720", "Endpoint = 127.0.0.1:443")
//                val config = Config.parse(BufferedReader(StringReader(configString)))
//                backend?.setState(tunnel!!, Tunnel.State.UP, config)
//                Log.d("WireGuardManager", "✅ VPN через WebSocket работает!")
//            } catch (e: Exception) {
//                Log.e("WireGuardManager", "❌ Ошибка подключения WireGuard", e)
//            }
//        }
//    }
//
//    suspend fun disconnectTunnel() {
//        Log.d("WireGuardManager", "Отключаем VPN и WebSocket...")
//
//        withContext(Dispatchers.IO) {
//            try {
//                tunnel?.let { backend?.setState(it, Tunnel.State.DOWN, null) }
//                webSocketProxy?.stopProxy()
//                Log.d("WireGuardManager", "❌ VPN и WebSocket отключены")
//            } catch (e: Exception) {
//                Log.e("WireGuardManager", "Ошибка отключения WireGuard", e)
//            }
//        }
//    }
//
//    private fun startVpnService() {
//        val serviceIntent = Intent(context.applicationContext, WireGuardVpnService::class.java)
//        ContextCompat.startForegroundService(context.applicationContext, serviceIntent)
//        Log.d("WireGuardManager", "VPN сервис запущен")
//    }
//}
