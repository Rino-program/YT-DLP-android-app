package com.example.ytdlpapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ytdlpapp.core.BinaryManager
import com.example.ytdlpapp.core.DownloadManager
import com.example.ytdlpapp.model.DownloadOptions
import com.example.ytdlpapp.model.DownloadState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    binaryManager: BinaryManager,
    downloadManager: DownloadManager
) {
    val scope = rememberCoroutineScope()
    
    // State
    var url by remember { mutableStateOf("") }
    var audioOnly by remember { mutableStateOf(false) }
    var showBinaryDialog by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }
    
    val ytdlpState by binaryManager.ytdlpState.collectAsState()
    val ffmpegState by binaryManager.ffmpegState.collectAsState()
    val downloadState by downloadManager.downloadState.collectAsState()
    val logOutput by downloadManager.logOutput.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("yt-dlp ダウンローダー") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    // バイナリ管理ボタン
                    IconButton(onClick = { showBinaryDialog = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "バイナリ管理",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // バイナリ状態表示
            BinaryStatusCard(
                ytdlpInstalled = ytdlpState.isInstalled,
                ffmpegInstalled = ffmpegState.isInstalled,
                onSetupClick = { showBinaryDialog = true }
            )

            // URL入力
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("ダウンロードURL") },
                placeholder = { Text("https://www.youtube.com/watch?v=...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                trailingIcon = {
                    if (url.isNotEmpty()) {
                        IconButton(onClick = { url = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "クリア")
                        }
                    }
                }
            )

            // オプション
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = audioOnly,
                    onCheckedChange = { audioOnly = it }
                )
                Text("音声のみ（MP3）")
            }

            // ダウンロードボタン
            Button(
                onClick = {
                    if (url.isNotBlank()) {
                        scope.launch {
                            downloadManager.download(
                                url = url.trim(),
                                options = DownloadOptions(audioOnly = audioOnly)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = url.isNotBlank() && 
                         ytdlpState.isInstalled && 
                         downloadState !is DownloadState.Downloading &&
                         downloadState !is DownloadState.Processing
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("ダウンロード開始")
            }

            // 進捗表示
            DownloadProgress(
                state = downloadState,
                onCancel = { downloadManager.cancel() },
                onReset = { downloadManager.reset() },
                onShowLog = { showLogDialog = true }
            )
        }
    }

    // バイナリ管理ダイアログ
    if (showBinaryDialog) {
        BinaryManagerDialog(
            binaryManager = binaryManager,
            onDismiss = { showBinaryDialog = false }
        )
    }

    // ログダイアログ
    if (showLogDialog) {
        LogDialog(
            log = logOutput,
            onDismiss = { showLogDialog = false }
        )
    }
}

@Composable
private fun BinaryStatusCard(
    ytdlpInstalled: Boolean,
    ffmpegInstalled: Boolean,
    onSetupClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (ytdlpInstalled) 
                MaterialTheme.colorScheme.secondaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (ytdlpInstalled) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (ytdlpInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("yt-dlp: ${if (ytdlpInstalled) "インストール済み" else "未インストール"}")
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (ffmpegInstalled) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (ffmpegInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("FFmpeg: ${if (ffmpegInstalled) "インストール済み" else "未インストール（オプション）"}")
                }
            }
            
            if (!ytdlpInstalled) {
                FilledTonalButton(onClick = onSetupClick) {
                    Text("セットアップ")
                }
            }
        }
    }
}

@Composable
private fun DownloadProgress(
    state: DownloadState,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onShowLog: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            when (state) {
                is DownloadState.Idle -> {
                    Text(
                        "URLを入力してダウンロードを開始",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                is DownloadState.Downloading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ダウンロード中... ${state.progress}%")
                            if (state.speed.isNotEmpty()) {
                                Text(
                                    "速度: ${state.speed}  残り: ${state.eta}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, contentDescription = "キャンセル")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = state.progress / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                is DownloadState.Processing -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(state.message)
                    }
                }
                
                is DownloadState.Completed -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ダウンロード完了！")
                            Text(
                                state.filePath,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        TextButton(onClick = onShowLog) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("ログ表示")
                        }
                        TextButton(onClick = onReset) {
                            Text("閉じる")
                        }
                    }
                }
                
                is DownloadState.Error -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            state.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        TextButton(onClick = onShowLog) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("ログ表示")
                        }
                        TextButton(onClick = onReset) {
                            Text("閉じる")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BinaryManagerDialog(
    binaryManager: BinaryManager,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val ytdlpState by binaryManager.ytdlpState.collectAsState()
    val ffmpegState by binaryManager.ffmpegState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("バイナリ管理") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // yt-dlp
                BinaryItem(
                    name = "yt-dlp",
                    description = "動画ダウンローダー（必須）",
                    isInstalled = ytdlpState.isInstalled,
                    isDownloading = ytdlpState.isDownloading,
                    progress = ytdlpState.progress,
                    error = ytdlpState.error,
                    onInstall = { scope.launch { binaryManager.installYtdlp() } },
                    onUninstall = { binaryManager.uninstallYtdlp() }
                )

                Divider()

                // FFmpeg
                BinaryItem(
                    name = "FFmpeg",
                    description = "音声抽出・動画変換（オプション）",
                    isInstalled = ffmpegState.isInstalled,
                    isDownloading = ffmpegState.isDownloading,
                    progress = ffmpegState.progress,
                    error = ffmpegState.error,
                    onInstall = { scope.launch { binaryManager.installFfmpeg() } },
                    onUninstall = { binaryManager.uninstallFfmpeg() }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

@Composable
private fun BinaryItem(
    name: String,
    description: String,
    isInstalled: Boolean,
    isDownloading: Boolean,
    progress: Int,
    error: String?,
    onInstall: () -> Unit,
    onUninstall: () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            when {
                isDownloading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("$progress%", style = MaterialTheme.typography.bodySmall)
                    }
                }
                isInstalled -> {
                    Row {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = onUninstall) {
                            Text("削除")
                        }
                    }
                }
                else -> {
                    Button(onClick = onInstall) {
                        Text("インストール")
                    }
                }
            }
        }
        
        if (error != null) {
            Text(
                "エラー: $error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun LogDialog(
    log: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("実行ログ") },
        text = {
            SelectionContainer {
                Text(
                    text = log.ifEmpty { "ログがありません" },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}
