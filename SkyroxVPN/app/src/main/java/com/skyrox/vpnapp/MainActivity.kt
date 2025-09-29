package com.skyrox.vpnapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.TrafficStats
import android.net.VpnService
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import com.skyrox.vpnapp.ui.theme.ThemeManager


import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {
    private lateinit var wireGuardManager: WireGuardManager
    private lateinit var vpnPermissionLauncher: ActivityResultLauncher<Intent>
    private val apiService = ApiService.create()
    private var selectedServer by mutableStateOf<ApiService.VpnServer?>(null)
    private var vpnConfig = mutableStateOf<ApiService.VpnConfig?>(null)
    private var uploadedData by mutableStateOf(0.0)
    private var downloadedData by mutableStateOf(0.0)

    private var initialTxBytes: Long = 0
    private var initialRxBytes: Long = 0

    private val serverSelectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                val endpoint = data.getStringExtra("server_endpoint") ?: ""
                Log.d("MainActivity", "Получен сервер: ${data.getStringExtra("server_name")}, Endpoint: $endpoint")

                selectedServer = ApiService.VpnServer(
                    id = data.getIntExtra("server_id", -1),
                    name = data.getStringExtra("server_name") ?: "Unknown",
                    location = data.getStringExtra("server_location") ?: "",
                    endpoint = endpoint,
                    load = 0 // Пока что оставим 0, если API не возвращает загрузку
                )

                vpnConfig.value = ApiService.VpnConfig(
                    privateKey = data.getStringExtra("config_private_key") ?: "",
                    publicKey = data.getStringExtra("config_public_key") ?: "",
                    address = data.getStringExtra("config_address") ?: "",
                    dns = data.getStringExtra("config_dns") ?: "",
                    endpoint = endpoint, // Используем endpoint из intent
                    allowedIps = data.getStringExtra("config_allowed_ips") ?: ""
                )

                Log.d("MainActivity", "Выбран сервер: ${selectedServer?.name}, Endpoint: ${vpnConfig.value?.endpoint}")
            }
        }
    }





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)

        wireGuardManager = WireGuardManager(this)


        vpnPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d("MainActivity", "VPN разрешение получено")
            } else {
                Log.e("MainActivity", "VPN разрешение не получено")
            }
        }


        if (!hasVpnPermission(this)) {
            Log.d("MainActivity", "VPN разрешение НЕТ!")
            vpnPermissionLauncher.launch(VpnService.prepare(this))
        } else {
            Log.d("MainActivity", "VPN разрешение уже есть, можно подключаться!")
        }

        val userKey = getUniqueDeviceId(this)
        updateKey(userKey)


        if (VpnConfigManager.selectedServer != null && VpnConfigManager.vpnConfig != null) {
            selectedServer = VpnConfigManager.selectedServer
            vpnConfig.value = VpnConfigManager.vpnConfig
        } else {
            loadFirstServer(userKey) // Загружаем первый сервер, если конфиг не установлен
        }

        setContent {
            VpnAppUI()
        }
    }

//Проверка разрешения vpn
    fun hasVpnPermission(context: Context): Boolean {
        return VpnService.prepare(context) == null
    }

//получаем ключ пользователя из кеша
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


    fun updateKey(key: String) {

        Log.d("MainActivity", "updateKey")
        apiService.updateUserKey(key).enqueue(object : Callback<ApiService.KeyResponse> {
            override fun onResponse(call: Call<ApiService.KeyResponse>, response: Response<ApiService.KeyResponse>) {
                if (response.isSuccessful) {
                    val keyResponse = response.body()
                    println("УСПЕШНО: ${keyResponse?.message}, Key: ${keyResponse?.key}")
                } else {
                    println("ОШИБКА: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ApiService.KeyResponse>, t: Throwable) {
                println("API request failed: ${t.message}")
            }
        })
    }



    private fun loadFirstServer(keyUser:String) {
        val token = null
        apiService.getServers("Bearer $token").enqueue(object : Callback<List<ApiService.VpnServer>> {
            override fun onResponse(call: Call<List<ApiService.VpnServer>>, response: Response<List<ApiService.VpnServer>>) {
                if (response.isSuccessful) {
                    val serverList = response.body() ?: emptyList()
                    if (serverList.isNotEmpty()) {
                        selectedServer = serverList.first()
                       // selectedServer = serverList[1]
                        getVpnConfig(selectedServer!!.id, keyUser)
                    }
                } else {
                    Log.e("MainActivity", "Ошибка загрузки серверов: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<ApiService.VpnServer>>, t: Throwable) {
                Log.e("MainActivity", "Ошибка сети: ${t.message}")
            }
        })
    }



    private fun getVpnConfig(serverId: Int, keyUser: String) {
        val token = null
        Log.d("MainActivity", "Отправляем запрос на конфиг для сервера ID: $serverId")

        apiService.getConfig("Bearer $token", serverId, keyUser)
            .enqueue(object : Callback<ApiService.VpnConfig> {
                override fun onResponse(call: Call<ApiService.VpnConfig>, response: Response<ApiService.VpnConfig>) {
                    Log.d("MainActivity", "Запрос: ${call.request()}")
                    Log.d("MainActivity", "Заголовки запроса: ${call.request().headers}")

                    if (response.isSuccessful) {
                        val config = response.body()
                        if (config != null) {
                            vpnConfig.value = config  // Сохраняем объект VpnConfig
                            Log.d("MainActivity", "WireGuard-конфиг: \n${generateWireGuardConfig(config)}")
                        } else {
                            Log.e("MainActivity", "Ошибка: JSON успешно получен, но `body()` = null")
                        }
                    } else {

                        Log.e("MainActivity", "Ошибка загрузки конфига: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<ApiService.VpnConfig>, t: Throwable) {
                    Log.e("MainActivity", "Ошибка сети при загрузке конфига: ${t.message}")
                }
            })
    }

    private fun generateWireGuardConfig(vpnConfig: ApiService.VpnConfig): String {
        return """
        [Interface]
        PrivateKey = ${vpnConfig.privateKey}
        Address = ${vpnConfig.address}
        DNS = ${vpnConfig.dns}

        [Peer]
        PublicKey = ${vpnConfig.publicKey}
        AllowedIPs = ${vpnConfig.allowedIps}, ::/0
        Endpoint = ${vpnConfig.endpoint}
        PersistentKeepalive = 25
    """.trimIndent()
    }


    @Composable
    fun AutoResizeText(
        text: String,
        modifier: Modifier = Modifier,
        maxFontSize: TextUnit = 18.sp, // Начальный размер шрифта
        minFontSize: TextUnit = 12.sp // Минимальный размер шрифта
    ) {
        var textSize by remember { mutableStateOf(maxFontSize) }
        var readyToDraw by remember { mutableStateOf(false) }

        Text(
            text = text,
            color = Color.White,
            fontSize = textSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.End,
            modifier = modifier,
            softWrap = false, // Запрещаем перенос строк
            onTextLayout = { textLayoutResult ->
                if (textLayoutResult.didOverflowWidth && textSize > minFontSize) {
                    textSize = (textSize.value - 1).sp // Уменьшаем размер
                } else {
                    readyToDraw = true
                }
            }
        )
    }



    @Composable
    fun SpeedCard(
        label: String,
        speed: Double,
        isConnected: Boolean, // ✅ Проверяем подключение
        icon: Int,
        modifier: Modifier = Modifier,
        cornerShape: Shape
    ) {
        val textColor = if (isConnected) ThemeManager.text else ThemeManager.netralText // ✅ Меняем цвет текста
        val iconColor = if (isConnected) ThemeManager.primary else ThemeManager.netral // ✅ Меняем цвет стрелки

        Card(
            colors = CardDefaults.cardColors(containerColor = ThemeManager.card),
            shape = cornerShape,
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = modifier
                .padding(vertical = 8.dp)
                .height(100.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        color = textColor, // ✅ Меняем цвет в зависимости от `isConnected`
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(ThemeManager.inBoxArrow, shape = RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = icon),
                            contentDescription = label,
                            tint = iconColor, // ✅ Меняем цвет стрелки
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = String.format("%.2f", speed),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor // ✅ Меняем цвет числа скорости
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "mb/s",
                        fontSize = 14.sp,
                        color = textColor // ✅ Меняем цвет "mb/s"
                    )
                }
            }
        }
    }




    @Composable
    fun VpnAppUI() {
        var isConnected by remember { mutableStateOf(false) }
        var startTime by remember { mutableStateOf(0L) }
        var elapsedTime by remember { mutableStateOf("00:00:00") }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(isConnected) {
            if (isConnected) {
                startTime = SystemClock.elapsedRealtime()
                while (isConnected) {
                    val currentTime = SystemClock.elapsedRealtime()
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(currentTime - startTime)
                    elapsedTime = String.format(
                        "%02d:%02d:%02d",
                        seconds / 3600,
                        (seconds % 3600) / 60,
                        seconds % 60
                    )
                    val currentTxBytes = TrafficStats.getTotalTxBytes()
                    val currentRxBytes = TrafficStats.getTotalRxBytes()

                    uploadedData = (currentTxBytes - initialTxBytes) / (1024.0 * 1024.0) // В мегабайты
                    downloadedData = (currentRxBytes - initialRxBytes) / (1024.0 * 1024.0) // В мегабайты

                    delay(1000)
                }
            } else {
                elapsedTime = "00:00:00"
                uploadedData = 0.0
                downloadedData = 0.0
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeManager.background) // ✅ Фон экрана
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp), // ✅ Отступ снизу для кнопки
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Верхняя панель
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Кнопка Settings
                    Button(
                        onClick = {
                            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                            startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.elevatedButtonElevation(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_menu),
                            contentDescription = "Menu",
                            tint = ThemeManager.netral,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Кнопка Premium
                    Button(
                        onClick = {
                            val intent = Intent(this@MainActivity, SubscriptionActivity::class.java)
                            startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.primary),//Color(0xFFFF6600)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.elevatedButtonElevation(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.width(180.dp).height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_premium),
                                contentDescription = "Premium",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AutoResizeText(
                                //text = "Go Premium",
                                text = getString(R.string.premium_button),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Карточки скорости
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SpeedCard(
                        label = getString(R.string.download),
                        speed = downloadedData,
                        isConnected = isConnected,
                        icon = R.drawable.ic_download,
                        modifier = Modifier.weight(1f),
                        cornerShape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
                    SpeedCard(
                        label = getString(R.string.upload),
                        speed = uploadedData,
                        isConnected = isConnected,
                        icon = R.drawable.ic_upload,
                        modifier = Modifier.weight(1f),
                        cornerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f)) // ✅ Раздвигает контент вверх

                // Кнопка подключения
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(120.dp).aspectRatio(1f)
                ) {
                    Button(
                        onClick = {
                            if (!hasVpnPermission(this@MainActivity)) {
                                vpnPermissionLauncher.launch(VpnService.prepare(this@MainActivity))
                                return@Button
                            }
                            if (isConnected) {
                                coroutineScope.launch { wireGuardManager.disconnectTunnel() }
                                isConnected = false
                            } else {
                                vpnConfig.value?.let { config ->
                                    val wireGuardConfig = generateWireGuardConfig(config)

                                    initialTxBytes = TrafficStats.getTotalTxBytes()
                                    initialRxBytes = TrafficStats.getTotalRxBytes()

                                    Log.d("MainActivity", "Подключение к серверу: ${selectedServer?.name}")
                                    coroutineScope.launch { wireGuardManager.connectTunnel(wireGuardConfig) }
                                    isConnected = true
                                } ?: Toast.makeText(this@MainActivity, "Конфиг не загружен", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) ThemeManager.primary else ThemeManager.surface
                        ),
                        shape = CircleShape,
                        elevation = ButtonDefaults.elevatedButtonElevation(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {}


                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(100.dp)
                            .aspectRatio(1f)
                            .border(
                                width = 2.dp,
                                color = if (isConnected) ThemeManager.background else ThemeManager.primary,
                                shape = CircleShape
                            )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = if (isConnected) R.drawable.ic_power_stop else R.drawable.ic_power_start),
                                contentDescription = "Power",
                                tint = if (isConnected) ThemeManager.surface else ThemeManager.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                //text = if (isConnected) "Stop" else "Start",
                                text = if (isConnected) getString(R.string.vpn_stop) else getString(R.string.vpn_start),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) ThemeManager.text else ThemeManager.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))


                Text(
                    text = if (isConnected) "Safely connected" else "Not connected",
                    fontSize = 20.sp,
                    color = ThemeManager.text
                )
                Text(
                    text = elapsedTime,
                    fontSize = 18.sp,
                    color = ThemeManager.netral,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Кнопка выбора сервера (Прижата к нижнему краю)
                Button(
                    onClick = {
                        val intent = Intent(this@MainActivity, ServerSelectionActivity::class.java)
                        serverSelectionLauncher.launch(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.primary),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.elevatedButtonElevation(8.dp),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        val selectedServerName = selectedServer?.name ?: "VPN Germany" // Значение по умолчанию
                        val flagIcon = serverFlags[selectedServerName]?: R.drawable.ic_flag_germany


                        Icon(
                            painter = painterResource(id = flagIcon),
                            contentDescription = "Flag",
                            tint = Color.Unspecified,
                            modifier = Modifier
                                    .size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedServer?.name ?: "Select Server",
                            fontSize = 18.sp,
                            color = ThemeManager.text
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_down),
                            contentDescription = "Arrow",
                            tint = ThemeManager.netral,
                            modifier = Modifier
                                .size(30.dp)
                        )
                    }
                }
            }
        }
    }
    //Вывести в отдельный класс
    val serverFlags = mapOf(
        "VPN Germany" to R.drawable.ic_flag_germany,
        "VPN USA" to R.drawable.ic_flag_usa

    )




}
