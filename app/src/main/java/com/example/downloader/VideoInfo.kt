package com.example.downloader

data class VideoQualityOption(
    val label: String,
    val url: String,
    val resolution: String = "1080p",
    val approxSizeMb: String = ""
)

data class VideoInfo(
    val title: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val author: String,
    val platform: String,
    val rawUrl: String,
    val qualities: List<VideoQualityOption> = emptyList()
)
