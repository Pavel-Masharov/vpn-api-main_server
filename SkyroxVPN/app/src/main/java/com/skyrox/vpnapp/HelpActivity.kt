package com.skyrox.vpnapp

import android.content.Intent
import android.net.Uri
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
import com.skyrox.vpnapp.ui.theme.ThemeManager


class HelpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HelpScreen()
        }
    }
}

@Composable
fun HelpScreen() {
    val context = LocalContext.current

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
                    .clickable { (context as? ComponentActivity)?.finish() }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_left),
                    contentDescription = "Back",
                    tint = ThemeManager.netral,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))


        // 🔹 Текст заголовка
        Text(
            text = stringResource(id = R.string.text_help_info),
            fontSize = 18.sp,
            color = ThemeManager.text,
            modifier = Modifier.padding(16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 🔹 Блок "Обратная связь"
        Text(text = stringResource(id = R.string.text_callback), fontSize = 16.sp, color = ThemeManager.netralText)
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "masharovpavel1991@gmail.com",
            fontSize = 18.sp,
            color = ThemeManager.primary,
            modifier = Modifier.clickable {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:masharovpavel1991@gmail.com")
                    putExtra(Intent.EXTRA_SUBJECT, "Support VPN App")
                }
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 🔹 Кнопки "Privacy Policy" и "Terms of Use"
        Button(
            onClick = { openUrl(context, "https://dzen.ru/") },
            colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = stringResource(id = R.string.privacy_policy),
                fontSize = 14.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { openUrl(context, "https://dzen.ru/") },
            colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = stringResource(id = R.string.terms_of_use),
                fontSize = 14.sp, color = Color.White)
        }
    }
}

// Функция для открытия URL
fun openUrl(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}
