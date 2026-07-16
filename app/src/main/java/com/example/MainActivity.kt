package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.InsertChart
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.FileRepository
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FileManagerViewModel
import com.example.ui.viewmodel.ViewModelFactory

class MainActivity : androidx.fragment.app.FragmentActivity() {
    private lateinit var viewModel: FileManagerViewModel

    @android.annotation.SuppressLint("InvalidFragmentVersionForActivityResult")
    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.all { it.value }
        if (granted) {
            viewModel.scanDeviceFiles()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialize local Room database and repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = FileRepository(database.fileDao())
        
        // 2. Instantiate ViewModel using ViewModelProvider and Factory
        viewModel = ViewModelProvider(
            this,
            ViewModelFactory(application, repository)
        )[FileManagerViewModel::class.java]

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppLayout(viewModel = viewModel)
            }
        }

        requestStoragePermissions()
    }

    override fun onResume() {
        super.onResume()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (android.os.Environment.isExternalStorageManager()) {
                viewModel.scanDeviceFiles()
            }
        } else {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                viewModel.scanDeviceFiles()
            }
        }
    }

    private fun requestStoragePermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = android.net.Uri.parse("package:${packageName}")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            } else {
                viewModel.scanDeviceFiles()
            }
        } else {
            val permissions = arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            permissionLauncher.launch(permissions)
        }
    }
}

@Composable
fun MainAppLayout(
    viewModel: FileManagerViewModel
) {
    // Elegant, robust state-based navigation
    // 0: Home Dashboard, 1: Files Browser, 2: Analytics & Cleaner, 3: Secure Vault
    var currentScreenIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing, // Perfect Edge-to-Edge window inset handling
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                val items = listOf(
                    NavigationTabItem(
                        index = 0,
                        title = "होम",
                        englishTitle = "Home",
                        selectedIcon = Icons.Filled.Storage,
                        unselectedIcon = Icons.Outlined.Storage
                    ),
                    NavigationTabItem(
                        index = 1,
                        title = "ब्राउज़र",
                        englishTitle = "Files",
                        selectedIcon = Icons.Filled.Category,
                        unselectedIcon = Icons.Outlined.Category
                    ),
                    NavigationTabItem(
                        index = 2,
                        title = "एनालिटिक्स",
                        englishTitle = "Cleaner",
                        selectedIcon = Icons.Filled.InsertChart,
                        unselectedIcon = Icons.Outlined.InsertChart
                    ),
                    NavigationTabItem(
                        index = 3,
                        title = "वॉल्ट",
                        englishTitle = "Vault",
                        selectedIcon = Icons.Filled.Lock,
                        unselectedIcon = Icons.Outlined.Lock
                    )
                )

                items.forEach { tab ->
                    val isSelected = currentScreenIndex == tab.index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreenIndex = tab.index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.englishTitle
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp
                            )
                        },
                        modifier = Modifier.testTag("nav_item_${tab.englishTitle.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreenIndex) {
                0 -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToCategory = { category ->
                        viewModel.setCategory(category)
                        currentScreenIndex = 1 // Switch to Files screen with filter applied
                    },
                    onNavigateToAnalytics = {
                        currentScreenIndex = 2 // Switch to Analytics & Cleaner screen
                    },
                    onNavigateToVault = {
                        currentScreenIndex = 3 // Switch to Secure Vault screen
                    }
                )
                1 -> BrowserScreen(
                    viewModel = viewModel
                )
                2 -> AnalyticsScreen(
                    viewModel = viewModel
                )
                3 -> VaultScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}

data class NavigationTabItem(
    val index: Int,
    val title: String,
    val englishTitle: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)
