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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.painterResource
import com.example.R
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

    val dynamicColorScheme = if (uiState.isDarkTheme) {
        darkColorScheme(
            primary = Color(0xFFFF9800),
            background = Color(0xFF0F172A),
            surface = Color(0xFF1E293B),
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFFF9800),
            background = SleekBackground,
            surface = Color.White,
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A)
        )
    }

    MaterialTheme(colorScheme = dynamicColorScheme) {
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
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF833AB4),
                                                Color(0xFFFD1D1D),
                                                Color(0xFFF77737)
                                            )
                                        )
                                    )
                                    .shadow(6.dp, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                    contentDescription = "آیکون برنامه",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "دانلودگر اینستاگرام",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
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
                        // Ad Trigger Button
                        IconButton(
                            onClick = { viewModel.triggerAdManually() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFFF9800).copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "تبلیغات ویژه",
                                tint = Color(0xFFFF9800)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // Theme Switcher Button
                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Icon(
                                imageVector = if (uiState.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "تغییر تم",
                                tint = if (uiState.isDarkTheme) Color(0xFFFFB74D) else Color(0xFF1E293B)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // Reset Button
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
                                onExtractClicked = { viewModel.extractVideoInfo(autoDownload = true) }
                            )

                            // Extracted Video Preview Card with Custom Title Option
                            AnimatedVisibility(
                                visible = uiState.extractedVideoInfo != null,
                                enter = fadeIn() + expandVertically()
                            ) {
                                uiState.extractedVideoInfo?.let { videoInfo ->
                                    VideoInfoCard(
                                        videoInfo = videoInfo,
                                        customTitle = uiState.customTitle,
                                        onTitleChanged = { viewModel.onCustomTitleChanged(it) },
                                        onStartDownload = { title -> viewModel.startDownload(title) }
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

                            // Banner Ad Section
                            uiState.bannerAd?.let { banner ->
                                BannerAdCard(adItem = banner)
                            }

                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }

                    1 -> {
                        // History / Gallery Screen
                        HistoryList(
                            historyItems = historyItems,
                            searchQuery = uiState.gallerySearchQuery,
                            onSearchQueryChanged = { viewModel.onGallerySearchQueryChanged(it) },
                            layoutMode = uiState.galleryLayoutMode,
                            onLayoutModeChanged = { viewModel.setGalleryLayoutMode(it) },
                            onPlayMedia = { viewModel.setPreviewUri(it) },
                            onRenameItem = { id, title -> viewModel.renameHistoryItem(id, title) },
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

        // Full Screen Ad Dialog
        uiState.activeAd?.let { ad ->
            com.example.ui.components.FullScreenAdDialog(
                adItem = ad,
                onDismiss = { viewModel.dismissAd() }
            )
        }
    }
}

@Composable
private fun SleekInputCard(
    inputUrl: String,
    isExtracting: Boolean,
    errorMessage: String?,
    onUrlChanged: (String) -> Unit,
    onPasteClicked: () -> Unit,
    onExtractClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color(0x1A000000))
            .testTag("url_input_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
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
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آدرس ریلز یا لینک ویدیو:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Single Unified Input Field (Always Left Aligned LTR)
            OutlinedTextField(
                value = inputUrl,
                onValueChange = onUrlChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("url_input_field"),
                textStyle = TextStyle(
                    textAlign = TextAlign.Left,
                    textDirection = TextDirection.Ltr,
                    color = Color.Black,
                    fontSize = 14.sp
                ),
                placeholder = {
                    Text(
                        text = "آدرس ریلز اینستاگرام (مثلاً instagram.com/reel/...)",
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                maxLines = 3,
                minLines = 1,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFFFF9800),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
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
                    containerColor = Color(0xFFFF9800),
                    disabledContainerColor = Color(0xFFFF9800).copy(alpha = 0.5f)
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
        color = Color(0xFF1E293B),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 10.dp),
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
            tint = if (isSelected) Color(0xFFFF9800) else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
