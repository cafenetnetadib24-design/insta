package com.example.downloader

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

class DownloadManagerHelper(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val gallerySaver = GallerySaver(context)

    @Volatile
    private var isPaused = false

    @Volatile
    private var isCancelled = false

    fun pauseDownload() {
        isPaused = true
    }

    fun resumeDownload() {
        isPaused = false
    }

    fun cancelDownload() {
        isCancelled = true
    }

    private fun sanitizeUrl(url: String): String {
        var u = url.trim()
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "https://$u"
        }
        return u
            .replace(" ", "%20")
            .replace("{", "%7B")
            .replace("}", "%7D")
            .replace("|", "%7C")
            .replace("[", "%5B")
            .replace("]", "%5D")
            .replace("^", "%5E")
            .replace("`", "%60")
            .replace("\\", "/")
    }

    fun downloadAndSaveToGallery(
        videoInfo: VideoInfo,
        selectedQualityUrl: String? = null
    ): Flow<DownloadProgress> = flow {
        isPaused = false
        isCancelled = false

        val primaryTargetUrl = selectedQualityUrl ?: videoInfo.videoUrl
        if (primaryTargetUrl.isBlank()) {
            emit(
                DownloadProgress(
                    state = DownloadState.FAILED,
                    errorMessage = "آدرس ویدیویی برای دانلود وجود ندارد."
                )
            )
            return@flow
        }

        val tempFile = File(context.cacheDir, "temp_download_${System.currentTimeMillis()}.mp4")

        emit(DownloadProgress(state = DownloadState.DOWNLOADING, progressPercent = 0f))

        try {
            var response: Response? = null

            val urlsToTry = mutableListOf<String>()
            urlsToTry.add(sanitizeUrl(primaryTargetUrl))
            if (videoInfo.videoUrl.isNotBlank() && videoInfo.videoUrl != primaryTargetUrl) {
                urlsToTry.add(sanitizeUrl(videoInfo.videoUrl))
            }

            targetLoop@ for (targetUrl in urlsToTry) {
                if (response != null && response.isSuccessful && response.body != null) break@targetLoop

                val headersToTry = listOf(
                    mapOf(
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                        "Accept" to "*/*"
                    ),
                    mapOf(
                        "User-Agent" to "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1",
                        "Referer" to "https://www.instagram.com/",
                        "Accept" to "*/*"
                    ),
                    mapOf(
                        "User-Agent" to "Instagram 318.0.0.26.109 Android (33/13; 420dpi; 1080x2220; Samsung; SM-G998B)",
                        "Accept" to "*/*"
                    ),
                    emptyMap()
                )

                for (headers in headersToTry) {
                    try {
                        val reqBuilder = Request.Builder().url(targetUrl)
                        headers.forEach { (k, v) -> reqBuilder.header(k, v) }
                        val res = client.newCall(reqBuilder.build()).execute()
                        if (res.isSuccessful && res.body != null) {
                            response = res
                            break@targetLoop
                        } else {
                            res.close()
                        }
                    } catch (e: Exception) {
                        // try next header configuration or next target URL
                    }
                }
            }

            if (response == null || !response.isSuccessful) {
                emit(
                    DownloadProgress(
                        state = DownloadState.FAILED,
                        errorMessage = "ارتباط با سرور اینستاگرام برقرار نشد. آدرس دانلود منقضی یا محدود شده است."
                    )
                )
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(
                    DownloadProgress(
                        state = DownloadState.FAILED,
                        errorMessage = "محتوای فایل ویدیو دریافت نشد."
                    )
                )
                return@flow
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            val buffer = ByteArray(64 * 1024)

            val inputStream: InputStream = body.byteStream()
            val outputStream = FileOutputStream(tempFile)

            var lastTime = System.currentTimeMillis()
            var lastDownloadedBytes = 0L
            var currentSpeedBps = 0L

            outputStream.use { out ->
                inputStream.use { input ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isCancelled || !coroutineContext.isActive) {
                            emit(DownloadProgress(state = DownloadState.CANCELLED))
                            tempFile.delete()
                            return@flow
                        }

                        while (isPaused) {
                            emit(
                                DownloadProgress(
                                    state = DownloadState.PAUSED,
                                    progressPercent = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes) * 100f else 0f,
                                    bytesDownloaded = downloadedBytes,
                                    totalBytes = totalBytes,
                                    speedBytesPerSec = 0L
                                )
                            )
                            kotlinx.coroutines.delay(500)
                            if (isCancelled || !coroutineContext.isActive) {
                                emit(DownloadProgress(state = DownloadState.CANCELLED))
                                tempFile.delete()
                                return@flow
                            }
                        }

                        out.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val currentTime = System.currentTimeMillis()
                        val timeDiff = currentTime - lastTime
                        if (timeDiff >= 300) {
                            val bytesDiff = downloadedBytes - lastDownloadedBytes
                            currentSpeedBps = if (timeDiff > 0) (bytesDiff * 1000) / timeDiff else 0L

                            val percent = if (totalBytes > 0) {
                                (downloadedBytes.toFloat() / totalBytes) * 100f
                            } else {
                                (downloadedBytes.toFloat() / (10 * 1024 * 1024)).coerceAtMost(0.9f) * 100f
                            }

                            val remainingBytes = if (totalBytes > downloadedBytes) totalBytes - downloadedBytes else 0L
                            val etaSec = if (currentSpeedBps > 0 && remainingBytes > 0) {
                                remainingBytes / currentSpeedBps
                            } else 0L

                            emit(
                                DownloadProgress(
                                    state = DownloadState.DOWNLOADING,
                                    progressPercent = percent.coerceIn(0f, 99f),
                                    bytesDownloaded = downloadedBytes,
                                    totalBytes = if (totalBytes > 0) totalBytes else downloadedBytes,
                                    speedBytesPerSec = currentSpeedBps,
                                    etaSeconds = etaSec
                                )
                            )

                            lastTime = currentTime
                            lastDownloadedBytes = downloadedBytes
                        }
                    }
                }
            }

            // Finished downloading temp file, now save to public Gallery
            emit(
                DownloadProgress(
                    state = DownloadState.SAVING,
                    progressPercent = 99f,
                    bytesDownloaded = downloadedBytes,
                    totalBytes = if (totalBytes > 0) totalBytes else downloadedBytes
                )
            )

            val galleryResult = gallerySaver.saveVideoToGallery(tempFile, videoInfo.title)
            tempFile.delete()

            if (galleryResult.isSuccess) {
                val (galleryUri, savedFilePath) = galleryResult.getOrThrow()
                emit(
                    DownloadProgress(
                        state = DownloadState.COMPLETED,
                        progressPercent = 100f,
                        bytesDownloaded = downloadedBytes,
                        totalBytes = if (totalBytes > 0) totalBytes else downloadedBytes,
                        savedFilePath = savedFilePath,
                        savedContentUri = galleryUri.toString()
                    )
                )
            } else {
                val error = galleryResult.exceptionOrNull()?.message ?: "ذخیره در گالری ناموفق بود"
                emit(
                    DownloadProgress(
                        state = DownloadState.FAILED,
                        errorMessage = error
                    )
                )
            }

        } catch (e: Exception) {
            emit(
                DownloadProgress(
                    state = DownloadState.FAILED,
                    errorMessage = e.localizedMessage ?: "خطای ناخواسته در دانلود ویدیو"
                )
            )
        }
    }.flowOn(Dispatchers.IO)
}
