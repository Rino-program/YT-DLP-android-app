package com.example.ytdlpapp.core

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.ytdlpapp.model.ConvertOptions
import com.example.ytdlpapp.model.ConvertState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.RandomAccessFile

/**
 * FFmpegを使用したファイル変換
 */
class FFmpegConverter(
    private val context: Context,
    private val binaryManager: BinaryManager
) {
    companion object {
        private const val TAG = "FFmpegConverter"
    }
    
    private val _convertState = MutableStateFlow<ConvertState>(ConvertState.Idle)
    val convertState: StateFlow<ConvertState> = _convertState.asStateFlow()
    
    private val _logOutput = MutableStateFlow("")
    val logOutput: StateFlow<String> = _logOutput.asStateFlow()
    
    private var currentProcess: Process? = null
    
    /**
     * ELFバイナリがPIE(Position Independent Executable)形式かどうかをチェック
     * PIE形式(e_type: 3 = ET_DYN)の場合のみlinkerで実行可能
     */
    private fun isPieExecutable(filePath: String): Boolean {
        return try {
            RandomAccessFile(File(filePath), "r").use { raf ->
                // ELFマジックナンバーをチェック (0x7F 'E' 'L' 'F')
                val magic = ByteArray(4)
                raf.read(magic)
                if (magic[0] != 0x7F.toByte() || magic[1] != 'E'.code.toByte() ||
                    magic[2] != 'L'.code.toByte() || magic[3] != 'F'.code.toByte()) {
                    return false
                }
                
                // e_type フィールドを読み取る (オフセット 0x10)
                raf.seek(0x10)
                val eType = raf.readShort().toInt() and 0xFFFF
                // ET_DYN (3) = PIE実行可能ファイルまたは共有オブジェクト
                eType == 3
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if binary is PIE: ${e.message}")
            false
        }
    }
    
    /**
     * デフォルトの出力フォルダを取得
     * 入力ファイルと同じフォルダに出力
     */
    fun getDefaultOutputDir(): File {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadDir, "YtDlp/Converted").apply { mkdirs() }
    }
    
    private fun appendLog(text: String) {
        _logOutput.value += text
    }
    
    /**
     * ファイルを変換
     * @param inputPath 入力ファイルパス
     * @param outputPath 出力ファイルパス（nullの場合は入力と同じフォルダに出力）
     * @param options 変換オプション
     */
    suspend fun convert(
        inputPath: String, 
        outputPath: String?, 
        options: ConvertOptions
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val ffmpegPath = binaryManager.getFfmpegPath()
            if (ffmpegPath == null) {
                val error = "FFmpegがインストールされていません"
                _convertState.value = ConvertState.Error(error)
                appendLog("✗ エラー: $error\n")
                return@withContext Result.failure(Exception(error))
            }
            
            val inputFile = File(inputPath)
            if (!inputFile.exists()) {
                val error = "入力ファイルが見つかりません: $inputPath"
                _convertState.value = ConvertState.Error(error)
                appendLog("✗ エラー: $error\n")
                return@withContext Result.failure(Exception(error))
            }
            
            val ffmpegFile = File(ffmpegPath)
            
            // 実行権限を確実に設定
            Log.d(TAG, "Ensuring executable permission for FFmpeg...")
            if (!ffmpegFile.canExecute()) {
                Log.w(TAG, "FFmpeg lacks execution permission, attempting to fix...")
                try {
                    // Os.chmod を直接使用（Android 6.0+）
                    android.system.Os.chmod(ffmpegPath, 0x1ED) // 0755
                    Log.d(TAG, "Applied chmod 0755 via Os.chmod")
                } catch (e: Exception) {
                    Log.w(TAG, "Os.chmod failed, trying 0777: ${e.message}")
                    try {
                        android.system.Os.chmod(ffmpegPath, 0x1FF) // 0777
                    } catch (e2: Exception) {
                        Log.e(TAG, "Os.chmod 0777 also failed: ${e2.message}")
                    }
                }
                
                // Java APIでも試行
                ffmpegFile.setExecutable(true, false)
                ffmpegFile.setReadable(true, false)
                ffmpegFile.setWritable(true, false)
            }
            
            // 古いchmodコード
            try {
                val chmodResult = Runtime.getRuntime().exec(arrayOf("chmod", "777", ffmpegPath))
                chmodResult.waitFor()
                Log.d(TAG, "Pre-execution chmod exit code: ${chmodResult.exitValue()}")
            } catch (e: Exception) {
                Log.w(TAG, "Pre-execution chmod failed: ${e.message}")
            }
            
            Log.d(TAG, "FFmpeg executable check: canExecute=${ffmpegFile.canExecute()}, canRead=${ffmpegFile.canRead()}")
            
            // 出力パスを決定
            val outputFile = if (outputPath != null) {
                File(outputPath)
            } else {
                // 入力ファイルと同じディレクトリに、拡張子を変えて出力
                val baseName = inputFile.nameWithoutExtension
                val outputDir = inputFile.parentFile ?: getDefaultOutputDir()
                File(outputDir, "$baseName.${options.outputFormat}")
            }
            
            // 出力ディレクトリを作成
            outputFile.parentFile?.mkdirs()
            
            Log.d(TAG, "Converting: $inputPath -> ${outputFile.absolutePath}")
            
            _convertState.value = ConvertState.Converting(0, "変換準備中...")
            _logOutput.value = ""
            
            appendLog("=== 変換開始 ===\n")
            appendLog("入力: $inputPath\n")
            appendLog("出力: ${outputFile.absolutePath}\n")
            appendLog("形式: ${options.outputFormat}\n\n")
            
            // FFmpegコマンドを構築
            val args = buildFfmpegArgs(ffmpegPath, inputPath, outputFile.absolutePath, options)
            
            appendLog("コマンド: ${args.joinToString(" ")}\n\n")
            
            // Android 10+ W^X制限対応: バイナリタイプに応じた実行方法を選択
            val linker = if (android.os.Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()) {
                "/system/bin/linker64"
            } else {
                "/system/bin/linker"
            }
            
            val command: List<String>
            val executionMode: String
            
            // バイナリがPIE形式かチェック
            val isPie = isPieExecutable(ffmpegPath)
            Log.d(TAG, "Binary type check: isPIE=$isPie")
            
            // PIEバイナリの場合のみlinkerを使用
            if (isPie && File(linker).exists()) {
                command = listOf(linker) + args
                executionMode = "linker wrapper ($linker) [PIE]"
                Log.d(TAG, "Using linker wrapper execution (PIE binary)")
            } else {
                // 非PIEバイナリまたはlinkerがない場合: 直接実行を試みる
                Log.d(TAG, "Using direct execution (non-PIE or no linker)")
                command = args
                executionMode = if (isPie) "direct execution [PIE, no linker]" else "direct execution [non-PIE]"
            }
            
            appendLog("実行モード: $executionMode\n\n")
            
            val processBuilder = ProcessBuilder(command)
                .directory(context.filesDir)
                .redirectErrorStream(true)
            
            val env = processBuilder.environment()
            env["HOME"] = context.filesDir.absolutePath
            env["TMPDIR"] = context.cacheDir.absolutePath
            env["LD_LIBRARY_PATH"] = "/system/lib64:/system/lib"
            
            currentProcess = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(currentProcess!!.inputStream))
            
            var duration: Long = 0
            
            reader.forEachLine { line ->
                appendLog(line + "\n")
                Log.d(TAG, "ffmpeg: $line")
                
                // Duration: 00:03:45.67 形式から総時間を取得
                if (line.contains("Duration:")) {
                    val durationMatch = Regex("""Duration:\s*(\d{2}):(\d{2}):(\d{2})""").find(line)
                    durationMatch?.let {
                        val h = it.groupValues[1].toLongOrNull() ?: 0
                        val m = it.groupValues[2].toLongOrNull() ?: 0
                        val s = it.groupValues[3].toLongOrNull() ?: 0
                        duration = h * 3600 + m * 60 + s
                    }
                }
                
                // time=00:01:23.45 形式から現在の進捗を取得
                if (line.contains("time=") && duration > 0) {
                    val timeMatch = Regex("""time=(\d{2}):(\d{2}):(\d{2})""").find(line)
                    timeMatch?.let {
                        val h = it.groupValues[1].toLongOrNull() ?: 0
                        val m = it.groupValues[2].toLongOrNull() ?: 0
                        val s = it.groupValues[3].toLongOrNull() ?: 0
                        val current = h * 3600 + m * 60 + s
                        val progress = ((current * 100) / duration).toInt().coerceIn(0, 100)
                        _convertState.value = ConvertState.Converting(progress, "変換中... $progress%")
                    }
                }
            }
            
            val exitCode = currentProcess!!.waitFor()
            currentProcess = null
            
            Log.d(TAG, "FFmpeg exit code: $exitCode")
            
            if (exitCode == 0 && outputFile.exists()) {
                _convertState.value = ConvertState.Completed(outputFile.absolutePath)
                appendLog("\n✓ 変換完了: ${outputFile.absolutePath}\n")
                Result.success(outputFile.absolutePath)
            } else {
                val error = "変換失敗 (exit code: $exitCode)"
                _convertState.value = ConvertState.Error(error)
                appendLog("\n✗ $error\n")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Convert error", e)
            val errorMsg = e.message ?: "不明なエラー"
            _convertState.value = ConvertState.Error(errorMsg)
            appendLog("\n✗ エラー: $errorMsg\n")
            Result.failure(e)
        }
    }
    
    /**
     * 後方互換性のためのエイリアス
     */
    suspend fun convertToMp3(
        inputPath: String, 
        outputPath: String?, 
        options: ConvertOptions
    ): Result<String> = convert(inputPath, outputPath, options)
    
    private fun buildFfmpegArgs(
        ffmpegPath: String,
        inputPath: String,
        outputPath: String,
        options: ConvertOptions
    ): List<String> {
        val args = mutableListOf<String>()
        
        args.add(ffmpegPath)
        args.addAll(listOf("-y")) // 上書き確認なし
        args.addAll(listOf("-i", inputPath))
        
        when (options.outputFormat.lowercase()) {
            "mp3" -> {
                args.addAll(listOf(
                    "-vn",                          // 動画なし
                    "-acodec", "libmp3lame",        // MP3コーデック
                    "-ab", options.audioBitrate,    // ビットレート
                    "-ar", options.audioSampleRate  // サンプルレート
                ))
            }
            "m4a", "aac" -> {
                args.addAll(listOf(
                    "-vn",
                    "-acodec", "aac",
                    "-ab", options.audioBitrate,
                    "-ar", options.audioSampleRate
                ))
            }
            "wav" -> {
                args.addAll(listOf(
                    "-vn",
                    "-acodec", "pcm_s16le",
                    "-ar", options.audioSampleRate
                ))
            }
            "flac" -> {
                args.addAll(listOf(
                    "-vn",
                    "-acodec", "flac"
                ))
            }
            "mp4" -> {
                // 動画変換（webm/mkv -> mp4）
                args.addAll(listOf(
                    "-c:v", "libx264",      // H.264コーデック
                    "-preset", "medium",     // エンコード速度
                    "-crf", "23",           // 品質（0-51、低いほど高品質）
                    "-c:a", "aac",
                    "-ab", options.audioBitrate
                ))
            }
            "webm" -> {
                args.addAll(listOf(
                    "-c:v", "libvpx-vp9",
                    "-crf", "30",
                    "-b:v", "0",
                    "-c:a", "libopus",
                    "-ab", options.audioBitrate
                ))
            }
            "mkv" -> {
                // MKVはコンテナなのでコーデックはコピー可能
                args.addAll(listOf(
                    "-c:v", "copy",
                    "-c:a", "copy"
                ))
            }
            else -> {
                // デフォルト：コーデックコピー
                args.addAll(listOf("-c", "copy"))
            }
        }
        
        // 進捗表示用オプション
        args.addAll(listOf("-progress", "pipe:1", "-nostats"))
        
        args.add(outputPath)
        
        return args
    }
    
    fun cancel() {
        currentProcess?.destroyForcibly()
        currentProcess = null
        _convertState.value = ConvertState.Error("キャンセルされました")
        appendLog("\n⚠ 変換がキャンセルされました\n")
    }
    
    fun reset() {
        _convertState.value = ConvertState.Idle
        _logOutput.value = ""
    }
}
