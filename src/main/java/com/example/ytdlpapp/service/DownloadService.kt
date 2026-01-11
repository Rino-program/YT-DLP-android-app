package com.example.ytdlpapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.ytdlpapp.MainActivity
import com.example.ytdlpapp.R
import com.example.ytdlpapp.core.BinaryManager
import com.example.ytdlpapp.core.DownloadManager
import com.example.ytdlpapp.model.DownloadOptions
import com.example.ytdlpapp.model.DownloadState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * バックグラウンドダウンロードサービス
 */
class DownloadService : Service() {
    
    companion object {
        const val CHANNEL_ID = "download_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_DOWNLOAD = "action_download"
        const val ACTION_CANCEL = "action_cancel"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_OPTIONS = "extra_options"
        
        fun startDownload(context: Context, url: String, options: DownloadOptions) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_DOWNLOAD
                putExtra(EXTRA_URL, url)
                putExtra("format", options.format.name)
                putExtra("outputDir", options.outputDir)
                putExtra("excludeAv1", options.excludeAv1)
                putExtra("playlistPadding", options.playlistPadding)
                putExtra("customOptions", options.customOptions)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun cancel(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null
    
    private lateinit var binaryManager: BinaryManager
    private lateinit var downloadManager: DownloadManager
    private lateinit var notificationManager: NotificationManager
    
    inner class LocalBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
        fun getDownloadManager(): DownloadManager = downloadManager
    }
    
    private val binder = LocalBinder()
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        binaryManager = BinaryManager(this)
        downloadManager = DownloadManager(this, binaryManager)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 状態変化を監視して通知を更新
        serviceScope.launch {
            downloadManager.downloadState.collectLatest { state ->
                updateNotification(state)
                
                // 完了またはエラー時は少し待ってからサービス停止
                if (state is DownloadState.Completed || state is DownloadState.Error) {
                    delay(5000)
                    stopSelf()
                }
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DOWNLOAD -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val options = DownloadOptions(
                    format = try {
                        com.example.ytdlpapp.model.OutputFormat.valueOf(
                            intent.getStringExtra("format") ?: "MP4"
                        )
                    } catch (e: Exception) {
                        com.example.ytdlpapp.model.OutputFormat.MP4
                    },
                    audioOnly = intent.getStringExtra("format")?.let { 
                        it == "MP3" || it == "M4A" 
                    } ?: false,
                    outputDir = intent.getStringExtra("outputDir") ?: "",
                    excludeAv1 = intent.getBooleanExtra("excludeAv1", true),
                    playlistPadding = intent.getIntExtra("playlistPadding", 2),
                    customOptions = intent.getStringExtra("customOptions") ?: ""
                )
                
                startForeground(NOTIFICATION_ID, createNotification("ダウンロード準備中..."))
                
                downloadJob?.cancel()
                downloadJob = serviceScope.launch {
                    downloadManager.download(url, options)
                }
            }
            
            ACTION_CANCEL -> {
                downloadManager.cancel()
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onDestroy() {
        super.onDestroy()
        downloadJob?.cancel()
        serviceScope.cancel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ダウンロード",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "ダウンロードの進捗を表示"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(content: String, progress: Int = -1): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val cancelIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, DownloadService::class.java).apply { action = ACTION_CANCEL },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("yt-dlp ダウンロード")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(progress >= 0)
            .apply {
                if (progress >= 0) {
                    setProgress(100, progress, false)
                    addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "キャンセル",
                        cancelIntent
                    )
                }
            }
            .build()
    }
    
    private fun updateNotification(state: DownloadState) {
        val notification = when (state) {
            is DownloadState.Idle -> createNotification("待機中...")
            is DownloadState.Downloading -> {
                val text = "ダウンロード中 ${state.progress}%"
                createNotification(text, state.progress)
            }
            is DownloadState.Processing -> createNotification(state.message, 0)
            is DownloadState.Completed -> {
                NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("ダウンロード完了")
                    .setContentText("ファイルを保存しました")
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setAutoCancel(true)
                    .build()
            }
            is DownloadState.Error -> {
                NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("ダウンロードエラー")
                    .setContentText(state.message.take(100))
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setAutoCancel(true)
                    .build()
            }
        }
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
