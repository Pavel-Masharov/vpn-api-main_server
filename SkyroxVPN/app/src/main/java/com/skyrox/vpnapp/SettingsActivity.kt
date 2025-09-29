package com.skyrox.vpnapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.skyrox.vpnapp.subscription.SubscriptionManager
import com.skyrox.vpnapp.ui.theme.ThemeManager


class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var themeState by remember { mutableStateOf(ThemeManager.isDarkTheme) }

            DisposableEffect(Unit) {
                val observer = { themeState = ThemeManager.isDarkTheme }
                ThemeManager.addObserver(observer)
                onDispose { }
            }

            SettingsScreen()
        }
    }

    @Composable
    fun SettingsScreen() {
        val context = LocalContext.current // ✅ Контекст для вызова Intent

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeManager.background) // ✅ Цвет фона из ThemeManager
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🔹 Кнопка "назад"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { finish() },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.elevatedButtonElevation(8.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_left),
                        contentDescription = "Back",
                        tint = ThemeManager.netral,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 🔹 Индикатор подписки
            SubscriptionIndicator()

            Spacer(modifier = Modifier.height(24.dp))

            // 🔹 Настройки
//            SettingsOption(R.drawable.ic_preferences, getString(R.string.button_preferences)) { /* Действие */ }
//            SettingsOption(R.drawable.ic_speed_test, getString(R.string.button_speed_test)) {
//                context.startActivity(Intent(context, SpeedTestActivity::class.java))
//            }
//            SettingsOption(R.drawable.ic_refer_friends, getString(R.string.button_referals)) { /* Действие */ }
            SettingsOption(R.drawable.ic_help_support, getString(R.string.button_help)) {

            /* Действие */
                context.startActivity(Intent(context, HelpActivity::class.java))
            }

            // 🔹 Переключатель смены темы
            SettingsOptionWithSwitch(
                icon = R.drawable.ic_theme,
                title = getString(R.string.button_change_theme),
                isChecked = ThemeManager.isDarkTheme,
                onCheckedChange = { ThemeManager.toggleTheme() }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 🔹 Версия приложения
            Text(
                text = "App version 1.2.2",
                color = ThemeManager.onBackground,
                fontSize = MaterialTheme.typography.bodySmall.fontSize
            )
        }
    }

    @Composable
    fun SubscriptionIndicator(totalDays: Int = 365) {
       // val progress = daysLeft.toFloat() / totalDays // Прогресс от 0 до 1
        val context = LocalContext.current // ✅ Получаем Context
        val subscriptionManager = remember { SubscriptionManager(context) }
        var daysLeft by remember { mutableStateOf(0) } // ✅ Количество оставшихся дней подписки
        var textInfo by remember { mutableStateOf("Проверка подписки...") } // ✅ Делает переменную реактивной


        LaunchedEffect(Unit) {
            subscriptionManager.getRemainingDays { days ->
                daysLeft = days // ✅ Обновляем оставшиеся дни
            }

            subscriptionManager.isSubscriptionActive { isActive ->
                textInfo = if (isActive) {
                    Log.d("Subscription", "✅ Подписка активна")
                    context.getString(R.string.text_secured)
                } else {
                    Log.d("Subscription", "❌ Подписка истекла")
                    "Не активна"
                }
            }
        }


        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                val progress = daysLeft.toFloat() / totalDays // ✅ Прогресс от 0 до 1
                Canvas(
                    modifier = Modifier.size(120.dp)
                ) {
                    // Серый фоновый круг
                    drawArc(
                        color = ThemeManager.netral,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Оранжевый прогресс
                    drawArc(
                        color = ThemeManager.primary, // Оранжевый цвет, как в дизайне
                        startAngle = -90f,
                        sweepAngle = 360 * progress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Текст с количеством дней
                Text(
                    text = "$daysLeft",
                    color = ThemeManager.text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

//            val context = LocalContext.current // ✅ Получаем Context
//            val subscriptionManager = remember { SubscriptionManager(context) }
//            var textInfo = "";

//            LaunchedEffect(Unit) {
//                subscriptionManager.isSubscriptionActive { isActive ->
//                    textInfo = if (isActive) {
//                        Log.d("Subscription", "✅ Подписка активна")
//                        context.getString(R.string.text_secured)
//                    } else {
//                        Log.d("Subscription", "❌ Подписка истекла")
//                        context.getString(R.string.text_no_subscription)
//                        "Не активна"
//                    }
//                }
//            }


            Text(
                //text = getString(R.string.text_secured),
                text = textInfo,
                color = ThemeManager.text,
                fontSize = 16.sp
            )
//            Text(
//                text = "Oct 22nd, 2021",
//                color = ThemeManager.text,
//                fontSize = 16.sp,
//                fontWeight = FontWeight.Bold
//            )
        }
    }


    @Composable
    fun SettingsOption(icon: Int, title: String, onClick: () -> Unit) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = title,
                    tint = ThemeManager.primary, // Устанавливаем цвет
                    modifier = Modifier
                        .size(36.dp)
                        .padding(end = 16.dp)
                )

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = ThemeManager.text,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    tint = ThemeManager.netral,
                    contentDescription = "Arrow",
                    modifier = Modifier.size(20.dp)
                )
            }
            HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
        }
    }


    @Composable
    fun SettingsOptionWithSwitch(icon: Int, title: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = title,
                    tint = ThemeManager.primary, // ✅ Цвет из ThemeManager
                    modifier = Modifier
                        .size(36.dp)
                        .padding(end = 16.dp)
                )

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = ThemeManager.text, // ✅ Цвет из ThemeManager
                    modifier = Modifier.weight(1f)
                )

                Switch(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = ThemeManager.netral, // ✅ Цвет из ThemeManager
                        uncheckedTrackColor = ThemeManager.netral,
                        checkedThumbColor =  ThemeManager.primary, // ✅ Цвет самого кружка (вкл.)
                        uncheckedThumbColor = ThemeManager.primary // ✅ Цвет самого кружка (выкл.)
                    )
                )
            }
            HorizontalDivider(color = ThemeManager.onSurface, thickness = 1.dp) // ✅ Цвет из ThemeManager
        }
    }


}
