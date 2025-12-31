package com.example.ytdlpapp.data.config

import android.content.Context
import com.google.gson.Gson
import java.io.File

data class DownloadProfile(
    val name: String = "",
    val ytdlpOptions: String = "",
    val ffmpegOptions: String = "",
    val format: String = "best"
)

class ProfileManager(private val context: Context) {
    private val profilesDir = File(context.filesDir, "profiles")
    private val gson = Gson()

    init {
        if (!profilesDir.exists()) {
            profilesDir.mkdirs()
        }
    }

    fun saveProfile(profile: DownloadProfile): Boolean {
        return try {
            val file = File(profilesDir, "${profile.name}.json")
            val json = gson.toJson(profile)
            file.writeText(json)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun loadProfile(name: String): DownloadProfile? {
        return try {
            val file = File(profilesDir, "$name.json")
            if (file.exists()) {
                val json = file.readText()
                gson.fromJson(json, DownloadProfile::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getAllProfiles(): List<DownloadProfile> {
        return try {
            profilesDir.listFiles()?.mapNotNull { file ->
                val json = file.readText()
                gson.fromJson(json, DownloadProfile::class.java)
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteProfile(name: String): Boolean {
        return try {
            val file = File(profilesDir, "$name.json")
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    fun createDefaultProfiles() {
        val profiles = listOf(
            DownloadProfile(
                name = "Audio Extract",
                ytdlpOptions = "-x --audio-format mp3 --audio-quality 192K",
                format = "best"
            ),
            DownloadProfile(
                name = "Best Video",
                ytdlpOptions = "-f bestvideo+bestaudio/best",
                format = "bestvideo+bestaudio/best"
            ),
            DownloadProfile(
                name = "Lightweight",
                ytdlpOptions = "-f \"best[height<=480]\"",
                format = "best[height<=480]"
            )
        )

        profiles.forEach { saveProfile(it) }
    }
}
