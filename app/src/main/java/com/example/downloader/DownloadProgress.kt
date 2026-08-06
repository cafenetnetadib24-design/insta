package com.example.downloader

enum class DownloadState {
    IDLE,
    EXTRACTING,
    DOWNLOADING,
    PAUSED,
    SAVING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadProgress(
    val state: DownloadState = DownloadState.IDLE,
    val progressPercent: Float = 0f, // 0.0 to 100.0
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val errorMessage: String? = null,
    val savedFilePath: String? = null,
    val savedContentUri: String? = null
) {
    val speedFormatted: String
        get() {
            if (speedBytesPerSec <= 0) return "0 KB/s"
            val kb = speedBytesPerSec / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format("%.1f MB/s", mb)
            } else {
                String.format("%.0f KB/s", kb)
            }
        }

    val bytesFormatted: String
        get() {
            val downMb = bytesDownloaded / (1024.0 * 1024.0)
            val totalMb = totalBytes / (1024.0 * 1024.0)
            return if (totalBytes > 0) {
                String.format("%.1f MB / %.1f MB", downMb, totalMb)
            } else {
                String.format("%.1f MB", downMb)
            }
        }

    val etaFormatted: String
        get() {
            if (etaSeconds <= 0 || state != DownloadState.DOWNLOADING) return "--:--"
            val minutes = etaSeconds / 60
            val seconds = etaSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}
