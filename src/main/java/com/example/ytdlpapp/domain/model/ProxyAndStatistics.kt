package com.example.ytdlpapp.domain.model

data class ProxySettings(
    val enabled: Boolean = false,
    val protocol: String = "http", // http, https, socks4, socks5
    val host: String = "",
    val port: Int = 0,
    val username: String? = null,
    val password: String? = null
) {
    fun toProxyUrl(): String? {
        if (!enabled || host.isEmpty()) return null
        val auth = if (username != null && password != null) {
            "$username:$password@"
        } else {
            ""
        }
        return "$protocol://$auth$host:$port"
    }
}

data class DownloadStatistics(
    val totalDownloads: Int = 0,
    val successfulDownloads: Int = 0,
    val failedDownloads: Int = 0,
    val totalBytesDownloaded: Long = 0L,
    val totalDuration: Long = 0L,
    val averageSpeed: Float = 0f,
    val successRate: Float = if (totalDownloads > 0) {
        successfulDownloads.toFloat() / totalDownloads * 100
    } else {
        0f
    }
)
