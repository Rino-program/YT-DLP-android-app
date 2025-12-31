package com.example.ytdlpapp

import com.example.ytdlpapp.domain.model.DownloadInfo
import com.example.ytdlpapp.domain.model.DownloadStatus
import org.junit.Test
import org.junit.Assert.*

class DownloadInfoTest {
    
    @Test
    fun testDownloadInfoCreation() {
        val downloadInfo = DownloadInfo(
            url = "https://example.com/video",
            title = "Test Video",
            format = "best"
        )
        
        assertEquals("https://example.com/video", downloadInfo.url)
        assertEquals("Test Video", downloadInfo.title)
        assertEquals(DownloadStatus.PENDING, downloadInfo.status)
    }

    @Test
    fun testDownloadInfoStatusUpdate() {
        val downloadInfo = DownloadInfo(
            url = "https://example.com/video",
            status = DownloadStatus.DOWNLOADING,
            progress = 50
        )
        
        assertEquals(DownloadStatus.DOWNLOADING, downloadInfo.status)
        assertEquals(50, downloadInfo.progress)
    }

    @Test
    fun testDownloadInfoError() {
        val downloadInfo = DownloadInfo(
            url = "https://example.com/video",
            status = DownloadStatus.FAILED,
            errorMessage = "Network error"
        )
        
        assertEquals(DownloadStatus.FAILED, downloadInfo.status)
        assertEquals("Network error", downloadInfo.errorMessage)
    }
}
