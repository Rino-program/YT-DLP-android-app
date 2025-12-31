package com.example.ytdlpapp.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CustomProgressBar(
    progress: Int, // 0-100
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(4.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress / 100f)
                .height(8.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(4.dp)
                )
        )
    }
}

@Composable
fun ComposeLinearProgressIndicator(
    progress: Float, // 0f-1f
    modifier: Modifier = Modifier
) {
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier.fillMaxWidth(),
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        color = MaterialTheme.colorScheme.primary
    )
}
