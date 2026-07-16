package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IndexedFile
import com.example.ui.viewmodel.AiSearchState
import com.example.ui.viewmodel.FileManagerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: FileManagerViewModel,
    modifier: Modifier = Modifier
) {
    val files by viewModel.nonVaultFiles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isAiEnabled by viewModel.isAiSearchEnabled.collectAsState()
    val aiSearchState by viewModel.aiSearchState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val vaultPin by viewModel.vaultPin.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            viewModel.importFileFromUri(context, it)
        }
    }

    var showPinRequiredDialog by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf("date_desc") }
    var fileToRename by remember { mutableStateOf<IndexedFile?>(null) }
    var newFileNameInput by remember { mutableStateOf("") }

    // Filter logic: Standard keyword filter or AI filters with Sort logic
    val displayedFiles = remember(files, searchQuery, isAiEnabled, aiSearchState, selectedCategory, sortBy) {
        var baseList = if (selectedCategory == "All") {
            files
        } else {
            files.filter { it.classification == selectedCategory }
        }

        if (searchQuery.isNotEmpty()) {
            baseList = if (isAiEnabled) {
                // In AI search mode, show matched IDs if search was successful
                when (val state = aiSearchState) {
                    is AiSearchState.Success -> {
                        baseList.filter { file -> state.matchedIds.contains(file.id) }
                    }
                    is AiSearchState.Loading -> baseList // Wait for loading to finish
                    else -> emptyList() // Default empty in idle/error until search matches
                }
            } else {
                // Local search matching: name, content summary, or summary keywords
                baseList.filter { file ->
                    file.name.contains(searchQuery, ignoreCase = true) ||
                    (file.contentSummary?.contains(searchQuery, ignoreCase = true) == true) ||
                    (file.summaryKeywords?.contains(searchQuery, ignoreCase = true) == true)
                }
            }
        }

        when (sortBy) {
            "date_desc" -> baseList.sortedByDescending { it.timestamp }
            "date_asc" -> baseList.sortedBy { it.timestamp }
            "name_asc" -> baseList.sortedBy { it.name.lowercase() }
            "name_desc" -> baseList.sortedByDescending { it.name.lowercase() }
            "size_desc" -> baseList.sortedByDescending { it.size }
            "size_asc" -> baseList.sortedBy { it.size }
            else -> baseList
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Search Bar with AI Switch
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Raw input field
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("फ़ाइल खोजें (Search files)...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search Icon",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Clear Search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_field")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // AI Toggle Switch Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "AI Search Icon",
                                tint = if (isAiEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI-पॉवर्ड सर्च (Gemini Mode)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isAiEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isAiEnabled,
                            onCheckedChange = { viewModel.toggleAiSearch(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("ai_search_switch")
                        )
                    }

                    // Trigger Search Button if AI search is enabled and query is typed
                    if (isAiEnabled && searchQuery.isNotEmpty() && aiSearchState is AiSearchState.Idle) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.triggerAiSearch() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_search_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "AI",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gemini AI से खोजें (Ask Gemini)")
                        }
                    }
                }
            }

            // 2. Horizontal Categories Row
            val cats = listOf("All", "Images", "Videos", "Audios", "Documents")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cats) { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            )
                            .then(
                                if (isSelected) Modifier
                                else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            )
                            .clickable { viewModel.setCategory(cat) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("category_pill_$cat"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (cat) {
                                "All" -> "सभी फ़ाइलें"
                                "Images" -> "फोटोज़"
                                "Videos" -> "वीडियो"
                                "Audios" -> "ऑडियो"
                                "Documents" -> "दस्तावेज़"
                                else -> cat
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2b. Sort & Refresh Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                var showSortMenu by remember { mutableStateOf(false) }
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable { showSortMenu = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Category,
                            contentDescription = "Sort Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = when (sortBy) {
                                "date_desc" -> "दिनांक: नई से पुरानी"
                                "date_asc" -> "दिनांक: पुरानी से नई"
                                "name_asc" -> "नाम: A से Z"
                                "name_desc" -> "नाम: Z से A"
                                "size_desc" -> "आकार: बड़ी से छोटी"
                                "size_asc" -> "आकार: छोटी से बड़ी"
                                else -> "क्रमबद्ध करें"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    androidx.compose.material3.DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        listOf(
                            Pair("date_desc", "दिनांक: नई से पुरानी"),
                            Pair("date_asc", "दिनांक: पुरानी से नई"),
                            Pair("name_asc", "नाम: A से Z"),
                            Pair("name_desc", "नाम: Z से A"),
                            Pair("size_desc", "आकार: बड़ी से छोटी"),
                            Pair("size_asc", "आकार: छोटी से बड़ी")
                        ).forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    sortBy = value
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { viewModel.scanDeviceFiles() },
                    modifier = Modifier.size(32.dp).testTag("refresh_files_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh Device Files",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. AI Search Explanation and Loading State Banner
            AnimatedVisibility(
                visible = isAiEnabled && searchQuery.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    when (val state = aiSearchState) {
                        is AiSearchState.Loading -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Gemini आपकी लोकल फाइल्स का विश्लेषण कर रहा है...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        is AiSearchState.Success -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                ),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = "AI Sparkle",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Gemini AI Search Insight",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = state.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        is AiSearchState.Error -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        is AiSearchState.Idle -> {
                            Text(
                                text = "प्रेस करें 'Gemini AI से खोजें' अर्थ-आधारित (Semantic) परिणाम देखने के लिए।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

            // 4. Files List
            if (displayedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = "No Files",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "कोई फ़ाइल नहीं मिली।" else "यह फ़ोल्डर खाली है।",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "निचला '+' बटन दबाकर नई फ़ाइल जोड़ें।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayedFiles, key = { it.id }) { file ->
                        FileItemRow(
                            file = file,
                            onDelete = { viewModel.deleteFile(file) },
                            onVaultMove = {
                                val isPinSetNow = viewModel.isPinSet.value
                                if (!isPinSetNow) {
                                    showPinRequiredDialog = true
                                } else {
                                    viewModel.toggleVaultStatus(file)
                                }
                            },
                            onRename = {
                                fileToRename = file
                                newFileNameInput = file.name
                            },
                            onCopy = {
                                val f = java.io.File(file.path)
                                val parent = f.parent ?: ""
                                val nameWithoutExt = f.nameWithoutExtension
                                val ext = f.extension
                                val newName = if (ext.isNotEmpty()) "${nameWithoutExt}_copy.${ext}" else "${nameWithoutExt}_copy"
                                val destFile = java.io.File(parent, newName)
                                viewModel.copyFile(file, destFile.absolutePath)
                            },
                            onMove = {
                                // Move file to a specific "moved_files" internal subfolder
                                val f = java.io.File(file.path)
                                val movedDir = java.io.File(f.parentFile?.parentFile, "moved_files")
                                viewModel.moveFile(file, movedDir.absolutePath)
                            },
                            onShare = {
                                try {
                                    val f = java.io.File(file.path)
                                    val authority = "${context.packageName}.provider"
                                    val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, f)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = file.mimeType
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "साझा करें"))
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "साझा करने में विफलता", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            onOpen = {
                                try {
                                    val f = java.io.File(file.path)
                                    val authority = "${context.packageName}.provider"
                                    val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, f)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, file.mimeType)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "फ़ाइल खोलने के लिए उपयुक्त ऐप नहीं मिला।", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }

        // 5. Floating Action Button: Add File
        FloatingActionButton(
            onClick = { filePickerLauncher.launch("*/*") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_file_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add File")
        }
    }

    if (showPinRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showPinRequiredDialog = false },
            title = {
                Text(
                    text = "सिक्योर पिन सेट करें",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = "फाइलों को वॉल्ट में सुरक्षित (Encrypt) करने के लिए, पहले 'Secure Vault' टैब में जाकर अपना 4-अंकीय पिन सेट करें।",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { showPinRequiredDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ठीक है")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (fileToRename != null) {
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = {
                Text(
                    text = "फ़ाइल का नाम बदलें",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                OutlinedTextField(
                    value = newFileNameInput,
                    onValueChange = { newFileNameInput = it },
                    label = { Text("नया नाम दर्ज करें") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rename_input_field")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentFile = fileToRename
                        if (currentFile != null && newFileNameInput.trim().isNotEmpty()) {
                            viewModel.renameFile(currentFile, newFileNameInput.trim())
                        }
                        fileToRename = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("rename_confirm_btn")
                ) {
                    Text("पुष्टि करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) {
                    Text("रद्द करें")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun FileItemRow(
    file: IndexedFile,
    onDelete: () -> Unit,
    onVaultMove: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable { onOpen() }
            .testTag("file_item_${file.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (file.classification) {
                            "Images" -> Icons.Filled.Image
                            "Videos" -> Icons.Filled.Movie
                            "Audios" -> Icons.Filled.MusicNote
                            else -> Icons.Filled.Description
                        },
                        contentDescription = file.classification,
                        tint = when (file.classification) {
                            "Images" -> Color(0xFF4CAF50)
                            "Videos" -> Color(0xFF2196F3)
                            "Audios" -> Color(0xFFFF9800)
                            else -> Color(0xFF9C27B0)
                        }
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatSize(file.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatDate(file.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!file.contentSummary.isNullOrEmpty() || !file.summaryKeywords.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        androidx.compose.material3.Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                if (!file.contentSummary.isNullOrEmpty()) {
                                    Text(
                                        text = file.contentSummary,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (!file.summaryKeywords.isNullOrEmpty()) {
                                    if (!file.contentSummary.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                    Text(
                                        text = "टैग्स: ${file.summaryKeywords}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Options triggers
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Send to Vault action (Lock Icon)
                IconButton(
                    onClick = onVaultMove,
                    modifier = Modifier.testTag("file_lock_btn_${file.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Move to Vault",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("file_menu_btn_${file.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    androidx.compose.material3.DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("खोलें (Open)") },
                            onClick = {
                                showMenu = false
                                onOpen()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("साझा करें (Share)") },
                            onClick = {
                                showMenu = false
                                onShare()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("नाम बदलें (Rename)") },
                            onClick = {
                                showMenu = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("कॉपी बनाएं (Copy)") },
                            onClick = {
                                showMenu = false
                                onCopy()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("स्थानांतरित करें (Move)") },
                            onClick = {
                                showMenu = false
                                onMove()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("हटाएं (Delete)", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
