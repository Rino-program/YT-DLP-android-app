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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import com.example.ytdlpapp.ui.viewmodel.QueueManagerViewModel
import com.example.ytdlpapp.domain.model.QueueStatus

@Composable
fun BatchProcessingScreen(
    viewModel: QueueManagerViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var urlInput by remember { mutableStateOf("") }
    var filePathInput by remember { mutableStateOf("") }

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
                text = "バッチ処理",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn {
            item {
                // ステータスメッセージ
                if (uiState.statusMessage.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = uiState.statusMessage,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // URLを1個ずつ追加
                Text(
                    text = "URLを追加",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    placeholder = { Text("https://www.youtube.com/watch?v=...") },
                    label = { Text("URL") }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Button(
                        onClick = {
                            if (urlInput.isNotEmpty()) {
                                viewModel.addSingleUrl(
                                    urlInput,
                                    uiState.format,
                                    uiState.outputPath
                                )
                                urlInput = ""
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("追加")
                    }
                }

                // ファイルからインポート
                Text(
                    text = "ファイルからインポート",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = filePathInput,
                    onValueChange = { filePathInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    placeholder = { Text("/storage/emulated/0/urls.txt") },
                    label = { Text("ファイルパス") }
                )

                Button(
                    onClick = {
                        if (filePathInput.isNotEmpty()) {
                            viewModel.addFromFile(
                                filePathInput,
                                uiState.outputPath
                            )
                            filePathInput = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text("インポート")
                }

                // キューの進捗
                if (uiState.queueItems.isNotEmpty()) {
                    Text(
                        text = "キュー中のアイテム (${uiState.queueCount})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("成功: ${uiState.completedCount}")
                            Text("失敗: ${uiState.failedCount}")
                            Text("キュー中: ${uiState.queueCount}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.startProcessing() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isProcessing
                            ) {
                                Text(if (uiState.isProcessing) "処理中..." else "処理開始")
                            }
                        }
                    }

                    // キュー内のアイテム一覧
                    LazyColumn {
                        items(uiState.queueItems) { item ->
                            QueueItemCard(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QueueItemCard(queueItem: com.example.ytdlpapp.domain.model.QueuedDownload) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = queueItem.title.ifEmpty { queueItem.url },
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "状態: ${queueItem.status.name}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${queueItem.progress}%",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (queueItem.errorMessage != null) {
                Text(
                    text = "エラー: ${queueItem.errorMessage}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
