package com.example.ytdlpapp.domain.usecase

/**
 * yt-dlpコマンドラインビルダー
 */
class YtdlpCommandBuilder(
    val ytdlpPath: String,
    val url: String,
    val format: String = "best",
    val outputPath: String
) {
    private val options = mutableListOf<String>(
        ytdlpPath,
        "-f", format,
        "-o", "${outputPath}/%(title)s.%(ext)s"
    )

    fun addOption(option: String) {
        options.add(option)
    }

    fun addOptions(optionsString: String) {
        if (optionsString.isNotEmpty()) {
            options.addAll(optionsString.split("\\s+".toRegex()))
        }
    }

    fun addProxy(proxyUrl: String) {
        options.add("--proxy")
        options.add(proxyUrl)
    }

    fun addPlaylistOptions() {
        options.add("-i")  // プレイリスト全体をダウンロード
        options.add("--yes-playlist")
    }

    fun addRetry(maxRetries: Int = 3) {
        options.add("--retries")
        options.add(maxRetries.toString())
    }

    fun addNoWarnings() {
        options.add("--no-warnings")
    }

    fun build(): List<String> {
        options.add(url)
        return options.toList()
    }

    fun buildString(): String {
        return build().joinToString(" ") { 
            if (it.contains(" ")) "\"$it\"" else it 
        }
    }
}
