package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.db.DownloadEntity
import com.example.data.db.FolderEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryList(
    historyItems: List<DownloadEntity>,
    foldersList: List<FolderEntity>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    layoutMode: Int,
    onLayoutModeChanged: (Int) -> Unit,
    filterTab: Int, // 0 = All, 1 = Favorites, 2 = Folders
    onFilterTabChanged: (Int) -> Unit,
    selectedFolderId: Long?,
    onFolderSelected: (Long?) -> Unit,
    onPlayMedia: (String) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    onMoveToFolder: (Long, Long?) -> Unit,
    onCreateFolder: (String, ((Long) -> Unit)?) -> Unit,
    onRenameFolder: (Long, String) -> Unit,
    onDeleteFolder: (Long) -> Unit,
    onRenameItem: (Long, String) -> Unit,
    onDeleteItem: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var itemToRename by remember { mutableStateOf<DownloadEntity?>(null) }
    var itemToMove by remember { mutableStateOf<DownloadEntity?>(null) }
    var folderToRename by remember { mutableStateOf<FolderEntity?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    // Filter items based on current tab & folder selection
    val baseDisplayItems = remember(historyItems, filterTab, selectedFolderId) {
        when {
            filterTab == 1 -> historyItems.filter { it.isFavorite }
            filterTab == 2 && selectedFolderId != null -> historyItems.filter { it.folderId == selectedFolderId }
            else -> historyItems
        }
    }

    val filteredItems = remember(baseDisplayItems, searchQuery) {
        if (searchQuery.isBlank()) {
            baseDisplayItems
        } else {
            baseDisplayItems.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    val currentSelectedFolder = remember(foldersList, selectedFolderId) {
        foldersList.find { it.id == selectedFolderId }
    }

    // Rename Video Dialog
    itemToRename?.let { item ->
        var editedTitle by remember(item) { mutableStateOf(item.title) }
        AlertDialog(
            onDismissRequest = { itemToRename = null },
            title = {
                Text(
                    text = "تغییر نام ویدیو",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "نام جدید را وارد کنید:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFFFF9800),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedTitle.isNotBlank()) {
                            onRenameItem(item.id, editedTitle.trim())
                        }
                        itemToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text("ذخیره", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRename = null }) {
                    Text("انصراف", color = TextSecondary)
                }
            }
        )
    }

    // Move to Folder Dialog
    itemToMove?.let { item ->
        var showInlineCreateFolder by remember { mutableStateOf(false) }
        var newFolderNameInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { itemToMove = null },
            title = {
                Text(
                    text = "انتقال به پوشه",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "پوشه مقصد را برای «${item.title.take(25)}...» انتخاب کنید:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    // Option: No Folder (Root)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (item.folderId == null) Color(0xFFFF9800).copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                        border = if (item.folderId == null) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onMoveToFolder(item.id, null)
                                itemToMove = null
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.FolderOff, contentDescription = null, tint = Color.Gray)
                            Text("بدون پوشه (اصلی)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }

                    // Created Folders List
                    if (foldersList.isNotEmpty()) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                        ) {
                            items(foldersList, key = { it.id }) { folder ->
                                val isCurrent = item.folderId == folder.id
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCurrent) Color(0xFFFF9800).copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                                    border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800)) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onMoveToFolder(item.id, folder.id)
                                            itemToMove = null
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFFF9800))
                                        Text(
                                            text = folder.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            modifier = Modifier.weight(1f)
                                        )
                                        val count = historyItems.count { it.folderId == folder.id }
                                        Text("$count فایل", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // Create New Folder Section inside Move Dialog
                    if (!showInlineCreateFolder) {
                        OutlinedButton(
                            onClick = { showInlineCreateFolder = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800))
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = Color(0xFFFF9800))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ایجاد پوشه جدید", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newFolderNameInput,
                                onValueChange = { newFolderNameInput = it },
                                placeholder = { Text("نام پوشه جدید...", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Color(0xFFFF9800)
                                )
                            )
                            Button(
                                onClick = {
                                    if (newFolderNameInput.isNotBlank()) {
                                        onCreateFolder(newFolderNameInput.trim()) { createdFolderId ->
                                            onMoveToFolder(item.id, createdFolderId)
                                            itemToMove = null
                                        }
                                    }
                                },
                                enabled = newFolderNameInput.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                            ) {
                                Text("ایجاد و انتقال به این پوشه", color = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { itemToMove = null }) {
                    Text("انصراف", color = TextSecondary)
                }
            }
        )
    }

    // Create New Folder Standalone Dialog
    if (showCreateFolderDialog) {
        var folderNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = {
                Text("ایجاد پوشه جدید", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("نام پوشه را وارد کنید:", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = folderNameInput,
                        onValueChange = { folderNameInput = it },
                        singleLine = true,
                        placeholder = { Text("مثلاً: کلیپ‌های خنده‌دار", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFFFF9800)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderNameInput.isNotBlank()) {
                            onCreateFolder(folderNameInput.trim(), null)
                            showCreateFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text("ایجاد پوشه", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("انصراف", color = TextSecondary)
                }
            }
        )
    }

    // Rename Folder Dialog
    folderToRename?.let { folder ->
        var editedFolderName by remember(folder) { mutableStateOf(folder.name) }
        AlertDialog(
            onDismissRequest = { folderToRename = null },
            title = {
                Text("تغییر نام پوشه", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("نام جدید پوشه را وارد کنید:", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = editedFolderName,
                        onValueChange = { editedFolderName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFFFF9800)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedFolderName.isNotBlank()) {
                            onRenameFolder(folder.id, editedFolderName.trim())
                            folderToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text("ذخیره", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToRename = null }) {
                    Text("انصراف", color = TextSecondary)
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Filter Tabs Bar (All, Favorites, Folders)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primary Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChipTab(
                        label = "همه فایل‌ها (${historyItems.size})",
                        icon = Icons.Default.Movie,
                        isSelected = filterTab == 0,
                        onClick = {
                            onFilterTabChanged(0)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    val favoriteCount = remember(historyItems) { historyItems.count { it.isFavorite } }
                    FilterChipTab(
                        label = "علاقه‌مندی‌ها ($favoriteCount)",
                        icon = Icons.Default.Star,
                        iconTint = if (filterTab == 1) Color.White else Color(0xFFFFB300),
                        isSelected = filterTab == 1,
                        onClick = {
                            onFilterTabChanged(1)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChipTab(
                        label = "پوشه‌ها (${foldersList.size})",
                        icon = Icons.Default.Folder,
                        isSelected = filterTab == 2,
                        onClick = {
                            onFilterTabChanged(2)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Sub-header / Back to Folders or Search Controls
                if (filterTab == 2 && selectedFolderId != null && currentSelectedFolder != null) {
                    // Inside a specific folder header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onFolderSelected(null) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = Color(0xFFFF9800)
                            )
                            Text(
                                text = "پوشه: ${currentSelectedFolder.name}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        Text(
                            text = "${filteredItems.size} ویدیو",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                } else if (filterTab != 2 || selectedFolderId != null) {
                    // Top controls row (Search & View Mode Switcher)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (filterTab) {
                                1 -> "ویدیوهای مورد علاقه (${filteredItems.size})"
                                else -> "کل ویدیوها (${filteredItems.size})"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        // Layout Mode Switcher (Grid / List)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (layoutMode == 0) Color(0xFFFF9800) else Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onLayoutModeChanged(0) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewList,
                                    contentDescription = "نمایش لیستی",
                                    tint = if (layoutMode == 0) Color.White else Color.Gray,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(20.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (layoutMode == 1) Color(0xFFFF9800) else Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onLayoutModeChanged(1) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "نمایش شبکه‌ای",
                                    tint = if (layoutMode == 1) Color.White else Color.Gray,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(20.dp)
                                )
                            }
                        }
                    }

                    // Search Bar Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "جستجو در عنوان ویدیوها...",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "جستجو",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "پاک کردن",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        textStyle = TextStyle(
                            fontSize = 13.sp,
                            color = Color.Black
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = Color(0xFFFF9800),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                }
            }
        }

        // CONTENT SECTION: Folders Tab vs Video List
        if (filterTab == 2 && selectedFolderId == null) {
            // Folders List View
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Create New Folder Button
                Button(
                    onClick = { showCreateFolderDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ایجاد پوشه جدید",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (foldersList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color.White,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier
                                        .padding(20.dp)
                                        .size(52.dp)
                                )
                            }
                            Text(
                                text = "هنوز هیچ پوشه‌ای ساخته نشده است",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "برای دسته‌بندی فایل‌های دانلود شده می‌توانید پوشه‌های دلخواه بسازید.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(foldersList, key = { it.id }) { folder ->
                            val itemCount = historyItems.count { it.folderId == folder.id }
                            FolderCard(
                                folder = folder,
                                itemCount = itemCount,
                                onClick = { onFolderSelected(folder.id) },
                                onRename = { folderToRename = folder },
                                onDelete = { onDeleteFolder(folder.id) }
                            )
                        }
                    }
                }
            }
        } else {
            // Video Items View (All, Favorites, or Selected Folder Content)
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = when (filterTab) {
                                    1 -> Icons.Default.Star
                                    2 -> Icons.Default.FolderOpen
                                    else -> Icons.Default.Movie
                                },
                                contentDescription = null,
                                tint = if (filterTab == 1) Color(0xFFFFB300) else Color.Gray,
                                modifier = Modifier
                                    .padding(20.dp)
                                    .size(52.dp)
                            )
                        }
                        Text(
                            text = when {
                                searchQuery.isNotBlank() -> "هیچ ویدیویی با این عنوان یافت نشد"
                                filterTab == 1 -> "لیست مورد علاقه‌ها خالی است"
                                filterTab == 2 -> "این پوشه هنوز خالی است"
                                else -> "هنوز ویدیویی دانلود نشده است"
                            },
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (filterTab) {
                                1 -> "می‌توانید با زدن آیکون ستاره روی ویدیوها، آنها را به لیست مورد علاقه اضافه کنید."
                                2 -> "از آیکون پوشه روی ویدیوها برای انتقال آنها به این پوشه استفاده کنید."
                                else -> "ویدیوهایی که دانلود می‌کنید در این بخش ذخیره می‌شوند."
                            },
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Display Grid vs List
                if (layoutMode == 1) {
                    // Grid Mode
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            HistoryGridCard(
                                item = item,
                                onPlay = { onPlayMedia(item.mediaUri) },
                                onToggleFavorite = { onToggleFavorite(item.id, item.isFavorite) },
                                onMoveFolder = { itemToMove = item },
                                onShare = { shareVideo(context, item.mediaUri, item.title) },
                                onRename = { itemToRename = item },
                                onDelete = { onDeleteItem(item.id) }
                            )
                        }
                    }
                } else {
                    // List Mode
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            HistoryItemRow(
                                item = item,
                                onPlay = { onPlayMedia(item.mediaUri) },
                                onToggleFavorite = { onToggleFavorite(item.id, item.isFavorite) },
                                onMoveFolder = { itemToMove = item },
                                onShare = { shareVideo(context, item.mediaUri, item.title) },
                                onRename = { itemToRename = item },
                                onDelete = { onDeleteItem(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color = Color.Unspecified,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFFFF9800) else Color(0xFFF1F5F9),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else if (iconTint != Color.Unspecified) iconTint else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FolderCard(
    folder: FolderEntity,
    itemCount: Int,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color(0x12000000))
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.15f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier
                            .padding(8.dp)
                            .size(28.dp)
                    )
                }

                Row {
                    IconButton(onClick = onRename, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "ویرایش",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = StatusError,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = folder.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "$itemCount ویدیو",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun HistoryItemRow(
    item: DownloadEntity,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveFolder: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val dateStr = dateFormat.format(Date(item.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color(0x12000000))
            .testTag("history_item_${item.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Thumbnail with click play action
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE2E8F0))
                    .clickable { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                if (item.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "پخش",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(24.dp)
                    )
                }
            }

            // Info Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = formatFileSize(item.fileSizeBytes) + " • " + dateStr,
                    color = TextSecondary,
                    fontSize = 10.sp
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusSuccess.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "ذخیره‌شده در گالری",
                        color = StatusSuccess,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Action Buttons (Favorite Star, Move Folder, Rename, Share, Delete)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                        contentDescription = "مورد علاقه",
                        tint = if (item.isFavorite) Color(0xFFFFB300) else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onMoveFolder, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.DriveFileMove,
                        contentDescription = "انتقال به پوشه",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onRename, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تغییر نام",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onShare, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "اشتراک‌گذاری",
                        tint = SleekPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = StatusError,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryGridCard(
    item: DownloadEntity,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveFolder: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color(0x12000000))
            .testTag("history_grid_item_${item.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color(0xFFE2E8F0))
                    .clickable { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                if (item.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "پخش",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(28.dp)
                    )
                }

                // Favorite Star Overlay Badge on Thumbnail Top-Right
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                        contentDescription = "مورد علاقه",
                        tint = if (item.isFavorite) Color(0xFFFFB300) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = formatFileSize(item.fileSizeBytes),
                    color = TextSecondary,
                    fontSize = 10.sp
                )

                // Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onMoveFolder, modifier = Modifier.size(26.dp)) {
                        Icon(
                            imageVector = Icons.Default.DriveFileMove,
                            contentDescription = "انتقال به پوشه",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(onClick = onRename, modifier = Modifier.size(26.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تغییر نام",
                            tint = Color.Gray,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(26.dp)) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "اشتراک‌گذاری",
                            tint = SleekPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = StatusError,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "--- MB"
    val mb = bytes / (1024.0 * 1024.0)
    return String.format("%.1f مگابایت", mb)
}

private fun shareVideo(context: Context, uriString: String, title: String) {
    try {
        val uri = Uri.parse(uriString)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری ویدیو"))
    } catch (e: Exception) {
        // Fallback
    }
}
