package com.example.ytdlpapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.example.ytdlpapp.domain.model.AppSettings

class SettingsRepository(context: Context) {
    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    fun getDownloadFolder(): String {
        return sharedPreferences.getString("download_folder", "") ?: ""
    }

    fun setDownloadFolder(path: String) {
        sharedPreferences.edit().putString("download_folder", path).apply()
    }

    fun isAutoUpdateBinary(): Boolean {
        return sharedPreferences.getBoolean("auto_update_binary", true)
    }

    fun setAutoUpdateBinary(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("auto_update_binary", enabled).apply()
    }

    fun getYtdlpOptions(): String {
        return sharedPreferences.getString("ytdlp_options", "") ?: ""
    }

    fun setYtdlpOptions(options: String) {
        sharedPreferences.edit().putString("ytdlp_options", options).apply()
    }

    fun getFfmpegOptions(): String {
        return sharedPreferences.getString("ffmpeg_options", "") ?: ""
    }

    fun setFfmpegOptions(options: String) {
        sharedPreferences.edit().putString("ffmpeg_options", options).apply()
    }

    fun isShowDebugLog(): Boolean {
        return sharedPreferences.getBoolean("show_debug_log", false)
    }

    fun setShowDebugLog(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("show_debug_log", enabled).apply()
    }

    fun getSettings(): AppSettings {
        return AppSettings(
            downloadFolder = getDownloadFolder(),
            autoUpdateBinary = isAutoUpdateBinary(),
            ytdlpOptions = getYtdlpOptions(),
            ffmpegOptions = getFfmpegOptions(),
            showDebugLog = isShowDebugLog()
        )
    }
}
