package com.example.ui

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.DownloadEntity
import com.example.downloader.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DownloadUiState(
    val inputUrl: String = "https://www.instagram.com/reel/Dbk_8dIN-na/?igsh=MTRpOXN4M3MzNWYxbw==",
    val isExtracting: Boolean = false,
    val extractionError: String? = null,
    val extractedVideoInfo: VideoInfo? = null,
    val downloadProgress: DownloadProgress = DownloadProgress(),
    val selectedTab: Int = 0, // 0 = Downloader, 1 = History
    val activePreviewUri: String? = null
)

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val downloadDao = db.downloadDao()
    private val extractor = InstagramExtractor()
    private val downloadManagerHelper = DownloadManagerHelper(application)

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    val historyList: StateFlow<List<DownloadEntity>> = downloadDao.getAllDownloads()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var downloadJob: Job? = null

    fun onUrlChanged(newUrl: String) {
        _uiState.update {
            it.copy(
                inputUrl = newUrl,
                extractionError = null
            )
        }
    }

    fun setTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun loadSampleUrl() {
        val sample = "https://www.instagram.com/reel/Dbk_8dIN-na/?igsh=MTRpOXN4M3MzNWYxbw=="
        _uiState.update {
            it.copy(
                inputUrl = sample,
                extractionError = null
            )
        }
        extractVideoInfo(autoDownload = true)
    }

    fun pasteFromClipboard() {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val pastedText = clipData.getItemAt(0).text?.toString() ?: ""
            if (pastedText.isNotBlank()) {
                val cleanUrl = InstagramExtractor.extractUrlFromText(pastedText)
                _uiState.update { it.copy(inputUrl = cleanUrl, extractionError = null) }
            }
        }
    }

    fun handleSharedUrl(sharedText: String) {
        val cleanUrl = InstagramExtractor.extractUrlFromText(sharedText)
        if (cleanUrl.isNotBlank()) {
            _uiState.update {
                it.copy(
                    inputUrl = cleanUrl,
                    selectedTab = 0,
                    extractionError = null
                )
            }
            extractVideoInfo(autoDownload = true)
        }
    }

    fun extractVideoInfo(autoDownload: Boolean = true) {
        val rawInput = _uiState.value.inputUrl.trim()
        val url = InstagramExtractor.extractUrlFromText(rawInput)

        if (url.isBlank()) {
            _uiState.update { it.copy(extractionError = "لطفاً لینک آدرس ویدیو یا ریلز را وارد کنید") }
            return
        }

        _uiState.update {
            it.copy(
                inputUrl = url,
                isExtracting = true,
                extractionError = null,
                extractedVideoInfo = null,
                downloadProgress = DownloadProgress()
            )
        }

        viewModelScope.launch {
            val result = extractor.extractVideoInfo(url)
            if (result.isSuccess) {
                val videoInfo = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        isExtracting = false,
                        extractedVideoInfo = videoInfo,
                        extractionError = null
                    )
                }
                if (autoDownload) {
                    startDownload()
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage
                    ?: "خطا در استخراج ویدیو. لطفاً عمومی بودن آدرس را بررسی کنید."
                _uiState.update {
                    it.copy(
                        isExtracting = false,
                        extractionError = errorMsg
                    )
                }
            }
        }
    }

    fun startDownload() {
        val videoInfo = _uiState.value.extractedVideoInfo ?: return
        val targetUrl = videoInfo.videoUrl

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            downloadManagerHelper.downloadAndSaveToGallery(videoInfo, targetUrl)
                .collect { progress ->
                    _uiState.update { it.copy(downloadProgress = progress) }

                    if (progress.state == DownloadState.COMPLETED && progress.savedContentUri != null) {
                        // Save to history DB
                        saveToHistory(
                            videoInfo = videoInfo,
                            mediaUri = progress.savedContentUri,
                            filePath = progress.savedFilePath ?: "",
                            fileSize = progress.totalBytes
                        )
                        _uiState.update { it.copy(activePreviewUri = progress.savedContentUri) }
                    }
                }
        }
    }

    fun pauseDownload() {
        downloadManagerHelper.pauseDownload()
    }

    fun resumeDownload() {
        downloadManagerHelper.resumeDownload()
    }

    fun cancelDownload() {
        downloadManagerHelper.cancelDownload()
        downloadJob?.cancel()
        _uiState.update {
            it.copy(
                downloadProgress = DownloadProgress(state = DownloadState.CANCELLED)
            )
        }
    }

    fun resetState() {
        downloadJob?.cancel()
        _uiState.update {
            it.copy(
                isExtracting = false,
                extractionError = null,
                extractedVideoInfo = null,
                downloadProgress = DownloadProgress(),
                activePreviewUri = null
            )
        }
    }

    private fun saveToHistory(
        videoInfo: VideoInfo,
        mediaUri: String,
        filePath: String,
        fileSize: Long
    ) {
        viewModelScope.launch {
            val entity = DownloadEntity(
                title = videoInfo.title,
                originalUrl = videoInfo.rawUrl,
                mediaUri = mediaUri,
                filePath = filePath,
                thumbnailUrl = videoInfo.thumbnailUrl,
                fileSizeBytes = fileSize,
                durationFormatted = "00:30",
                downloadStatus = "COMPLETED"
            )
            downloadDao.insertDownload(entity)
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            downloadDao.deleteDownloadById(id)
        }
    }

    fun setPreviewUri(uri: String?) {
        _uiState.update { it.copy(activePreviewUri = uri) }
    }
}
