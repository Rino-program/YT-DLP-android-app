package com.example.ytdlpapp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ytdlpapp.domain.usecase.BinaryManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BinaryManagerTest {
    
    private lateinit var context: Context
    private lateinit var binaryManager: BinaryManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        binaryManager = BinaryManager(context)
    }

    @Test
    fun testBinaryDirectoryCreation() {
        val binariesDir = binaryManager.getBinariesDir()
        assert(binariesDir.exists())
    }

    @Test
    fun testIsBinaryInstalled() {
        val isInstalled = binaryManager.isBinaryInstalled("yt-dlp")
        // インストール前は false であることを期待
        assert(!isInstalled)
    }

    @Test
    fun testGetBinaryPath() {
        val path = binaryManager.getBinaryPath("yt-dlp")
        // インストール前は null であることを期待
        assert(path == null)
    }
}
