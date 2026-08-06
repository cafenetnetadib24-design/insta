package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.downloader.DownloadState
import com.example.ui.DownloadViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: DownloadViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historyItems by viewModel.historyList.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SleekPrimary,
                            shadowElevation = 6.dp
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "InstaFetch",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "دانلود مستقیم ریلز و ویدیوهای اینستاگرام",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(
                        onClick = { viewModel.resetState() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "بازنشانی",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        },
        bottomBar = {
            SleekBottomNavigation(
                selectedTab = uiState.selectedTab,
                onTabSelected = { viewModel.setTab(it) },
                historyCount = historyItems.size
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            when (uiState.selectedTab) {
                0 -> {
                    // Downloader Screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Unified Input Card
                        SleekInputCard(
                            inputUrl = uiState.inputUrl,
                            isExtracting = uiState.isExtracting,
                            errorMessage = uiState.extractionError,
                            onUrlChanged = { viewModel.onUrlChanged(it) },
                            onPasteClicked = { viewModel.pasteFromClipboard() },
                            onSampleClicked = { viewModel.loadSampleUrl() },
                            onExtractClicked = { viewModel.extractVideoInfo(autoDownload = true) }
                        )

                        // Extracted Video Preview Card
                        AnimatedVisibility(
                            visible = uiState.extractedVideoInfo != null,
                            enter = fadeIn() + expandVertically()
                        ) {
                            uiState.extractedVideoInfo?.let { videoInfo ->
                                VideoInfoCard(
                                    videoInfo = videoInfo,
                                    onStartDownload = { viewModel.startDownload() }
                                )
                            }
                        }

                        // Advanced Progress Card
                        AnimatedVisibility(
                            visible = uiState.downloadProgress.state != DownloadState.IDLE,
                            enter = fadeIn() + expandVertically()
                        ) {
                            AdvancedProgressBar(
                                progress = uiState.downloadProgress,
                                onPause = { viewModel.pauseDownload() },
                                onResume = { viewModel.resumeDownload() },
                                onCancel = { viewModel.cancelDownload() },
                                onOpenGallery = {
                                    uiState.downloadProgress.savedContentUri?.let { uri ->
                                        viewModel.setPreviewUri(uri)
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }

                1 -> {
                    // History Screen
                    HistoryList(
                        historyItems = historyItems,
                        onPlayMedia = { viewModel.setPreviewUri(it) },
                        onDeleteItem = { viewModel.deleteHistoryItem(it) }
                    )
                }
            }
        }
    }

    // Video Preview Modal Dialog
    uiState.activePreviewUri?.let { uri ->
        VideoPreviewDialog(
            videoUriString = uri,
            onDismiss = { viewModel.setPreviewUri(null) }
        )
    }
}

@Composable
private fun SleekInputCard(
    inputUrl: String,
    isExtracting: Boolean,
    errorMessage: String?,
    onUrlChanged: (String) -> Unit,
    onPasteClicked: () -> Unit,
    onSampleClicked: () -> Unit,
    onExtractClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color(0x1A000000))
            .testTag("url_input_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Input Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آدرس ریلز یا لینک ویدیو:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                TextButton(
                    onClick = onSampleClicked,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = SleekPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "لینک نمونه",
                        color = SleekPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Single Unified Input Field
            OutlinedTextField(
                value = inputUrl,
                onValueChange = onUrlChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("url_input_field"),
                placeholder = {
                    Text(
                        text = "آدرس ریلز اینستاگرام (مثلاً instagram.com/reel/...)",
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                },
                maxLines = 3,
                minLines = 1,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SleekBackground.copy(alpha = 0.7f),
                    unfocusedContainerColor = SleekBackground.copy(alpha = 0.7f),
                    focusedBorderColor = SleekPrimary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (inputUrl.isNotBlank()) {
                            IconButton(onClick = { onUrlChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "پاک کردن", tint = TextSecondary)
                            }
                        }
                        IconButton(onClick = onPasteClicked) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "چسباندن", tint = SleekPrimary)
                        }
                    }
                }
            )

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = StatusError,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Action Button
            Button(
                onClick = onExtractClicked,
                enabled = !isExtracting && inputUrl.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("extract_video_btn"),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SleekDarkBtn,
                    disabledContainerColor = SleekDarkBtn.copy(alpha = 0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                if (isExtracting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("در حال بررسی و دانلود اتوماتیک...", color = Color.White, fontSize = 14.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "بررسی و دانلود مستقیم ویدیو",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SleekBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    historyCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(20.dp, spotColor = Color(0x1A000000)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationTabItem(
                icon = Icons.Default.Home,
                label = "صفحه اصلی",
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )

            NavigationTabItem(
                icon = Icons.Default.PermMedia,
                label = "گالری ($historyCount)",
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )
        }
    }
}

@Composable
private fun NavigationTabItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) SleekPrimary else TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = if (isSelected) SleekPrimary else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
