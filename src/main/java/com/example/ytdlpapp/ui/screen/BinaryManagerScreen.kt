package com.example.ytdlpapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.ytdlpapp.ui.viewmodel.BinaryManagerViewModel

@Composable
fun BinaryManagerScreen(
    viewModel: BinaryManagerViewModel,
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
                text = "バイナリ管理",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // ステータスメッセージ
        if (uiState.statusMessage.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = uiState.statusMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

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

        // yt-dlp管理
        BinaryManagerCard(
            name = "yt-dlp",
            isInstalled = uiState.ytdlp.isInstalled,
            version = uiState.ytdlp.version,
            onInstall = { viewModel.installYtdlp() },
            onUpdate = { viewModel.updateYtdlp() },
            onRemove = { viewModel.removeYtdlp() },
            isLoading = uiState.isInstalling || uiState.isUpdating
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ffmpeg管理
        BinaryManagerCard(
            name = "ffmpeg",
            isInstalled = uiState.ffmpeg.isInstalled,
            version = uiState.ffmpeg.version,
            onInstall = { viewModel.installFfmpeg() },
            onUpdate = { /* ffmpeg自動更新は実装省略 */ },
            onRemove = { viewModel.removeFfmpeg() },
            isLoading = uiState.isInstalling || uiState.isUpdating
        )

        Spacer(modifier = Modifier.height(16.dp))

        // プログレス表示
        if (uiState.progress > 0 && uiState.progress < 100) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ダウンロード進捗: ${uiState.progress}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // プログレスバー実装（SimplifyするためTextのみ）
                }
            }
        }
    }
}

@Composable
fun BinaryManagerCard(
    name: String,
    isInstalled: Boolean,
    version: String,
    onInstall: () -> Unit,
    onUpdate: () -> Unit,
    onRemove: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isInstalled) "✓ インストール済み (v$version)" else "未インストール",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isInstalled) Color.Green else Color.Gray
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                if (!isInstalled) {
                    Button(
                        onClick = onInstall,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        enabled = !isLoading
                    ) {
                        Text("インストール")
                    }
                } else {
                    Button(
                        onClick = onUpdate,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        enabled = !isLoading
                    ) {
                        Text("更新")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onRemove,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        enabled = !isLoading
                    ) {
                        Text("削除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
