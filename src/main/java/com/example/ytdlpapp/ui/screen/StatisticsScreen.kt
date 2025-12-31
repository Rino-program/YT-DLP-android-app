package com.example.ytdlpapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ytdlpapp.ui.viewmodel.StatisticsViewModel
import kotlin.math.roundToInt

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // ヘッダー
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "戻る"
                )
            }
            Text(
                text = "ダウンロード統計",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn {
            item {
                if (uiState.totalDownloads > 0) {
                    // 主要統計情報
                    StatisticCard(
                        title = "総ダウンロード数",
                        value = uiState.totalDownloads.toString(),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        StatisticCard(
                            title = "成功",
                            value = uiState.successfulDownloads.toString(),
                            color = Color.Green,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        StatisticCard(
                            title = "失敗",
                            value = uiState.failedDownloads.toString(),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 成功率
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "成功率",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "${uiState.successRate.roundToInt()}%",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            // プログレスバー
                            ProgressBar(
                                progress = uiState.successRate.toInt(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )
                        }
                    }

                    // データ転送量
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "総ダウンロード量",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = formatBytes(uiState.totalBytesDownloaded),
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    // 平均速度
                    if (uiState.averageSpeed > 0) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "平均速度",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = String.format("%.2f KB/s", uiState.averageSpeed),
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.resetStatistics() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("統計情報をリセット")
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "まだダウンロード履歴がありません",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = color
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp),
                color = color
            )
        }
    }
}

@Composable
fun ProgressBar(
    progress: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress / 100f)
                .height(8.dp)
                .background(
                    Color.Green,
                    shape = RoundedCornerShape(4.dp)
                )
        )
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes <= 0 -> "0 B"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
