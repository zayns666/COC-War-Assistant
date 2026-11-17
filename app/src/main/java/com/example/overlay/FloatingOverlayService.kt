package com.example.overlay

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.voice.VoiceCommander
import com.example.ai.BaseAnalysisRepository
import com.example.model.AnalysisUiState
import com.example.model.MarkerType
import com.example.state.TacticalStateHolder
import com.example.ui.components.TacticalMapCanvas
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.SlateCommand
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.VictoryEmerald
import com.example.ui.theme.WarGold
import com.example.ui.theme.WarStratTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val TAG = "FloatingOverlaySvc"
private const val NOTIFICATION_ID = 4109
private const val CHANNEL_ID = "warstrat_overlay_channel"

class FloatingOverlayService : Service() {

    companion object {
        const val ACTION_START_OVERLAY = "com.example.action.START_OVERLAY"
        const val ACTION_START_PROJECTION = "com.example.action.START_PROJECTION"
        const val ACTION_STOP_OVERLAY = "com.example.action.STOP_OVERLAY"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_PROJECTION_DATA = "extra_projection_data"

        fun startOverlayOnly(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_START_OVERLAY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startWithProjection(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_START_PROJECTION
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_PROJECTION_DATA, data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_STOP_OVERLAY
            })
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val aiRepository = BaseAnalysisRepository()
    private val voiceCommander by lazy { VoiceCommander(applicationContext) }
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var windowManager: WindowManager
    private var canvasOverlayView: ComposeView? = null
    private var canvasParams: WindowManager.LayoutParams? = null
    private var controlOverlayView: ComposeView? = null
    private var controlParams: WindowManager.LayoutParams? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureWidth: Int = 0
    private var captureHeight: Int = 0
    private var captureDensity: Int = 0
    private var isCapturing = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_OVERLAY -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_PROJECTION -> {
                startForegroundWithType(hasProjection = true)
                if (!Settings.canDrawOverlays(this)) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                ensureOverlayAttached()
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                @Suppress("DEPRECATION")
                val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_PROJECTION_DATA, Intent::class.java)
                } else {
                    intent.getParcelableExtra(EXTRA_PROJECTION_DATA)
                }
                if (resultCode == Activity.RESULT_OK && data != null) {
                    initMediaProjection(resultCode, data)
                }
            }
            else -> {
                startForegroundWithType(hasProjection = mediaProjection != null)
                if (!Settings.canDrawOverlays(this)) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                ensureOverlayAttached()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithType(hasProjection: Boolean) {
        val notification = buildNotification()
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (hasProjection) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
    }

    private fun initMediaProjection(resultCode: Int, data: Intent) {
        try {
            releaseProjectionResources()
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = manager.getMediaProjection(resultCode, data)
            if (projection == null) {
                TacticalStateHolder.setHasMediaProjection(false)
                TacticalStateHolder.setStatusBanner("Gagal memulai sesi tangkap layar.")
                return
            }

            projection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    mainHandler.post {
                        releaseProjectionResources()
                        TacticalStateHolder.setHasMediaProjection(false)
                        TacticalStateHolder.setStatusBanner("Sesi tangkap layar dihentikan.")
                    }
                }

                override fun onCapturedContentResize(width: Int, height: Int) {
                    mainHandler.post {
                        resizeCaptureSurface(width, height)
                    }
                }
            }, mainHandler)

            mediaProjection = projection
            val created = setupPersistentVirtualDisplay(projection)
            TacticalStateHolder.setHasMediaProjection(created)
            if (created) {
                TacticalStateHolder.setStatusBanner("Tangkap layar siap! Ketuk Scan Base di HUD mengambang.")
            } else {
                releaseProjectionResources()
                TacticalStateHolder.setStatusBanner("Tidak dapat membuat layar virtual tangkap layar.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaProjection", e)
            releaseProjectionResources()
            TacticalStateHolder.setHasMediaProjection(false)
            TacticalStateHolder.setStatusBanner("Gagal inisialisasi tangkap layar: ${e.localizedMessage}")
        }
    }

    private fun resolveCaptureMetrics(): Triple<Int, Int, Int> {
        val rawW: Int
        val rawH: Int
        val density: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            rawW = bounds.width()
            rawH = bounds.height()
            density = resources.configuration.densityDpi.takeIf { it > 0 }
                ?: resources.displayMetrics.densityDpi
        } else {
            val dm = resources.displayMetrics
            rawW = dm.widthPixels
            rawH = dm.heightPixels
            density = dm.densityDpi
        }
        val safeDensity = density.coerceIn(120, 640)
        // Scale down oversized screens cleanly while keeping even dimensions
        val maxSide = 1280
        val scale = if (maxOf(rawW, rawH) > maxSide) {
            maxSide.toFloat() / maxOf(rawW, rawH).toFloat()
        } else 1f
        val w = ((rawW * scale).roundToInt().coerceAtLeast(320)) and 1.inv()
        val h = ((rawH * scale).roundToInt().coerceAtLeast(320)) and 1.inv()
        return Triple(w, h, safeDensity)
    }

    private fun setupPersistentVirtualDisplay(projection: MediaProjection): Boolean {
        val (w, h, density) = resolveCaptureMetrics()
        captureWidth = w
        captureHeight = h
        captureDensity = density

        val reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        return try {
            val vd = projection.createVirtualDisplay(
                "WarStratCapture",
                w,
                h,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                mainHandler
            )
            if (vd == null) {
                Log.e(TAG, "createVirtualDisplay returned null")
                reader.close()
                imageReader = null
                false
            } else {
                virtualDisplay = vd
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating VirtualDisplay", e)
            reader.close()
            imageReader = null
            false
        }
    }

    private fun resizeCaptureSurface(width: Int, height: Int) {
        val vd = virtualDisplay ?: return
        if (width <= 0 || height <= 0) return
        val maxSide = 1280
        val scale = if (maxOf(width, height) > maxSide) {
            maxSide.toFloat() / maxOf(width, height).toFloat()
        } else 1f
        val newW = ((width * scale).roundToInt().coerceAtLeast(320)) and 1.inv()
        val newH = ((height * scale).roundToInt().coerceAtLeast(320)) and 1.inv()
        if (newW == captureWidth && newH == captureHeight) return

        // Keep the old reader usable unless the new surface is installed successfully.
        val oldReader = imageReader
        val newReader = try {
            ImageReader.newInstance(newW, newH, PixelFormat.RGBA_8888, 2)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot allocate capture reader", e)
            return
        }
        try {
            vd.resize(newW, newH, captureDensity.coerceAtLeast(160))
            vd.surface = newReader.surface
            imageReader = newReader
            captureWidth = newW
            captureHeight = newH
            oldReader?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Capture surface resize failed; restoring previous reader", e)
            runCatching { if (oldReader != null) vd.surface = oldReader.surface }
            runCatching { vd.resize(captureWidth, captureHeight, captureDensity.coerceAtLeast(160)) }
            newReader.close()
            TacticalStateHolder.setStatusBanner("Rotasi tangkap layar gagal; coba aktifkan ulang capture.")
        }
    }

    private fun releaseProjectionResources() {
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.close() }
        imageReader = null
        runCatching { mediaProjection?.stop() }
        mediaProjection = null
    }

    @SuppressLint("RtlHardcoded")
    private fun ensureOverlayAttached() {
        if (canvasOverlayView != null && controlOverlayView != null) return

        val owner = OverlayLifecycleOwner().apply { onCreate() }
        lifecycleOwner = owner
        TacticalStateHolder.setOverlayRunning(true)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // 1. Fullscreen transparent tactical canvas overlay
        val cParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        canvasParams = cParams

        val cView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setContent {
                WarStratTheme {
                    OverlayTacticalCanvasLayer()
                }
            }
        }
        canvasOverlayView = cView
        windowManager.addView(cView, cParams)

        // 2. Floating draggable control pill & mini HUD
        val ctrlParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 32
            y = 180
        }
        controlParams = ctrlParams

        val ctrlView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setContent {
                WarStratTheme {
                    FloatingHudController(
                        onDrag = { dx, dy ->
                            ctrlParams.x += dx.roundToInt()
                            ctrlParams.y += dy.roundToInt()
                            runCatching { windowManager.updateViewLayout(this, ctrlParams) }
                        },
                        onScanScreen = { captureAndAnalyzeScreen() },
                        onVoiceHint = { voiceCommander.speak("Mode panduan manual aktif. Periksa Town Hall, Clan Castle, pertahanan udara dan jalur masuk. Tandai target sebelum menyerang.") },
                        onOpenApp = {
                            val launch = Intent(this@FloatingOverlayService, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                            startActivity(launch)
                        },
                        onCloseOverlay = { stopSelf() }
                    )
                }
            }
        }
        controlOverlayView = ctrlView
        windowManager.addView(ctrlView, ctrlParams)

        // Sync touchability of the fullscreen tactical canvas with isOverlayDrawingMode
        TacticalStateHolder.isOverlayDrawingMode
            .onEach { drawingEnabled ->
                val params = canvasParams ?: return@onEach
                val view = canvasOverlayView ?: return@onEach
                params.flags = if (drawingEnabled) {
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                } else {
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                }
                runCatching { windowManager.updateViewLayout(view, params) }
            }
            .launchIn(serviceScope)
    }

    private fun captureAndAnalyzeScreen() {
        val projection = mediaProjection
        val vd = virtualDisplay
        if (projection == null || vd == null || imageReader == null) {
            TacticalStateHolder.setHasMediaProjection(false)
            TacticalStateHolder.setStatusBanner("Izin tangkap layar belum aktif. Buka WarStrat AI untuk mengaktifkan.")
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
            return
        }
        if (isCapturing) return
        isCapturing = true

        serviceScope.launch {
            try {
                // Drain any old queued frame first
                runCatching { imageReader?.acquireLatestImage()?.close() }

                // Temporarily hide overlay views so the captured screenshot shows the enemy base cleanly
                canvasOverlayView?.visibility = View.INVISIBLE
                controlOverlayView?.visibility = View.INVISIBLE
                delay(220)

                val freshAfterNanos = android.os.SystemClock.elapsedRealtimeNanos()
                val captured = grabLatestFrameFromPersistentReader(freshAfterNanos)
                canvasOverlayView?.visibility = View.VISIBLE
                controlOverlayView?.visibility = View.VISIBLE

                if (captured == null) {
                    TacticalStateHolder.setAnalysisState(
                        AnalysisUiState.Error("Gagal menangkap gambar layar. Pastikan layar bergerak atau beri izin ulang.")
                    )
                    return@launch
                }

                TacticalStateHolder.setActiveBitmap(captured)
                TacticalStateHolder.setStatusBanner("Screenshot baru berhasil diambil. Analisis lokal belum tersedia.")
                TacticalStateHolder.setAnalysisState(AnalysisUiState.Loading)

                val result = aiRepository.analyzeBase(captured)
                TacticalStateHolder.setAnalysisState(AnalysisUiState.Success(result))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Screen capture analysis error", e)
                canvasOverlayView?.visibility = View.VISIBLE
                controlOverlayView?.visibility = View.VISIBLE
                TacticalStateHolder.setAnalysisState(
                    AnalysisUiState.Error(e.localizedMessage ?: "Gagal memindai layar base.")
                )
            } finally {
                isCapturing = false
            }
        }
    }

    private suspend fun grabLatestFrameFromPersistentReader(freshAfterNanos: Long): Bitmap? {
        val reader = imageReader ?: return null
        val w = captureWidth
        val h = captureHeight
        if (w <= 0 || h <= 0) return null

        repeat(12) { attempt ->
            val image = runCatching { reader.acquireLatestImage() }.getOrNull()
            if (image != null) {
                if (image.timestamp <= 0L || image.timestamp < freshAfterNanos) {
                    image.close()
                    delay(80)
                    return@repeat
                }
                return image.use { img ->
                    val plane = img.planes[0]
                    val buffer = plane.buffer
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val rowPadding = rowStride - pixelStride * w
                    val fullBitmap = Bitmap.createBitmap(
                        w + rowPadding / pixelStride,
                        h,
                        Bitmap.Config.ARGB_8888
                    )
                    fullBitmap.copyPixelsFromBuffer(buffer)
                    val cropped = Bitmap.createBitmap(fullBitmap, 0, 0, w, h)
                    if (fullBitmap != cropped) fullBitmap.recycle()
                    cropped
                }
            }
            if (attempt < 11) delay(80)
        }
        return null
    }

    override fun onDestroy() {
        if (::windowManager.isInitialized) { /* HUD teardown continues below */ }
        voiceCommander.shutdown()
        TacticalStateHolder.setOverlayRunning(false)
        TacticalStateHolder.setHasMediaProjection(false)
        canvasOverlayView?.let { runCatching { windowManager.removeView(it) } }
        controlOverlayView?.let { runCatching { windowManager.removeView(it) } }
        canvasOverlayView = null
        controlOverlayView = null
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        releaseProjectionResources()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_STOP_OVERLAY
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setContentIntent(pendingOpen)
            .addAction(0, "Tutup Overlay", pendingStop)
            .setOngoing(true)
            .build()
    }
}

@Composable
private fun OverlayTacticalCanvasLayer() {
    val analysisState by TacticalStateHolder.analysisState.collectAsStateWithLifecycle()
    val activeStepIndex by TacticalStateHolder.activeStepIndex.collectAsStateWithLifecycle()
    val isSimulating by TacticalStateHolder.isSimulating.collectAsStateWithLifecycle()
    val manualMarkers by TacticalStateHolder.manualMarkers.collectAsStateWithLifecycle()
    val selectedTool by TacticalStateHolder.selectedMarkerTool.collectAsStateWithLifecycle()
    val isDrawingMode by TacticalStateHolder.isOverlayDrawingMode.collectAsStateWithLifecycle()
    val showAiPaths by TacticalStateHolder.showAiPaths.collectAsStateWithLifecycle()
    val showHeatmap by TacticalStateHolder.showWeaknessHeatmap.collectAsStateWithLifecycle()

    val result = (analysisState as? AnalysisUiState.Success)?.result

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isDrawingMode) {
                    Modifier
                        .background(ObsidianNight.copy(alpha = 0.14f))
                        .border(2.dp, WarGold.copy(alpha = 0.7f))
                } else Modifier
            )
    ) {
        TacticalMapCanvas(
            analysisResult = result,
            activeStepIndex = activeStepIndex,
            isSimulating = isSimulating,
            showAiPaths = showAiPaths,
            showWeaknessHeatmap = showHeatmap,
            manualMarkers = manualMarkers,
            interactiveMarkingEnabled = isDrawingMode,
            selectedTool = selectedTool,
            onTapPlaceMarker = { x, y -> TacticalStateHolder.addTapMarker(x, y) },
            onDrawPathComplete = { pts -> TacticalStateHolder.addPathMarker(pts) },
            modifier = Modifier.fillMaxSize(),
            drawGridOverlay = isDrawingMode
        )

        if (isDrawingMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 36.dp)
                    .clip(CircleShape)
                    .background(ObsidianNight.copy(alpha = 0.82f))
                    .border(1.dp, WarGold.copy(alpha = 0.8f), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "MODE TANDAI REAL-TIME: Ketuk / seret di atas base (${selectedTool.labelId})",
                    style = MaterialTheme.typography.labelMedium,
                    color = WarGold
                )
            }
        }
    }
}

@Composable
private fun FloatingHudController(
    onDrag: (Float, Float) -> Unit,
    onScanScreen: () -> Unit,
    onVoiceHint: () -> Unit,
    onOpenApp: () -> Unit,
    onCloseOverlay: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val analysisState by TacticalStateHolder.analysisState.collectAsStateWithLifecycle()
    val activeStepIndex by TacticalStateHolder.activeStepIndex.collectAsStateWithLifecycle()
    val isSimulating by TacticalStateHolder.isSimulating.collectAsStateWithLifecycle()
    val isDrawingMode by TacticalStateHolder.isOverlayDrawingMode.collectAsStateWithLifecycle()
    val selectedTool by TacticalStateHolder.selectedMarkerTool.collectAsStateWithLifecycle()
    val hasProjection by TacticalStateHolder.hasMediaProjection.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        Color(0xFF151E32).copy(alpha = 0.86f),
                        ObsidianNight.copy(alpha = 0.92f)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(CyberCyan.copy(alpha = 0.7f), WarGold.copy(alpha = 0.65f))
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .widthIn(max = 312.dp)
        ) {
            // Draggable Esports Glass Header Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(WarGold, Color(0xFFFBBF24))
                            )
                        )
                        .clickable { expanded = !expanded }
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Toggle WarStrat HUD",
                        tint = ObsidianNight,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable { expanded = !expanded }
                ) {
                    Text(
                        text = "WARSTRAT // HUD",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            analysisState is AnalysisUiState.Loading -> "Menganalisis base..."
                            isDrawingMode -> "Sentuh layar untuk menandai"
                            else -> "Geser HUD • Ketuk tutup/buka"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDrawingMode) WarGold else CyberCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onOpenApp,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Buka Aplikasi",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onCloseOverlay,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup Overlay",
                        tint = ThreatCrimson,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    // Capsule Quick Actions Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(
                                    if (hasProjection) CyberCyan else Color.White.copy(alpha = 0.08f)
                                )
                                .border(
                                    1.dp,
                                    CyberCyan.copy(alpha = 0.6f),
                                    CircleShape
                                )
                                .clickable(onClick = onScanScreen)
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            if (analysisState is AnalysisUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(15.dp),
                                    strokeWidth = 2.dp,
                                    color = ObsidianNight
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Scan Layar",
                                    tint = if (hasProjection) ObsidianNight else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (hasProjection) "Scan Base" else "Izin Scan",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (hasProjection) ObsidianNight else Color.White
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(
                                    if (isDrawingMode) WarGold else Color.White.copy(alpha = 0.08f)
                                )
                                .border(
                                    1.dp,
                                    WarGold.copy(alpha = 0.6f),
                                    CircleShape
                                )
                                .clickable { TacticalStateHolder.toggleOverlayDrawingMode() }
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isDrawingMode) Icons.Default.LockOpen else Icons.Default.Edit,
                                contentDescription = "Mode Penanda",
                                tint = if (isDrawingMode) ObsidianNight else WarGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (isDrawingMode) "Kunci Game" else "Tandai Base",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isDrawingMode) ObsidianNight else WarGold
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "🔊 Dengar panduan manual",
                        color = CyberCyan,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onVoiceHint)
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    )

                    // Floating Pill Marker Tools
                    if (isDrawingMode) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MarkerType.entries.forEach { tool ->
                                val active = selectedTool == tool
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (active) WarGold else Color.White.copy(alpha = 0.08f))
                                        .clickable { TacticalStateHolder.setSelectedMarkerTool(tool) }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = tool.labelId,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (active) ObsidianNight else Color.White
                                    )
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { TacticalStateHolder.undoMarker() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Undo Penanda",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { TacticalStateHolder.clearMarkers() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Hapus Semua Penanda",
                                    tint = ThreatCrimson,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Frosted Step-by-Step Mini HUD
                    val result = (analysisState as? AnalysisUiState.Success)?.result
                    if (result != null && result.steps.isNotEmpty()) {
                        val step = result.steps.getOrNull(activeStepIndex)
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(ObsidianNight.copy(alpha = 0.72f))
                                .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = result.recommendedStrategyName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = WarGold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${result.efficiencyScore}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = VictoryEmerald
                                    )
                                }

                                if (step != null) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Fase 0${step.stepNumber}: ${step.title}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = step.instruction,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { TacticalStateHolder.prevStep() },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                                contentDescription = "Fase Sebelumnya",
                                                tint = Color.White
                                            )
                                        }
                                        Text(
                                            text = "0${activeStepIndex + 1}/0${result.steps.size}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = CyberCyan
                                        )
                                        IconButton(
                                            onClick = { TacticalStateHolder.nextStep() },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = "Fase Berikutnya",
                                                tint = Color.White
                                            )
                                        }
                                    }

                                    Row {
                                        IconButton(
                                            onClick = { TacticalStateHolder.toggleShowAiPaths() },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Visibility,
                                                contentDescription = "Toggle Jalur AI",
                                                tint = CyberCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { TacticalStateHolder.toggleSimulation() },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isSimulating) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Play Pause Simulasi",
                                                tint = WarGold,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (analysisState is AnalysisUiState.Error) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = (analysisState as AnalysisUiState.Error).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = ThreatCrimson,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
