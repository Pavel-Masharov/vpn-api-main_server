package com.skyrox.vpnapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.skyrox.vpnapp.ui.theme.ThemeManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID
import androidx.compose.ui.text.style.TextAlign

class ServerSelectionActivity : ComponentActivity() {
    private val apiService = ApiService.create()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var selectedServer by remember { mutableStateOf<ApiService.VpnServer?>(null) }

            ServerListUI(
                selectedServer = selectedServer,
                onServerSelect = { server ->
                    selectedServer = server
                    selectServer(server) // Вызываем функцию выбора сервера
                }
            )
        }
    }


    private fun loadServers(callback: (List<ApiService.VpnServer>) -> Unit) {
        val token = null;

        apiService.getServers("Bearer $token").enqueue(object : Callback<List<ApiService.VpnServer>> {
            override fun onResponse(call: Call<List<ApiService.VpnServer>>, response: Response<List<ApiService.VpnServer>>) {
                if (response.isSuccessful) {
                    callback(response.body() ?: emptyList())
                } else {
                    Log.e("ServerSelection", "Ошибка загрузки серверов: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<ApiService.VpnServer>>, t: Throwable) {
                Log.e("ServerSelection", "Ошибка сети: ${t.message}")
            }
        })
    }


    private fun selectServer(server: ApiService.VpnServer) {
        val token = null
        val uniqueId = getUniqueDeviceId(this)

        apiService.getConfig("Bearer $token", server.id, uniqueId).enqueue(object : Callback<ApiService.VpnConfig> {
            override fun onResponse(call: Call<ApiService.VpnConfig>, response: Response<ApiService.VpnConfig>) {
                if (response.isSuccessful) {
                    val config = response.body()
                    if (config != null) {
                        // Проверяем, есть ли порт в endpoint
                        var endpoint = server.endpoint
                        if (!endpoint.contains(":")) {
                            endpoint += ":443" // Добавляем стандартный порт, если его нет
                        }

                        Log.d("ServerSelection", "Выбран сервер: ${server.name}, Endpoint: $endpoint")

                        val resultIntent = Intent().apply {
                            putExtra("server_id", server.id)
                            putExtra("server_name", server.name)
                            putExtra("server_location", server.location)
                            putExtra("server_endpoint", endpoint) // ГАРАНТИРУЕМ IP:PORT
                            putExtra("config_private_key", config.privateKey)
                            putExtra("config_public_key", config.publicKey)
                            putExtra("config_address", config.address)
                            putExtra("config_dns", config.dns)
                            putExtra("config_allowed_ips", config.allowedIps)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                } else {
                    Toast.makeText(this@ServerSelectionActivity, "Ошибка загрузки конфигурации", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiService.VpnConfig>, t: Throwable) {
                Toast.makeText(this@ServerSelectionActivity, "Ошибка сети", Toast.LENGTH_SHORT).show()
            }
        })
    }


    fun getUniqueDeviceId(context: Context): String {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        var uniqueId = sharedPreferences.getString("device_id", null)

        if (uniqueId == null) {

            uniqueId = UUID.randomUUID().toString()
            sharedPreferences.edit().putString("device_id", uniqueId).apply()
            Log.d("MainActivity", "НОВЫЙ КЛЮЧ ПОЛЬЗОВАТЕЛЯ: $uniqueId")
        }

        return uniqueId
    }


    @Composable
    fun ServerItem(
        server: ApiService.VpnServer,
        selectedServer: ApiService.VpnServer?,
        onServerSelect: (ApiService.VpnServer) -> Unit
    ) {
//        val isSelected = server == selectedServer
        val backgroundColor = ThemeManager.surface//if (isSelected) Color(0xFFFF6600) else Color.White
        val textColor = ThemeManager.text//if (isSelected) Color.White else Color.Black

        // Определяем флаг по `server.name`
        val flagIcon = serverFlags[server.name] ?: R.drawable.ic_flag_germany

        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clickable { onServerSelect(server) },
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = flagIcon),
                    contentDescription = "Flag",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = server.name,
                    fontSize = 16.sp,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    painter = painterResource(id = R.drawable.ic_signal),
                    contentDescription = "Signal",
                    tint = ThemeManager.primary,//if (isSelected) Color.White else Color(0xFFFF6600),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = "Arrow",
                    tint = ThemeManager.netral,//if (isSelected) Color.White else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

//    @Composable
//    fun ServerListUI(selectedServer: ApiService.VpnServer?, onServerSelect: (ApiService.VpnServer) -> Unit) {
//        var servers by remember { mutableStateOf<List<ApiService.VpnServer>>(emptyList()) }
//
//        LaunchedEffect(Unit) {
//            loadServers { loadedServers ->
//                servers = loadedServers
//            }
//        }
//
//        // ✅ Фон теперь полностью покрывает экран, без белых рамок
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(ThemeManager.background) // ✅ Тёмный фон на весь экран
//        ) {
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(horizontal = 16.dp) // ✅ Отступы только для контента
//            ) {
//                Text(
//                    //text = "Choose server location",
//                    text = getString(R.string.choose_server),
//                    fontSize = 22.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = ThemeManager.onBackground // ✅ Цвет текста правильный
//                )
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                LazyColumn {
//                    items(servers) { server ->
//                        ServerItem(server, selectedServer, onServerSelect)
//                    }
//                }
//            }
//        }
//    }

    @Composable
    fun ServerListUI(selectedServer: ApiService.VpnServer?, onServerSelect: (ApiService.VpnServer) -> Unit) {
        var servers by remember { mutableStateOf<List<ApiService.VpnServer>>(emptyList()) }

        // Количество бесплатных серверов (можно менять)
        val freeServerCount = 1

        LaunchedEffect(Unit) {
            loadServers { loadedServers ->
                servers = loadedServers
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeManager.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = getString(R.string.choose_server),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeManager.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                val freeServers = servers.take(freeServerCount)
                val premiumServers = servers.drop(freeServerCount)

                LazyColumn {
                    if (freeServers.isNotEmpty()) {
                        item {
                            Text(
                                text = getString(R.string.free_servers),//"Бесплатные сервера",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ThemeManager.onBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        items(freeServers) { server ->
                            ServerItem(server, selectedServer, onServerSelect)
                        }
                    }

                    if (premiumServers.isNotEmpty()) {
                        item {
                            Text(
                                text = getString(R.string.premium_servers),//"Премиум сервера",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ThemeManager.onBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        items(premiumServers) { server ->
                            ServerItem(server, selectedServer, onServerSelect)
                        }
                    }
                }
            }
        }
    }



    val serverFlags = mapOf(
        "VPN Germany" to R.drawable.ic_flag_germany,
        "VPN USA" to R.drawable.ic_flag_usa

    )


}
