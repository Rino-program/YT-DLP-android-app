package com.example.ytdlpapp.ui.screen

import android.app.Activity
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ytdlpapp.core.BinaryManager
import com.example.ytdlpapp.core.DownloadManager
import com.example.ytdlpapp.core.FFmpegConverter
import com.example.ytdlpapp.model.*
import com.example.ytdlpapp.service.DownloadService
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "MainScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    binaryManager: BinaryManager,
    downloadManager: DownloadManager,
    ffmpegConverter: FFmpegConverter
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("ダウンロード", "変換")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("yt-dlp ダウンローダー") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                        icon = {
                            Icon(
                                imageVector = if (index == 0) Icons.Default.Download else Icons.Default.Transform,
                                contentDescription = null
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> DownloadTab(binaryManager, downloadManager)
                1 -> ConvertTab(binaryManager, ffmpegConverter)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadTab(
    binaryManager: BinaryManager,
    downloadManager: DownloadManager
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // State
    var url by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var showBinaryDialog by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }
    
    // Options
    var selectedFormat by remember { mutableStateOf(OutputFormat.MP4) }
    var outputDir by remember { mutableStateOf(downloadManager.getDefaultOutputDir().absolutePath) }
    var excludeAv1 by remember { mutableStateOf(true) }
    var playlistPadding by remember { mutableStateOf(2) }
    var customOptions by remember { mutableStateOf("") }
    var useBackgroundDownload by remember { mutableStateOf(false) }
    
    val ytdlpState by binaryManager.ytdlpState.collectAsState()
    val ffmpegState by binaryManager.ffmpegState.collectAsState()
    val downloadState by downloadManager.downloadState.collectAsState()
    val logOutput by downloadManager.logOutput.collectAsState()

    // フォルダ選択
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val path = getPathFromUri(context as Activity, it)
            if (path != null) {
                outputDir = path
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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

        // 出力フォルダ
        OutlinedTextField(
            value = outputDir,
            onValueChange = { outputDir = it },
            label = { Text("出力フォルダ") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { folderPicker.launch(null) }) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "フォルダを選択")
                }
            }
        )

        // フォーマット選択
        var formatExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = formatExpanded,
            onExpandedChange = { formatExpanded = !formatExpanded }
        ) {
            OutlinedTextField(
                value = selectedFormat.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("出力形式") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = formatExpanded,
                onDismissRequest = { formatExpanded = false }
            ) {
                OutputFormat.entries.forEach { format ->
                    DropdownMenuItem(
                        text = { Text(format.displayName) },
                        onClick = {
                            selectedFormat = format
                            formatExpanded = false
                        }
                    )
                }
            }
        }

        // 詳細設定ボタン
        TextButton(onClick = { showSettings = !showSettings }) {
            Icon(
                if (showSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
            Spacer(Modifier.width(4.dp))
            Text("詳細設定")
        }

        // 詳細設定パネル
        if (showSettings) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // バックグラウンドダウンロード
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = useBackgroundDownload,
                            onCheckedChange = { useBackgroundDownload = it }
                        )
                        Column {
                            Text("バックグラウンドダウンロード")
                            Text(
                                "アプリを閉じても継続します",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Divider()

                    // AV1除外オプション
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = excludeAv1,
                            onCheckedChange = { excludeAv1 = it }
                        )
                        Column {
                            Text("AV1コーデックを除外")
                            Text(
                                "YouTube用。再生互換性を高めます",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Divider()

                    // プレイリスト番号の桁数
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("プレイリスト番号の桁数: ", modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (playlistPadding > 1) playlistPadding-- }) {
                                Icon(Icons.Default.Remove, contentDescription = "減らす")
                            }
                            Text("$playlistPadding", style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { if (playlistPadding < 5) playlistPadding++ }) {
                                Icon(Icons.Default.Add, contentDescription = "増やす")
                            }
                        }
                    }
                    Text(
                        "例: 桁数2 → \"01 - タイトル.mp4\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Divider()

                    // カスタムオプション
                    OutlinedTextField(
                        value = customOptions,
                        onValueChange = { customOptions = it },
                        label = { Text("追加オプション") },
                        placeholder = { Text("--cookies cookies.txt など") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // ダウンロードボタン
        Button(
            onClick = {
                if (url.isNotBlank()) {
                    Log.d(TAG, "Download button clicked: url=$url")
                    val options = DownloadOptions(
                        format = selectedFormat,
                        audioOnly = selectedFormat == OutputFormat.MP3 || selectedFormat == OutputFormat.M4A,
                        outputDir = outputDir,
                        excludeAv1 = excludeAv1,
                        playlistPadding = playlistPadding,
                        customOptions = customOptions
                    )
                    
                    if (useBackgroundDownload) {
                        // バックグラウンドサービスで実行
                        Log.d(TAG, "Starting background download service")
                        DownloadService.startDownload(context, url.trim(), options)
                    } else {
                        // フォアグラウンドで実行
                        Log.d(TAG, "Starting foreground download")
                        scope.launch {
                            downloadManager.download(url.trim(), options)
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConvertTab(
    binaryManager: BinaryManager,
    ffmpegConverter: FFmpegConverter
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var inputFile by remember { mutableStateOf("") }
    var outputFormat by remember { mutableStateOf("mp3") }
    var audioBitrate by remember { mutableStateOf("192k") }
    var showLogDialog by remember { mutableStateOf(false) }
    
    val ffmpegState by binaryManager.ffmpegState.collectAsState()
    val convertState by ffmpegConverter.convertState.collectAsState()
    val logOutput by ffmpegConverter.logOutput.collectAsState()

    // ファイル選択
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val path = getFilePathFromUri(context as Activity, it)
            if (path != null) {
                inputFile = path
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // FFmpegが未インストールの場合の警告
        if (!ffmpegState.isInstalled) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "変換にはFFmpegが必要です。\n" +
                        "ダウンロードタブの「管理」からインストールしてください。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        // 出力フォルダの説明
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "変換ファイルは入力ファイルと同じフォルダに保存されます。\n" +
                    "例: /Download/video.webm → /Download/video.mp4",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // 入力ファイル
        OutlinedTextField(
            value = inputFile,
            onValueChange = { inputFile = it },
            label = { Text("変換するファイル") },
            placeholder = { Text("ファイルを選択...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.AudioFile, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { filePicker.launch("*/*") }) {
                    Icon(Icons.Default.FileOpen, contentDescription = "ファイルを選択")
                }
            }
        )

        // 出力形式（音声）
        Text("音声形式", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("mp3", "m4a", "wav", "flac").forEach { format ->
                FilterChip(
                    selected = outputFormat == format,
                    onClick = { outputFormat = format },
                    label = { Text(format.uppercase()) }
                )
            }
        }
        
        // 出力形式（動画）
        Text("動画形式", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("mp4", "webm", "mkv").forEach { format ->
                FilterChip(
                    selected = outputFormat == format,
                    onClick = { outputFormat = format },
                    label = { Text(format.uppercase()) }
                )
            }
        }

        // ビットレート
        if (outputFormat in listOf("mp3", "m4a", "mp4", "webm")) {
            Text("ビットレート", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("128k", "192k", "256k", "320k").forEach { bitrate ->
                    FilterChip(
                        selected = audioBitrate == bitrate,
                        onClick = { audioBitrate = bitrate },
                        label = { Text(bitrate) }
                    )
                }
            }
        }

        // 変換ボタン
        Button(
            onClick = {
                if (inputFile.isNotBlank()) {
                    val options = ConvertOptions(
                        outputFormat = outputFormat,
                        audioBitrate = audioBitrate
                    )
                    scope.launch {
                        ffmpegConverter.convert(inputFile, null, options)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = inputFile.isNotBlank() && 
                     ffmpegState.isInstalled &&
                     convertState !is ConvertState.Converting
        ) {
            Icon(Icons.Default.Transform, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("変換開始")
        }

        // 変換進捗
        ConvertProgress(
            state = convertState,
            onCancel = { ffmpegConverter.cancel() },
            onReset = { ffmpegConverter.reset() },
            onShowLog = { showLogDialog = true }
        )
    }

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
                    Text("yt-dlp: ${if (ytdlpInstalled) "OK" else "未インストール"}")
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (ffmpegInstalled) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (ffmpegInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("FFmpeg: ${if (ffmpegInstalled) "OK" else "未インストール（変換に必要）"}")
                }
            }
            
            FilledTonalButton(onClick = onSetupClick) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("管理")
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
    Card(modifier = Modifier.fillMaxWidth()) {
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
                            Text("ログ")
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
                            Text("ログ")
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
private fun ConvertProgress(
    state: ConvertState,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onShowLog: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            when (state) {
                is ConvertState.Idle -> {
                    Text(
                        "ファイルを選択して変換を開始",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                is ConvertState.Converting -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(state.message)
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
                
                is ConvertState.Completed -> {
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
                            Text("変換完了！")
                            Text(
                                state.outputPath,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        TextButton(onClick = onShowLog) {
                            Text("ログ")
                        }
                        TextButton(onClick = onReset) {
                            Text("閉じる")
                        }
                    }
                }
                
                is ConvertState.Error -> {
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
                            Text("ログ")
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
    // GlobalScope を使用してダイアログが閉じられてもダウンロードを継続
    val ytdlpState by binaryManager.ytdlpState.collectAsState()
    val ffmpegState by binaryManager.ffmpegState.collectAsState()
    
    DisposableEffect(Unit) {
        onDispose { 
            // ダイアログが閉じられてもダウンロードは継続
            // バックグラウンドで実行中のコルーチンはキャンセルしない
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("バイナリ管理") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "※ ダウンロード中にこの画面を閉じても、ダウンロードは継続されます",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(Modifier.height(8.dp))
                
                BinaryItem(
                    name = "yt-dlp",
                    description = "動画ダウンローダー（必須）",
                    isInstalled = ytdlpState.isInstalled,
                    isDownloading = ytdlpState.isDownloading,
                    progress = ytdlpState.progress,
                    error = ytdlpState.error,
                    onInstall = { 
                        // GlobalScope で実行してダイアログクローズ後も継続
                        kotlinx.coroutines.GlobalScope.launch { 
                            binaryManager.installYtdlp() 
                        } 
                    },
                    onUninstall = { binaryManager.uninstallYtdlp() }
                )

                Divider()

                BinaryItem(
                    name = "FFmpeg",
                    description = "ファイル変換（webm/mkv→mp4など）",
                    isInstalled = ffmpegState.isInstalled,
                    isDownloading = ffmpegState.isDownloading,
                    progress = ffmpegState.progress,
                    error = ffmpegState.error,
                    onInstall = { 
                        // GlobalScope で実行してダイアログクローズ後も継続
                        kotlinx.coroutines.GlobalScope.launch { 
                            binaryManager.installFfmpeg() 
                        } 
                    },
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
                    fontSize = 11.sp,
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

// ユーティリティ関数
@Suppress("UNUSED_PARAMETER")
private fun getPathFromUri(activity: Activity, uri: Uri): String? {
    return try {
        val docId = uri.lastPathSegment ?: return null
        if (docId.contains(":")) {
            val split = docId.split(":")
            val type = split[0]
            val path = split.getOrNull(1) ?: ""
            
            if ("primary".equals(type, ignoreCase = true)) {
                "${Environment.getExternalStorageDirectory()}/$path"
            } else {
                "/storage/$type/$path"
            }
        } else {
            uri.path
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get path from URI", e)
        null
    }
}

@Suppress("UNUSED_PARAMETER")
private fun getFilePathFromUri(activity: Activity, uri: Uri): String? {
    return try {
        if (uri.scheme == "content") {
            val cursor = activity.contentResolver.query(uri, null, null, null, null)
            val fileName = cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) c.getString(nameIndex) else "temp_file"
                } else "temp_file"
            } ?: "temp_file"
            
            val tempFile = File(activity.cacheDir, fileName)
            activity.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.absolutePath
        } else {
            uri.path
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get file path from URI", e)
        null
    }
}
