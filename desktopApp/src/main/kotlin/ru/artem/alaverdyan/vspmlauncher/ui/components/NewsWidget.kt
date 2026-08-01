package ru.artem.alaverdyan.vspmlauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NewsItem(val title: String, val body: String)

/**
 * Полупрозрачный стеклянный виджет новостей сервера.
 */
// NewsWidget.kt — принимает scale, масштабирует все шрифты и отступы
@Composable
fun NewsWidget(
    news: List<NewsItem>,
    modifier: Modifier = Modifier,
    scale: Float = 1f
) {
    GlassPanel(modifier = modifier) {
        Column(modifier = Modifier.padding(18.dp * scale)) {
            Text(
                text = "Новости сервера",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (18 * scale).sp
            )
            Spacer(Modifier.height(10.dp * scale))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp * scale)) {
                items(news) { item ->
                    Column {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = (14 * scale).sp
                        )
                        Spacer(Modifier.height(2.dp * scale))
                        Text(
                            text = item.body,
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = (13 * scale).sp
                        )
                    }
                }
            }
        }
    }
}