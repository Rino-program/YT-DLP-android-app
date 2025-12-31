package com.example.ytdlpapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.ytdlpapp.ui.viewmodel.MainUiState
import com.example.ytdlpapp.ui.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onSettingsClick: () -> Unit,
    onBinaryManagerClick: () -> Unit,
    onBatchProcessingClick: () -> Unit,
    onStatisticsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var ytdlpOptions by remember { mutableStateOf("") }
    var ffmpegOptions by remember { mutableStateOf("") }

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
            Text(
                text = "yt-dlp ダウンローダー",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onStatisticsClick) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "統計"
                )
            }
            IconButton(onClick = onBatchProcessingClick) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "バッチ処理"
                )
            }
            IconButton(onClick = onBinaryManagerClick) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "バイナリ管理"
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "設定"
                )
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                // エラーメッセージ
                if (uiState.errorMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "✗ ${uiState.errorMessage}",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // URL入力
                Text(
                    text = "ダウンロード対象 URL",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = uiState.url,
                    onValueChange = { viewModel.setUrl(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    placeholder = { Text("https://www.youtube.com/watch?v=...") },
                    singleLine = true,
                    enabled = !uiState.isDownloading
                )

                // フォーマット選択
                Text(
                    text = "形式",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = uiState.format,
                    onValueChange = { viewModel.setFormat(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    placeholder = { Text("best (best/worst/bestvideo など)") },
                    singleLine = true,
                    enabled = !uiState.isDownloading
                )

                // 出力先
                Text(
                    text = "出力フォルダ",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = uiState.outputPath,
                    onValueChange = { viewModel.setOutputPath(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    placeholder = { Text("/storage/emulated/0/Downloads") },
                    singleLine = true,
                    enabled = !uiState.isDownloading
                )

                // yt-dlpオプション
                Text(
                    text = "yt-dlp オプション (空白区切り)",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = ytdlpOptions,
                    onValueChange = { ytdlpOptions = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(80.dp),
                    placeholder = { Text("-x --audio-format mp3 --audio-quality 192K") },
                    enabled = !uiState.isDownloading
                )

                // ffmpegオプション
                Text(
                    text = "ffmpeg オプション",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = ffmpegOptions,
                    onValueChange = { ffmpegOptions = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(80.dp),
                    placeholder = { Text("-c:v libx264 -preset fast") },
                    enabled = !uiState.isDownloading
                )

                // ダウンロードボタン
                Button(
                    onClick = {
                        viewModel.startDownload(ytdlpOptions, ffmpegOptions)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .height(48.dp),
                    enabled = !uiState.isDownloading
                ) {
                    if (uiState.isDownloading) {
                        Text("ダウンロード中...")
                    } else {
                        Text("ダウンロード開始")
                    }
                }

                // プログレス表示
                if (uiState.currentDownload != null) {
                    val currentDownload = uiState.currentDownload
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "進捗: ${currentDownload?.progress ?: 0}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // プログレスバー実装は省略
                        }
                    }
                }

                // ログ表示
                Text(
                    text = "実行ログ",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .padding(8.dp)
                    ) {
                        items(uiState.logs) { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }

                // ダウンロード履歴
                if (uiState.downloadHistory.isNotEmpty()) {
                    Text(
                        text = "ダウンロード履歴",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    LazyColumn {
                        items(uiState.downloadHistory) { item ->
                            DownloadHistoryItem(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadHistoryItem(downloadInfo: com.example.ytdlpapp.domain.model.DownloadInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = downloadInfo.title.ifEmpty { downloadInfo.url },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "状態: ${downloadInfo.status.name} (${downloadInfo.progress}%)",
                style = MaterialTheme.typography.labelSmall
            )
            if (downloadInfo.errorMessage != null) {
                Text(
                    text = "エラー: ${downloadInfo.errorMessage}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
