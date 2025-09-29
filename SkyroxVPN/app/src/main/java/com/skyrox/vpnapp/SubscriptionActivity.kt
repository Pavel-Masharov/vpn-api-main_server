package com.skyrox.vpnapp

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyrox.vpnapp.subscription.SubscriptionManager
import com.skyrox.vpnapp.ui.theme.ThemeManager

class SubscriptionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SubscriptionScreen()
        }
    }
}

@Composable
fun SubscriptionScreen() {
    val context = LocalContext.current
    val subscriptionManager = remember { SubscriptionManager(context) }

    // 🔹 Карта SKU для подписок
    val subscriptionSkus = mapOf(
        "Daily" to "vpn_premium_daily",
        "Weekly" to "vpn_premium_weekly",
        "Monthly" to "vpn_premium_monthly",
        "Yearly" to "vpn_premium_yearly"
    )

    // 🔹 Текущее активное SKU (по умолчанию "Monthly")
    var activeSku by remember { mutableStateOf(subscriptionSkus["Monthly"] ?: "") }
    var selectedPlan by remember { mutableStateOf("Monthly") }

    // 🔹 Проверяем, есть ли активная подписка
    LaunchedEffect(Unit) {
        subscriptionManager.isSubscriptionActive { isActive ->
            if (!isActive) {
                activeSku = subscriptionSkus["Monthly"] ?: "" // Устанавливаем "Monthly" по умолчанию
                selectedPlan = "Monthly"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeManager.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 🔙 Кнопка "Назад"
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(ThemeManager.surface, shape = RoundedCornerShape(12.dp))
                    .clickable { (context as? Activity)?.finish() }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_left),
                    contentDescription = "Back",
                    tint = ThemeManager.text,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 🔹 Заголовок
        Text(text = stringResource(id = R.string.upgrade_premium), fontSize = 22.sp, color = ThemeManager.text)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(id = R.string.text_info_settings_1), fontSize = 16.sp, color = ThemeManager.netralText)
        Spacer(modifier = Modifier.height(24.dp))

        // 🔹 Выбор тарифов
        Text(text = stringResource(id = R.string.choose_plan), fontSize = 14.sp, color = ThemeManager.netralText)
        Spacer(modifier = Modifier.height(12.dp))

        // 🔹 Список подписок
        Column(modifier = Modifier.fillMaxWidth()) {
            subscriptionSkus.keys.forEach { plan ->
                SubscriptionOption(
                    period = plan,
                    price = when (plan) {
                        "Daily" -> "$0.99"
                        "Weekly" -> "$1.99"
                        "Monthly" -> "$9.99"
                        "Yearly" -> "$99.99"
                        else -> ""
                    },
                    selectedPlan = selectedPlan
                ) {
                    selectedPlan = plan
                    activeSku = subscriptionSkus[plan] ?: "" // ✅ Обновляем активный SKU
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🔹 Кнопка "Upgrade"
        Button(
            onClick = {
                subscriptionManager.purchaseSubscription(context as Activity, activeSku) // ✅ Покупаем выбранную подписку
            },
            colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = stringResource(id = R.string.upgrade), fontSize = 18.sp, color = Color.White)
        }
    }
}

@Composable
fun SubscriptionOption(period: String, price: String, selectedPlan: String, onSelect: (String) -> Unit) {
    val isSelected = period == selectedPlan

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(period) }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) ThemeManager.primary else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeManager.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = price, fontSize = 20.sp, color = ThemeManager.text)
                Text(text = period, fontSize = 16.sp, color = ThemeManager.netralText)
            }
        }
    }
}
