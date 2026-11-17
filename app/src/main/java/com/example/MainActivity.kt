package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.overlay.FloatingOverlayService
import com.example.ui.components.EsportsGlassPanel
import com.example.ui.components.GlassStatusPill
import com.example.ui.components.ObsidianGlassBackdrop
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.TacticalMapScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.VictoryEmerald
import com.example.ui.theme.WarGold
import com.example.ui.theme.WarStratTheme
import com.example.viewmodel.WarViewModel

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_TACTICAL_MAP = "tactical_map"
const val ROUTE_HISTORY = "history"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration before UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            WarStratTheme {
                WarStratApp()
            }
        }
    }
}

@Composable
fun WarStratApp(viewModel: WarViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val activeBitmap by viewModel.activeBitmap.collectAsStateWithLifecycle()
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val activeStepIndex by viewModel.activeStepIndex.collectAsStateWithLifecycle()
    val isSimulating by viewModel.isSimulating.collectAsStateWithLifecycle()
    val manualMarkers by viewModel.manualMarkers.collectAsStateWithLifecycle()
    val selectedMarkerTool by viewModel.selectedMarkerTool.collectAsStateWithLifecycle()
    val showAiPaths by viewModel.showAiPaths.collectAsStateWithLifecycle()
    val showWeaknessHeatmap by viewModel.showWeaknessHeatmap.collectAsStateWithLifecycle()
    val isOverlayRunning by viewModel.isOverlayRunning.collectAsStateWithLifecycle()
    val hasMediaProjection by viewModel.hasMediaProjection.collectAsStateWithLifecycle()
    val savedPlans by viewModel.savedPlans.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDrawOverlays = Settings.canDrawOverlays(context)
                hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        canDrawOverlays = Settings.canDrawOverlays(context)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            FloatingOverlayService.startWithProjection(context, result.resultCode, result.data!!)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.analyzeUri(uri)
            navController.navigate(ROUTE_TACTICAL_MAP) {
                launchSingleTop = true
            }
        }
    }

    val launchOverlayPermission = {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        overlayPermissionLauncher.launch(intent)
    }

    val launchNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val launchMediaProjection = {
        val mgr = context.getSystemService(MediaProjectionManager::class.java)
        val captureIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mgr.createScreenCaptureIntent(
                android.media.projection.MediaProjectionConfig.createConfigForDefaultDisplay()
            )
        } else {
            mgr.createScreenCaptureIntent()
        }
        projectionLauncher.launch(captureIntent)
    }

    val toggleOverlayOnly = {
        if (isOverlayRunning) {
            FloatingOverlayService.stop(context)
        } else {
            FloatingOverlayService.startOverlayOnly(context)
        }
    }

    val launchPhotoPicker = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_DASHBOARD

    val navItems = listOf(
        Triple(ROUTE_DASHBOARD, stringResource(R.string.nav_command_center), Icons.Default.Shield),
        Triple(ROUTE_TACTICAL_MAP, stringResource(R.string.nav_tactical_map), Icons.Default.Map),
        Triple(ROUTE_HISTORY, stringResource(R.string.nav_war_history), Icons.Default.History)
    )

    val navigateToRoute: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(ROUTE_DASHBOARD) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    ObsidianGlassBackdrop {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 720.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.navigationBarsPadding().padding(bottom = 84.dp)
                    )
                },
                topBar = {
                    EsportsGlassTopHeader(isOverlayRunning = isOverlayRunning)
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (useRail) {
                            FloatingGlassNavRail(
                                items = navItems,
                                currentRoute = currentRoute,
                                onSelect = navigateToRoute,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .navigationBarsPadding()
                                    .padding(start = 12.dp, top = 8.dp, bottom = 16.dp)
                            )
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            NavHost(
                                navController = navController,
                                startDestination = ROUTE_DASHBOARD
                            ) {
                                composable(ROUTE_DASHBOARD) {
                                    DashboardScreen(
                                        canDrawOverlays = canDrawOverlays,
                                        hasNotificationPermission = hasNotificationPermission,
                                        isOverlayRunning = isOverlayRunning,
                                        hasMediaProjection = hasMediaProjection,
                                        savedPlans = savedPlans,
                                        onRequestOverlayPermission = launchOverlayPermission,
                                        onRequestNotificationPermission = launchNotificationPermission,
                                        onRequestMediaProjectionAndStartOverlay = launchMediaProjection,
                                        onToggleOverlayOnly = toggleOverlayOnly,
                                        onPickScreenshot = launchPhotoPicker,
                                        onAnalyzeSampleBase = {
                                            viewModel.analyzeSampleBase()
                                            navigateToRoute(ROUTE_TACTICAL_MAP)
                                        },
                                        onOpenPlan = { plan ->
                                            viewModel.loadSavedPlan(plan) {
                                                navigateToRoute(ROUTE_TACTICAL_MAP)
                                            }
                                        }
                                    )
                                }

                                composable(ROUTE_TACTICAL_MAP) {
                                    TacticalMapScreen(
                                        bitmap = activeBitmap,
                                        analysisState = analysisState,
                                        activeStepIndex = activeStepIndex,
                                        isSimulating = isSimulating,
                                        showAiPaths = showAiPaths,
                                        showWeaknessHeatmap = showWeaknessHeatmap,
                                        manualMarkers = manualMarkers,
                                        selectedMarkerTool = selectedMarkerTool,
                                        onSelectStep = viewModel::selectStep,
                                        onPrevStep = viewModel::prevStep,
                                        onNextStep = viewModel::nextStep,
                                        onToggleSimulation = viewModel::toggleSimulation,
                                        onToggleAiPaths = viewModel::toggleShowAiPaths,
                                        onToggleHeatmap = viewModel::toggleShowWeaknessHeatmap,
                                        onSelectMarkerTool = viewModel::setSelectedTool,
                                        onTapPlaceMarker = viewModel::addTapMarker,
                                        onDrawPathComplete = viewModel::addPathMarker,
                                        onUndoMarker = viewModel::undoMarker,
                                        onClearMarkers = viewModel::clearMarkers,
                                        onSaveWithMarkers = viewModel::saveCurrentSnapshotWithMarkers,
                                        onPickScreenshot = launchPhotoPicker,
                                        onAnalyzeSampleBase = viewModel::analyzeSampleBase,
                                        onRetryAnalysis = viewModel::reanalyzeActiveBase
                                    )
                                }

                                composable(ROUTE_HISTORY) {
                                    HistoryScreen(
                                        plans = savedPlans,
                                        onOpenPlan = { plan ->
                                            viewModel.loadSavedPlan(plan) {
                                                navigateToRoute(ROUTE_TACTICAL_MAP)
                                            }
                                        },
                                        onDeletePlan = viewModel::deleteSavedPlan
                                    )
                                }
                            }
                        }
                    }

                    // Floating Capsule Bottom Navigation Dock (compact/phone width)
                    if (!useRail) {
                        FloatingCapsuleNavDock(
                            items = navItems,
                            currentRoute = currentRoute,
                            onSelect = navigateToRoute,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EsportsGlassTopHeader(isOverlayRunning: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(WarGold, Color(0xFFFBBF24)))
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = ObsidianNight,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "WARSTRAT // AI",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ESPORTS TACTICAL OVERLAY",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan
                )
            }
        }

        GlassStatusPill(
            label = if (isOverlayRunning) "LIVE HUD" else "STANDBY",
            accentColor = if (isOverlayRunning) VictoryEmerald else WarGold
        )
    }
}

@Composable
private fun FloatingCapsuleNavDock(
    items: List<Triple<String, String, ImageVector>>,
    currentRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    EsportsGlassPanel(
        shape = CircleShape,
        accentColor = CyberCyan,
        surfaceAlpha = 0.86f,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            items.forEach { (route, label, icon) ->
                val selected = currentRoute == route
                val pillBg by animateColorAsState(
                    targetValue = if (selected) WarGold else Color.Transparent,
                    label = "dock_pill_bg"
                )
                val fg = if (selected) ObsidianNight else Color(0xFFCBD5E1)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(pillBg)
                        .clickable { onSelect(route) }
                        .padding(horizontal = if (selected) 16.dp else 14.dp)
                        .testTag("nav_$route")
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = fg,
                        modifier = Modifier.size(20.dp)
                    )
                    if (selected) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = fg,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingGlassNavRail(
    items: List<Triple<String, String, ImageVector>>,
    currentRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    EsportsGlassPanel(
        shape = RoundedCornerShape(28.dp),
        accentColor = CyberCyan,
        surfaceAlpha = 0.82f,
        modifier = modifier.width(84.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp)
        ) {
            items.forEach { (route, label, icon) ->
                val selected = currentRoute == route
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) WarGold else Color.Transparent)
                        .clickable { onSelect(route) }
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .testTag("nav_$route")
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) ObsidianNight else Color(0xFFCBD5E1),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) ObsidianNight else Color(0xFFCBD5E1),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
