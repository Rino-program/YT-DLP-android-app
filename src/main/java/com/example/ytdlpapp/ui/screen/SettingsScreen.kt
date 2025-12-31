package com.example.ytdlpapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ytdlpapp.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
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
                text = "設定",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn {
            item {
                // ダウンロードフォルダ設定
                Text(
                    text = "ダウンロード設定",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ダウンロードフォルダ",
                            style = MaterialTheme.typography.labelLarge
                        )
                        OutlinedTextField(
                            value = uiState.downloadFolder,
                            onValueChange = { viewModel.setDownloadFolder(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            placeholder = { Text("/storage/emulated/0/Downloads") },
                            singleLine = false
                        )
                    }
                }

                // yt-dlpオプション
                Text(
                    text = "yt-dlp 設定",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "デフォルトオプション",
                            style = MaterialTheme.typography.labelLarge
                        )
                        OutlinedTextField(
                            value = uiState.ytdlpOptions,
                            onValueChange = { viewModel.setYtdlpOptions(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .height(120.dp),
                            placeholder = { Text("例: -x --audio-format mp3 --audio-quality 192K") }
                        )
                        Text(
                            text = "複数のオプションは空白で区切ってください。",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // ffmpegオプション
                Text(
                    text = "ffmpeg 設定",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "デフォルトオプション",
                            style = MaterialTheme.typography.labelLarge
                        )
                        OutlinedTextField(
                            value = uiState.ffmpegOptions,
                            onValueChange = { viewModel.setFfmpegOptions(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .height(120.dp),
                            placeholder = { Text("例: -c:v libx264 -preset fast") }
                        )
                    }
                }

                // その他設定
                Text(
                    text = "その他",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 自動更新オプション
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "バイナリの自動更新を有効にする",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = uiState.autoUpdateBinary,
                                onCheckedChange = { viewModel.setAutoUpdateBinary(it) }
                            )
                        }

                        // デバッグログオプション
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "デバッグログを表示",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = uiState.showDebugLog,
                                onCheckedChange = { viewModel.setShowDebugLog(it) }
                            )
                        }
                    }
                }

                // 情報
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "アプリ情報",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "yt-dlp ダウンローダー v1.0.0",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = "yt-dlpとffmpegを使用した動画ダウンロード支援ツール",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
