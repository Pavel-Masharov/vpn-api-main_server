package com.skyrox.vpnapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import kotlin.math.min
import kotlin.math.roundToInt

class SpeedTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpeedTestScreen()
        }
    }

    @Composable
    fun SpeedTestScreen() {
        var downloadSpeed by remember { mutableStateOf(0.0) }
        var isTesting by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Кнопка назад
            Button(
                onClick = { finish() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(text = "Back")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Заголовок
            Text(text = "Speed Test", fontSize = 24.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // График скорости (аналог стрелочного индикатора)
            SpeedMeter(downloadSpeed)

            Spacer(modifier = Modifier.height(32.dp))

            // Показатель скорости
            Text( // ✅ Используем Text вместо BasicText
                text = "${downloadSpeed.roundToInt()} MB/s",
                fontSize = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка теста скорости
            Button(
                onClick = {
                    if (!isTesting) {
                        coroutineScope.launch {
                            isTesting = true
                            downloadSpeed = measureDownloadSpeed()
                            isTesting = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (isTesting) "Testing..." else "Start Test", fontSize = 18.sp)
            }
        }
    }

    @Composable
    fun SpeedMeter(speed: Double) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val angle = (speed / 100) * 180 // Пропорционально максимуму (100 MB/s)
            rotate(angle.toFloat()) {
                drawLine(
                    color = Color.Red,
                    start = center.copy(y = size.height * 0.75f),
                    end = center.copy(y = size.height * 0.25f),
                    strokeWidth = 8f
                )
            }
        }
    }

    private suspend fun measureDownloadSpeed(): Double {
        return withContext(Dispatchers.IO) {
            val testUrl = "https://proof.ovh.net/files/100Mb.dat" // Файл для теста
            val client = OkHttpClient()
            val request = Request.Builder().url(testUrl).build()

            try {
                val startTime = System.currentTimeMillis()
                val response = client.newCall(request).execute()
                val inputStream: InputStream? = response.body?.byteStream()
                val buffer = ByteArray(1024 * 1024) // 1 MB буфер
                var totalBytesRead = 0L
                val durationMillis = 5000 // 5 секунд теста

                val start = System.currentTimeMillis()
                while (System.currentTimeMillis() - start < durationMillis) {
                    val bytesRead = inputStream?.read(buffer) ?: break
                    if (bytesRead == -1) break
                    totalBytesRead += bytesRead
                }

                val endTime = System.currentTimeMillis()
                val speedMbps = (totalBytesRead * 8 / 1024.0 / 1024.0) / ((endTime - startTime) / 1000.0)
                response.close()
                Log.d("SpeedTest", "Download speed: $speedMbps Mbps")
                min(speedMbps, 100.0) // Ограничение до 100 Мбит/с для индикатора
            } catch (e: Exception) {
                Log.e("SpeedTest", "Error measuring speed", e)
                0.0
            }
        }
    }
}
