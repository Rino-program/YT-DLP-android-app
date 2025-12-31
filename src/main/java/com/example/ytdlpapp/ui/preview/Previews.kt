package com.example.ytdlpapp.ui.preview

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.ytdlpapp.domain.model.DownloadInfo
import com.example.ytdlpapp.domain.model.DownloadStatus
import com.example.ytdlpapp.ui.screen.DownloadHistoryItem
import com.example.ytdlpapp.ui.theme.YtDlpAppTheme

@Preview(showBackground = true)
@Composable
fun PreviewDownloadHistoryItem() {
    YtDlpAppTheme {
        Surface {
            DownloadHistoryItem(
                downloadInfo = DownloadInfo(
                    id = 1,
                    url = "https://www.youtube.com/watch?v=example",
                    title = "Example Video",
                    format = "best",
                    outputPath = "/storage/emulated/0/Downloads",
                    status = DownloadStatus.COMPLETED,
                    progress = 100
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDownloadHistoryItemFailed() {
    YtDlpAppTheme {
        Surface {
            DownloadHistoryItem(
                downloadInfo = DownloadInfo(
                    id = 2,
                    url = "https://example.com/video",
                    title = "Failed Download",
                    format = "best",
                    outputPath = "/storage/emulated/0/Downloads",
                    status = DownloadStatus.FAILED,
                    progress = 45,
                    errorMessage = "Network timeout"
                )
            )
        }
    }
}
