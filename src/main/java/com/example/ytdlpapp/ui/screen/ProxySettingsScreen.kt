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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.example.ytdlpapp.ui.viewmodel.ProxySettingsViewModel

@Composable
fun ProxySettingsScreen(
    viewModel: ProxySettingsViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var expandedProtocol by remember { mutableStateOf(false) }

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
                text = "プロキシ設定",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn {
            item {
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 有効/無効
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "プロキシを有効にする",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = uiState.enabled,
                                onCheckedChange = { viewModel.setEnabled(it) }
                            )
                        }

                        if (uiState.enabled) {
                            // プロトコル選択
                            Text(
                                text = "プロトコル",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { expandedProtocol = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(uiState.protocol)
                                }

                                DropdownMenu(
                                    expanded = expandedProtocol,
                                    onDismissRequest = { expandedProtocol = false }
                                ) {
                                    listOf("http", "https", "socks4", "socks5").forEach { protocol ->
                                        DropdownMenuItem(
                                            text = { Text(protocol) },
                                            onClick = {
                                                viewModel.setProtocol(protocol)
                                                expandedProtocol = false
                                            }
                                        )
                                    }
                                }
                            }

                            // ホスト
                            Text(
                                text = "ホスト",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            OutlinedTextField(
                                value = uiState.host,
                                onValueChange = { viewModel.setHost(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                placeholder = { Text("proxy.example.com") },
                                singleLine = true
                            )

                            // ポート
                            Text(
                                text = "ポート",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            OutlinedTextField(
                                value = uiState.port.toString(),
                                onValueChange = {
                                    viewModel.setPort(it.toIntOrNull() ?: 0)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                placeholder = { Text("8080") },
                                singleLine = true
                            )

                            // ユーザー名
                            Text(
                                text = "ユーザー名 (オプション)",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            OutlinedTextField(
                                value = uiState.username ?: "",
                                onValueChange = { viewModel.setUsername(it.ifEmpty { null }) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                placeholder = { Text("ユーザー名") },
                                singleLine = true
                            )

                            // パスワード
                            Text(
                                text = "パスワード (オプション)",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            OutlinedTextField(
                                value = uiState.password ?: "",
                                onValueChange = { viewModel.setPassword(it.ifEmpty { null }) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                placeholder = { Text("パスワード") },
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.saveProxySettings() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("保存")
                            }
                        }
                    }
                }
            }
        }
    }
}
