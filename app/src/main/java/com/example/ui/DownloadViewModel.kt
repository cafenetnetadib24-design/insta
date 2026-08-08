package com.example.ui

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdItem
import com.example.data.db.AppDatabase
import com.example.data.db.DownloadEntity
import com.example.data.db.FolderEntity
import com.example.data.db.FolderDao
import com.example.downloader.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DownloadUiState(
    val inputUrl: String = "",
    val isExtracting: Boolean = false,
    val extractionError: String? = null,
    val extractedVideoInfo: VideoInfo? = null,
    val customTitle: String = "",
    val downloadProgress: DownloadProgress = DownloadProgress(),
    val selectedTab: Int = 0, // 0 = Downloader, 1 = History/Gallery
    val activePreviewUri: String? = null,
    val isDarkTheme: Boolean = false,
    val galleryLayoutMode: Int = 1, // 0 = List, 1 = Grid (2 cols)
    val gallerySearchQuery: String = "",
    val galleryFilterTab: Int = 0, // 0 = All, 1 = Favorites, 2 = Folders
    val selectedFolderId: Long? = null, // null when not inside a specific folder
    val showTutorialDialog: Boolean = false,
    val activeAd: AdItem? = null,
    val bannerAd: AdItem? = null
)

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val downloadDao = db.downloadDao()
    private val folderDao = db.folderDao()
    private val extractor = InstagramExtractor()
    private val downloadManagerHelper = DownloadManagerHelper(application)
    private val adManager = AdManager(application)
    private val prefs = application.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    init {
        checkFirstLaunchTutorial()
        startAdTimer()
    }

    private fun checkFirstLaunchTutorial() {
        val hasSeenTutorial = prefs.getBoolean("has_seen_tutorial", false)
        if (!hasSeenTutorial) {
            _uiState.update { it.copy(showTutorialDialog = true) }
            prefs.edit().putBoolean("has_seen_tutorial", true).apply()
        }
    }

    fun openTutorial() {
        _uiState.update { it.copy(showTutorialDialog = true) }
    }

    fun dismissTutorial() {
        _uiState.update { it.copy(showTutorialDialog = false) }
    }

    private fun startAdTimer() {
        viewModelScope.launch {
            try {
                // Initial fetch & pre-cache images (online update or offline cache)
                val ads = adManager.fetchAndCacheAds()
                if (ads.isNotEmpty()) {
                    val banner = ads.random()
                    _uiState.update { it.copy(bannerAd = banner) }

                    // Show full-screen ad after short initial delay (1.5 seconds)
                    delay(1500)
                    val fullAd = ads.random()
                    _uiState.update { it.copy(activeAd = fullAd) }
                }

                // Recurring 4-minute (240 seconds) ad rotation schedule
                val fourMinutesMs = 4 * 60 * 1000L
                while (isActive) {
                    delay(fourMinutesMs)
                    val currentAds = adManager.fetchAndCacheAds()
                    if (currentAds.isNotEmpty()) {
                        val randomAd = currentAds.random()
                        val randomBanner = currentAds.random()
                        _uiState.update { it.copy(activeAd = randomAd, bannerAd = randomBanner) }
                    }
                }
            } catch (e: Exception) {
                Log.e("DownloadViewModel", "Error in startAdTimer: ${e.message}")
            }
        }
    }

    fun triggerAdManually() {
        val randomAd = adManager.getRandomAd()
        if (randomAd != null) {
            _uiState.update { it.copy(activeAd = randomAd) }
        } else {
            viewModelScope.launch {
                val ads = adManager.fetchAndCacheAds()
                if (ads.isNotEmpty()) {
                    _uiState.update { it.copy(activeAd = ads.random(), bannerAd = ads.random()) }
                }
            }
        }
    }

    fun dismissAd() {
        _uiState.update { it.copy(activeAd = null) }
    }

    val historyList: StateFlow<List<DownloadEntity>> = downloadDao.getAllDownloads()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val foldersList: StateFlow<List<FolderEntity>> = folderDao.getAllFolders()
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

    fun onCustomTitleChanged(newTitle: String) {
        _uiState.update { it.copy(customTitle = newTitle) }
    }

    fun toggleTheme() {
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun setGalleryLayoutMode(mode: Int) {
        _uiState.update { it.copy(galleryLayoutMode = mode) }
    }

    fun onGallerySearchQueryChanged(query: String) {
        _uiState.update { it.copy(gallerySearchQuery = query) }
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
                customTitle = "",
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
                        customTitle = videoInfo.title,
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

    fun startDownload(overrideTitle: String? = null) {
        val rawVideoInfo = _uiState.value.extractedVideoInfo ?: return
        val finalTitle = overrideTitle?.ifBlank { null }
            ?: _uiState.value.customTitle.ifBlank { null }
            ?: rawVideoInfo.title
        val videoInfo = rawVideoInfo.copy(title = finalTitle)
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
                        val completionAd = adManager.getRandomAd()
                        _uiState.update { 
                            it.copy(
                                activePreviewUri = progress.savedContentUri,
                                activeAd = completionAd ?: it.activeAd
                            ) 
                        }
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
                customTitle = "",
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

    fun setGalleryFilterTab(tab: Int) {
        _uiState.update { it.copy(galleryFilterTab = tab, selectedFolderId = null) }
    }

    fun setSelectedFolderId(folderId: Long?) {
        _uiState.update { it.copy(selectedFolderId = folderId) }
    }

    fun toggleFavorite(id: Long, currentStatus: Boolean) {
        viewModelScope.launch {
            downloadDao.updateFavoriteStatus(id, !currentStatus)
        }
    }

    fun moveFileToFolder(downloadId: Long, folderId: Long?) {
        viewModelScope.launch {
            downloadDao.updateFolderId(downloadId, folderId)
        }
    }

    fun createFolder(name: String, onFolderCreated: ((Long) -> Unit)? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newId = folderDao.insertFolder(FolderEntity(name = name.trim()))
            onFolderCreated?.invoke(newId)
        }
    }

    fun renameFolder(id: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            folderDao.updateFolderName(id, newName.trim())
        }
    }

    fun deleteFolder(id: Long) {
        viewModelScope.launch {
            downloadDao.clearFolderIdFromDownloads(id)
            folderDao.deleteFolderById(id)
            if (_uiState.value.selectedFolderId == id) {
                _uiState.update { it.copy(selectedFolderId = null) }
            }
        }
    }

    fun renameHistoryItem(id: Long, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            downloadDao.updateTitle(id, newTitle)
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
