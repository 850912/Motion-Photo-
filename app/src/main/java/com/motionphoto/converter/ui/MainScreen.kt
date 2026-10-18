package com.motionphoto.converter.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CameraEnhance
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MotionPhotosOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.motionphoto.converter.core.MotionPhotoBuilder
import com.motionphoto.converter.ui.components.AppTab
import com.motionphoto.converter.ui.components.ExpressiveConnectedButtonGroup
import com.motionphoto.converter.ui.components.ExpressiveFloatingToolbar
import com.motionphoto.converter.ui.components.ExpressiveMorphingBadge
import com.motionphoto.converter.ui.components.ExpressiveWavyCircularHalo
import com.motionphoto.converter.ui.components.ExpressiveWavyLinearProgress
import com.motionphoto.converter.ui.components.LiveMotionPhotoPlayerCard
import com.motionphoto.converter.ui.theme.ExpressivePalette
import com.motionphoto.converter.ui.theme.expressiveBounceClick
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

private val PreviewModes = listOf("VIDEO", "COVER")
private val QualityOptions = listOf(88, 95, 100)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val themeSettings by viewModel.themeSettings.collectAsState()
    val statusBanner by viewModel.statusBannerMessage.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val isDark = themeSettings.darkModeOverride ?: systemDark

    var showPaletteSheet by remember { mutableStateOf(false) }
    var showLiquidGlassSheet by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    val pickVideoVisualMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectVideo(uri)
        }
    }

    val pickVideoFallback = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectVideo(uri)
        }
    }

    val pickMultipleVideos = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(12)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.startBatchConversion(uris)
        }
    }

    val pickCustomCoverPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setCustomCoverImage(uri)
        }
    }

    val pickReferenceExifPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setReferenceExifImage(uri)
        }
    }

    val pickImageForInspector = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.inspectMotionPhotoUri(uri)
        }
    }

    val launchVideoPicker = remember(pickVideoVisualMedia, pickVideoFallback) {
        {
            try {
                pickVideoVisualMedia.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                )
            } catch (_: Exception) {
                pickVideoFallback.launch("video/*")
            }
        }
    }

    val launchBatchVideoPicker = remember(pickMultipleVideos, pickVideoFallback) {
        {
            try {
                pickMultipleVideos.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                )
            } catch (_: Exception) {
                pickVideoFallback.launch("video/*")
            }
        }
    }

    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
    }

    Scaffold(
        topBar = {
            ExpressiveTopBar(
                currentTab = currentTab,
                isDark = isDark,
                onOpenThemeSheet = { showPaletteSheet = true },
                onToggleDark = { viewModel.cycleDarkMode(isDark) }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                ExpressiveFloatingToolbar(
                    currentTab = currentTab,
                    onSelectTab = viewModel::selectTab,
                    onQuickPickVideo = launchVideoPicker,
                    liquidGlassConfig = themeSettings.liquidGlass,
                    isDark = isDark,
                    onOpenLiquidGlassTuner = { showLiquidGlassSheet = true }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            AnimatedContent(
                targetState = currentTab,
                contentKey = { it.name },
                transitionSpec = {
                    fadeIn(animationSpec = tween(140)) togetherWith
                        fadeOut(animationSpec = tween(110))
                },
                label = "tab_content_transition"
            ) { tab ->
                when (tab) {
                    AppTab.STUDIO -> {
                        val studioState by viewModel.studioState.collectAsState()
                        StudioTabContent(
                            state = studioState,
                            onPickVideo = launchVideoPicker,
                            onPickBatchVideos = launchBatchVideoPicker,
                            onPickCustomCover = { pickCustomCoverPhoto.launch("image/*") },
                            onClearCustomCover = { viewModel.setCustomCoverImage(null) },
                            onPickReferenceExif = { pickReferenceExifPhoto.launch("image/*") },
                            onClearReferenceExif = { viewModel.setReferenceExifImage(null) },
                            onUpdateCoverTime = viewModel::updateCoverFrameTime,
                            onUpdateTrimRange = viewModel::updateTrimRange,
                            onUpdateAudio = viewModel::updateIncludeAudio,
                            onUpdateQuality = viewModel::updateJpegQuality,
                            onUpdateFormat = viewModel::updateOutputFormat,
                            onUpdateLongExposure = viewModel::updateLongExposureMode,
                            onUpdateFilter = viewModel::updateCoverFilter,
                            onUpdateWatermark = viewModel::updateWatermarkEnabled,
                            onStartConvert = viewModel::startConversion,
                            onReset = viewModel::resetStudio,
                            onInspectFile = viewModel::inspectMotionPhotoFile,
                            onShareFile = { file -> shareMotionPhoto(context, file) }
                        )
                    }

                    AppTab.INSPECTOR -> {
                        val inspectorState by viewModel.inspectorState.collectAsState()
                        InspectorTabContent(
                            state = inspectorState,
                            onPickImage = { pickImageForInspector.launch("image/*") },
                            onExportVideo = viewModel::exportInspectedVideo,
                            onExportStill = viewModel::exportInspectedStill,
                            onRepairMotionPhoto = viewModel::repairInspectedMotionPhoto
                        )
                    }

                    AppTab.GALLERY -> {
                        val historyItems by viewModel.historyFiles.collectAsState()
                        GalleryTabContent(
                            historyItems = historyItems,
                            onInspectFile = viewModel::inspectMotionPhotoFile,
                            onShareFile = { file -> shareMotionPhoto(context, file) },
                            onDeleteFile = viewModel::deleteHistoryFile,
                            onPickVideo = launchVideoPicker
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = statusBanner != null,
                enter = fadeIn(tween(140)) + scaleIn(initialScale = 0.92f, animationSpec = tween(140)),
                exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.92f, animationSpec = tween(140)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 8.dp
                ) {
                    Text(
                        text = statusBanner ?: "",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    if (showPaletteSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPaletteSheet = false },
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            ThemeCustomizationSheet(
                settings = themeSettings,
                onSelectPalette = viewModel::updatePalette,
                onToggleDynamic = viewModel::toggleDynamicColor,
                onOpenLiquidGlass = {
                    showPaletteSheet = false
                    showLiquidGlassSheet = true
                },
                onDismiss = { showPaletteSheet = false }
            )
        }
    }

    if (showLiquidGlassSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLiquidGlassSheet = false },
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            scrimColor = Color.Black.copy(alpha = 0.18f)
        ) {
            LiquidGlassTunerSheet(
                config = themeSettings.liquidGlass,
                onUpdateConfig = viewModel::updateLiquidGlassConfig,
                onSelectPreset = viewModel::applyLiquidGlassPreset,
                onResetDefault = {
                    viewModel.applyLiquidGlassPreset(MainViewModel.LiquidGlassPreset.CRYSTAL_DROP)
                }
            )
        }
    }
}

@Composable
private fun ExpressiveTopBar(
    currentTab: AppTab,
    isDark: Boolean,
    onOpenThemeSheet: () -> Unit,
    onToggleDark: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.MotionPhotosOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column {
                Text(
                    text = "实况照片",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = when (currentTab) {
                        AppTab.STUDIO -> "视频转动态照片"
                        AppTab.INSPECTOR -> "查看信息与提取视频"
                        AppTab.GALLERY -> "历史记录"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .expressiveBounceClick { onOpenThemeSheet() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = "主题配色",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .expressiveBounceClick { onToggleDark() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = "深浅色模式",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioTabContent(
    state: MainViewModel.StudioState,
    onPickVideo: () -> Unit,
    onPickBatchVideos: () -> Unit,
    onPickCustomCover: () -> Unit,
    onClearCustomCover: () -> Unit,
    onPickReferenceExif: () -> Unit,
    onClearReferenceExif: () -> Unit,
    onUpdateCoverTime: (Long) -> Unit,
    onUpdateTrimRange: (Long, Long) -> Unit,
    onUpdateAudio: (Boolean) -> Unit,
    onUpdateQuality: (Int) -> Unit,
    onUpdateFormat: (MotionPhotoBuilder.OutputFormat) -> Unit,
    onUpdateLongExposure: (MotionPhotoBuilder.LongExposureMode) -> Unit,
    onUpdateFilter: (MotionPhotoBuilder.CoverFilter) -> Unit,
    onUpdateWatermark: (Boolean) -> Unit,
    onStartConvert: () -> Unit,
    onReset: () -> Unit,
    onInspectFile: (File) -> Unit,
    onShareFile: (File) -> Unit
) {
    AnimatedContent(
        targetState = state,
        contentKey = { it::class.simpleName },
        transitionSpec = {
            fadeIn(animationSpec = tween(150)) togetherWith
                fadeOut(animationSpec = tween(120))
        },
        label = "studio_state_transition"
    ) { current ->
        when (current) {
            is MainViewModel.StudioState.Idle -> {
                StudioIdleHeroView(
                    onPickVideo = onPickVideo,
                    onPickBatchVideos = onPickBatchVideos
                )
            }

            is MainViewModel.StudioState.Editing -> {
                StudioEditorView(
                    state = current,
                    onPickCustomCover = onPickCustomCover,
                    onClearCustomCover = onClearCustomCover,
                    onPickReferenceExif = onPickReferenceExif,
                    onClearReferenceExif = onClearReferenceExif,
                    onUpdateCoverTime = onUpdateCoverTime,
                    onUpdateTrimRange = onUpdateTrimRange,
                    onUpdateAudio = onUpdateAudio,
                    onUpdateQuality = onUpdateQuality,
                    onUpdateFormat = onUpdateFormat,
                    onUpdateLongExposure = onUpdateLongExposure,
                    onUpdateFilter = onUpdateFilter,
                    onUpdateWatermark = onUpdateWatermark,
                    onStartConvert = onStartConvert,
                    onCancel = onReset
                )
            }

            is MainViewModel.StudioState.Converting -> {
                StudioConvertingView(
                    progress = current.progress,
                    stage = current.stage,
                    detailMessage = current.detailMessage
                )
            }

            is MainViewModel.StudioState.Success -> {
                StudioSuccessView(
                    result = current.result,
                    batchCount = current.batchCount,
                    onShare = { onShareFile(current.result.localFile) },
                    onInspect = { onInspectFile(current.result.localFile) },
                    onConvertAnother = {
                        onReset()
                        onPickVideo()
                    }
                )
            }

            is MainViewModel.StudioState.Error -> {
                StudioErrorView(
                    message = current.message,
                    onRetry = {
                        onReset()
                        onPickVideo()
                    },
                    onBack = onReset
                )
            }
        }
    }
}

@Composable
private fun StudioIdleHeroView(
    onPickVideo: () -> Unit,
    onPickBatchVideos: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ExpressiveMorphingBadge(
                    modifier = Modifier.size(136.dp),
                    lobes = 8f,
                    waveAmplitude = 0.10f,
                    isAnimating = false
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MotionPhotosOn,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "视频转动态照片",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "选择一段视频，自动生成可在手机相册中长按播放的动态照片。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .expressiveBounceClick(pressedScale = 0.95f) {
                            onPickVideo()
                        },
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VideoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "选择视频",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .expressiveBounceClick(pressedScale = 0.96f) {
                            onPickBatchVideos()
                        },
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "批量选择视频",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudioEditorView(
    state: MainViewModel.StudioState.Editing,
    onPickCustomCover: () -> Unit,
    onClearCustomCover: () -> Unit,
    onPickReferenceExif: () -> Unit,
    onClearReferenceExif: () -> Unit,
    onUpdateCoverTime: (Long) -> Unit,
    onUpdateTrimRange: (Long, Long) -> Unit,
    onUpdateAudio: (Boolean) -> Unit,
    onUpdateQuality: (Int) -> Unit,
    onUpdateFormat: (MotionPhotoBuilder.OutputFormat) -> Unit,
    onUpdateLongExposure: (MotionPhotoBuilder.LongExposureMode) -> Unit,
    onUpdateFilter: (MotionPhotoBuilder.CoverFilter) -> Unit,
    onUpdateWatermark: (Boolean) -> Unit,
    onStartConvert: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var previewMode by remember { mutableStateOf("VIDEO") }
    var isVideoPlaying by remember { mutableStateOf(true) }
    var showAdvancedOptions by remember { mutableStateOf(false) }
    var showFormatSheet by remember { mutableStateOf(false) }

    var localCoverTimeUs by remember(state.videoUri, state.coverTimeUs) {
        mutableFloatStateOf(state.coverTimeUs.toFloat())
    }
    var isDraggingCoverSlider by remember { mutableStateOf(false) }

    var localTrimStartMs by remember(state.videoUri, state.trimStartMs) {
        mutableFloatStateOf(state.trimStartMs.toFloat())
    }
    var localTrimEndMs by remember(state.videoUri, state.trimEndMs) {
        mutableFloatStateOf(state.trimEndMs.toFloat())
    }

    val exoPlayer = remember(state.videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(state.videoUri))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(state.includeAudio, exoPlayer) {
        exoPlayer.volume = if (state.includeAudio) 1f else 0f
    }

    LaunchedEffect(previewMode, isVideoPlaying, exoPlayer) {
        if (previewMode == "VIDEO" && isVideoPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    // 预缓存时间轴缩略图的 ImageBitmap，避免滑块拖动重组时每帧重复转换 Bitmap -> ImageBitmap
    val timelineImageBitmaps: List<ImageBitmap> = remember(state.timelineThumbnails) {
        state.timelineThumbnails.map { it.asImageBitmap() }
    }
    val coverPreviewImageBitmap: ImageBitmap? = remember(state.coverPreviewBitmap) {
        state.coverPreviewBitmap?.asImageBitmap()
    }

    // 使用 derivedStateOf 将滑块连续微秒值收敛为离散缩略图索引，大幅降低拖动时的预览重组频率
    val draggedThumbIndex by remember(timelineImageBitmaps.size, state.metadata.durationMs) {
        derivedStateOf {
            if (timelineImageBitmaps.isEmpty()) 0
            else {
                val ratio = (localCoverTimeUs / (state.metadata.durationMs * 1000f).coerceAtLeast(1f)).coerceIn(0f, 1f)
                (ratio * (timelineImageBitmaps.size - 1)).roundToInt()
                    .coerceIn(0, timelineImageBitmaps.lastIndex)
            }
        }
    }

    val displayedCoverImageBitmap: ImageBitmap? = if (isDraggingCoverSlider && timelineImageBitmaps.isNotEmpty()) {
        timelineImageBitmaps[draggedThumbIndex]
    } else {
        coverPreviewImageBitmap ?: timelineImageBitmaps.firstOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ExpressiveConnectedButtonGroup(
            items = PreviewModes,
            selectedItem = previewMode,
            onSelect = { mode -> previewMode = mode },
            labelProvider = { mode ->
                if (mode == "VIDEO") "视频预览" else "封面预览"
            }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black)
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            if (previewMode == "VIDEO") {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { view ->
                        if (view.player !== exoPlayer) {
                            view.player = exoPlayer
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .expressiveBounceClick(pressedScale = 0.99f) {
                            isVideoPlaying = !isVideoPlaying
                        }
                )
            } else {
                if (state.customCoverImageUri != null) {
                    AsyncImage(
                        model = state.customCoverImageUri,
                        contentDescription = "自定义封面",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (displayedCoverImageBitmap != null) {
                    Image(
                        bitmap = displayedCoverImageBitmap,
                        contentDescription = "封面预览",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = state.videoUri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (previewMode == "VIDEO") Icons.Rounded.MotionPhotosOn else Icons.Rounded.CameraEnhance,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "${state.metadata.displayWidth}×${state.metadata.displayHeight} · ${
                        String.format(Locale.US, "%.1fs", (localTrimEndMs - localTrimStartMs) / 1000f)
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (previewMode == "VIDEO") {
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .expressiveBounceClick { isVideoPlaying = !isVideoPlaying },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isVideoPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 封面帧位置滑块
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "封面位置",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f 秒", localCoverTimeUs / 1_000_000f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (timelineImageBitmaps.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        timelineImageBitmaps.forEachIndexed { index, imgBitmap ->
                            val count = timelineImageBitmaps.size
                            val frameTimeUs = if (count > 1) {
                                (state.metadata.durationMs * 1000L * index) / (count - 1)
                            } else 0L
                            Image(
                                bitmap = imgBitmap,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .expressiveBounceClick {
                                        previewMode = "COVER"
                                        localCoverTimeUs = frameTimeUs.toFloat()
                                        onUpdateCoverTime(frameTimeUs)
                                    }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                val minUs = (localTrimStartMs * 1000f)
                val maxUs = (localTrimEndMs * 1000f).coerceAtLeast(minUs + 1000f)

                Slider(
                    value = localCoverTimeUs.coerceIn(minUs, maxUs),
                    onValueChange = { newUs ->
                        isDraggingCoverSlider = true
                        if (previewMode != "COVER") {
                            previewMode = "COVER"
                        }
                        localCoverTimeUs = newUs
                    },
                    onValueChangeFinished = {
                        isDraggingCoverSlider = false
                        onUpdateCoverTime(localCoverTimeUs.toLong())
                    },
                    valueRange = minUs..maxUs
                )
            }
        }

        // 主操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(0.32f)
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "重选",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                modifier = Modifier
                    .weight(0.68f)
                    .height(56.dp)
                    .expressiveBounceClick(pressedScale = 0.95f) {
                        onStartConvert()
                    },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "生成动态照片",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // 更多设置折叠栏（默认收起）
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .expressiveBounceClick(pressedScale = 0.98f) {
                    showAdvancedOptions = !showAdvancedOptions
                },
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                    Column {
                        Text(
                            text = "更多设置",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "截取片段、替换封面、滤镜、相册兼容模式",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (showAdvancedOptions) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(
            visible = showAdvancedOptions,
            enter = fadeIn(tween(150)) + expandVertically(tween(170)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(140))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCut,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                                Text(
                                    text = "截取片段",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.1fs", localTrimStartMs / 1000f)} — ${
                                    String.format(Locale.US, "%.1fs", localTrimEndMs / 1000f)
                                }",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        val totalDurationFloat = state.metadata.durationMs.coerceAtLeast(500L).toFloat()
                        RangeSlider(
                            value = localTrimStartMs..localTrimEndMs,
                            onValueChange = { range ->
                                localTrimStartMs = range.start
                                localTrimEndMs = range.endInclusive
                            },
                            onValueChangeFinished = {
                                onUpdateTrimRange(localTrimStartMs.toLong(), localTrimEndMs.toLong())
                            },
                            valueRange = 0f..totalDurationFloat
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.includeAudio) Icons.Rounded.MusicNote else Icons.Rounded.MusicOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "保留视频声音",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Switch(
                                checked = state.includeAudio,
                                onCheckedChange = onUpdateAudio
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "封面图片质量",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ExpressiveConnectedButtonGroup(
                                items = QualityOptions,
                                selectedItem = state.jpegQuality,
                                onSelect = onUpdateQuality,
                                labelProvider = { q ->
                                    when (q) {
                                        88 -> "标准"
                                        95 -> "高清"
                                        else -> "原画"
                                    }
                                }
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "封面与画面效果",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "替换封面图",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = state.customCoverName?.let { "已选：$it" }
                                        ?: "从相册选一张照片作为封面",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (state.customCoverImageUri != null) {
                                OutlinedButton(onClick = onClearCustomCover, shape = RoundedCornerShape(22.dp)) {
                                    Text("还原")
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = {
                                        previewMode = "COVER"
                                        onPickCustomCover()
                                    },
                                    shape = RoundedCornerShape(22.dp)
                                ) {
                                    Text("选择照片")
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "导入相机参数",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = state.referenceExifName?.let { "已选：$it" }
                                        ?: "从本机拍的照片复制机型信息，提高相册识别率",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (state.referenceExifImageUri != null) {
                                OutlinedButton(onClick = onClearReferenceExif, shape = RoundedCornerShape(22.dp)) {
                                    Text("清除")
                                }
                            } else {
                                FilledTonalButton(onClick = onPickReferenceExif, shape = RoundedCornerShape(22.dp)) {
                                    Text("选择样张")
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "封面效果",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ExpressiveConnectedButtonGroup(
                                items = MotionPhotoBuilder.LongExposureMode.Entries,
                                selectedItem = state.longExposureMode,
                                onSelect = { mode ->
                                    previewMode = "COVER"
                                    onUpdateLongExposure(mode)
                                },
                                labelProvider = { it.title }
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "滤镜",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ExpressiveConnectedButtonGroup(
                                items = MotionPhotoBuilder.CoverFilter.Entries,
                                selectedItem = state.coverFilter,
                                onSelect = { filter ->
                                    previewMode = "COVER"
                                    onUpdateFilter(filter)
                                },
                                labelProvider = { it.title }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "底部相机水印",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Switch(
                                checked = state.watermarkEnabled,
                                onCheckedChange = onUpdateWatermark
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .expressiveBounceClick { showFormatSheet = true },
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "兼容模式：${state.selectedFormat.displayName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = state.selectedFormat.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "更改",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showFormatSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFormatSheet = false },
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "选择兼容模式",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                MotionPhotoBuilder.OutputFormat.Entries.forEach { fmt ->
                    val selected = fmt == state.selectedFormat
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .expressiveBounceClick {
                                onUpdateFormat(fmt)
                                showFormatSheet = false
                            },
                        shape = RoundedCornerShape(22.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fmt.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = fmt.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun StudioConvertingView(
    progress: Float,
    stage: MotionPhotoBuilder.ConversionProgress.Stage,
    detailMessage: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            ExpressiveMorphingBadge(
                modifier = Modifier.size(148.dp),
                lobes = 8f,
                waveAmplitude = 0.10f,
                isAnimating = true
            ) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            ExpressiveWavyCircularHalo(
                progress = progress,
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 7.dp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = when (stage) {
                MotionPhotoBuilder.ConversionProgress.Stage.EXTRACTING_FRAME -> "正在生成封面..."
                MotionPhotoBuilder.ConversionProgress.Stage.EXTRACTING_VIDEO -> "正在处理视频..."
                MotionPhotoBuilder.ConversionProgress.Stage.WRITING_METADATA -> "正在写入信息..."
                MotionPhotoBuilder.ConversionProgress.Stage.COMBINING -> "正在合成文件..."
                MotionPhotoBuilder.ConversionProgress.Stage.SAVING_TO_GALLERY -> "正在保存到相册..."
                MotionPhotoBuilder.ConversionProgress.Stage.COMPLETE -> "已完成"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = detailMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        ExpressiveWavyLinearProgress(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun StudioSuccessView(
    result: MotionPhotoBuilder.ConversionResult,
    batchCount: Int,
    onShare: () -> Unit,
    onInspect: () -> Unit,
    onConvertAnother: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (batchCount > 1) {
                            "已保存 $batchCount 张动态照片到相册"
                        } else {
                            "已保存到相册"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = result.galleryDisplayPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        LiveMotionPhotoPlayerCard(
            coverImageModel = result.localFile,
            videoFileOrUri = result.localFile,
            resolutionLabel = result.resolution,
            durationMs = result.videoDurationMs,
            autoPlayInitially = false
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = onInspect,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Verified, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("查看详情", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onShare,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("分享", fontWeight = FontWeight.Bold)
            }
        }

        OutlinedButton(
            onClick = onConvertAnother,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("继续制作", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun StudioErrorView(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ExpressiveMorphingBadge(
            modifier = Modifier.size(120.dp),
            lobes = 5f,
            primaryColor = MaterialTheme.colorScheme.errorContainer,
            secondaryColor = MaterialTheme.colorScheme.errorContainer
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "生成失败",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.height(50.dp),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text("返回")
            }
            Button(
                onClick = onRetry,
                modifier = Modifier.height(50.dp),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text("重试")
            }
        }
    }
}

@Composable
private fun InspectorTabContent(
    state: MainViewModel.InspectorState,
    onPickImage: () -> Unit,
    onExportVideo: () -> Unit,
    onExportStill: () -> Unit,
    onRepairMotionPhoto: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showRawXmp by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "照片解析与提取",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "选择一张动态照片，可单独保存其中的视频、静态图片，或修复相册无法识别的动态照片。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onPickImage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("选择照片", fontWeight = FontWeight.Bold)
                }
            }
        }

        when (state) {
            is MainViewModel.InspectorState.Idle -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "未选择照片",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            is MainViewModel.InspectorState.Inspecting -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ExpressiveWavyLinearProgress(progress = 0.65f)
                }
            }

            is MainViewModel.InspectorState.Error -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }

            is MainViewModel.InspectorState.ResultReady -> {
                val report = state.report

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = if (report.isValidMotionPhoto) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (report.isValidMotionPhoto) Icons.Rounded.Verified else Icons.Rounded.Info,
                                contentDescription = null,
                                tint = if (report.isValidMotionPhoto) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                            Text(
                                text = if (report.isValidMotionPhoto) {
                                    "包含完整动态视频"
                                } else if (report.canBeRepaired) {
                                    "包含视频，但信息不完整（可修复）"
                                } else {
                                    "普通静态照片"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (report.extractedVideoFile != null) {
                    LiveMotionPhotoPlayerCard(
                        coverImageModel = state.sourceUriOrFile,
                        videoFileOrUri = report.extractedVideoFile,
                        resolutionLabel = report.MakeModelSummary,
                        durationMs = 2500L,
                        autoPlayInitially = false
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.50f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = onExportVideo,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text("保存视频", fontWeight = FontWeight.Bold)
                                }

                                FilledTonalButton(
                                    onClick = onExportStill,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text("保存图片", fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = onRepairMotionPhoto,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("修复并重新保存到相册", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "文件信息",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        MetricRow("文件名", report.fileName)
                        MetricRow("总大小", formatBytes(report.totalFileSize))
                        MetricRow("内嵌视频大小", formatBytes(report.actualMp4Length))

                        if (report.rawXmpSnippet.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            FilledTonalButton(
                                onClick = { showRawXmp = !showRawXmp },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.Code, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (showRawXmp) "收起 XMP 信息" else "查看 XMP 信息")
                            }

                            AnimatedVisibility(visible = showRawXmp) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    shape = RoundedCornerShape(22.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = report.rawXmpSnippet,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        ),
                                        modifier = Modifier
                                            .horizontalScroll(rememberScrollState())
                                            .padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * 作品历史记录页（采用高性能 LazyColumn + 预计算元数据 + 缩略图精确采样，消除滚动掉帧）
 */
@Composable
private fun GalleryTabContent(
    historyItems: List<MainViewModel.HistoryPhotoItem>,
    onInspectFile: (File) -> Unit,
    onShareFile: (File) -> Unit,
    onDeleteFile: (File) -> Unit,
    onPickVideo: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header", contentType = "header") {
            Text(
                text = "历史记录 (${historyItems.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (historyItems.isEmpty()) {
            item(key = "empty_state", contentType = "empty") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoAlbum,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "暂无记录",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        FilledTonalButton(
                            onClick = onPickVideo,
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Text("去制作")
                        }
                    }
                }
            }
        } else {
            items(
                items = historyItems,
                key = { it.key },
                contentType = { "history_item" }
            ) { item ->
                val thumbRequest = remember(context, item.key) {
                    ImageRequest.Builder(context)
                        .data(item.file)
                        .size(204, 204)
                        .precision(Precision.INEXACT)
                        .crossfade(false)
                        .build()
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = thumbRequest,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .expressiveBounceClick { onInspectFile(item.file) }
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.formattedMeta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row {
                            IconButton(onClick = { onInspectFile(item.file) }) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "查看",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { onShareFile(item.file) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "分享",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                            IconButton(onClick = { onDeleteFile(item.file) }) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "bottom_spacer", contentType = "spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemeCustomizationSheet(
    settings: MainViewModel.ThemeSettings,
    onSelectPalette: (ExpressivePalette) -> Unit,
    onToggleDynamic: (Boolean) -> Unit,
    onOpenLiquidGlass: () -> Unit = {},
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "主题配色",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "跟随系统壁纸颜色",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "使用系统动态配色",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.useDynamicColor,
                        onCheckedChange = onToggleDynamic
                    )
                }
            }
        }

        ExpressivePalette.Entries.forEach { palette ->
            val selected = !settings.useDynamicColor && settings.palette == palette
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .expressiveBounceClick {
                        onSelectPalette(palette)
                        onDismiss()
                    },
                shape = RoundedCornerShape(22.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(palette.seedColor, palette.accentPreview)
                                )
                            )
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = palette.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .expressiveBounceClick { onOpenLiquidGlass() },
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "底部栏液态玻璃调节",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "自由调节模糊度、折射曲率、色散与透光率",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LiquidGlassTunerSheet(
    config: MainViewModel.LiquidGlassConfig,
    onUpdateConfig: ((MainViewModel.LiquidGlassConfig) -> MainViewModel.LiquidGlassConfig) -> Unit,
    onSelectPreset: (MainViewModel.LiquidGlassPreset) -> Unit,
    onResetDefault: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "底部栏液态玻璃",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "基于开源 Kyant0/AndroidLiquidGlass 光学折射与色散着色器",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = config.enabled,
                onCheckedChange = { enabled ->
                    onUpdateConfig { it.copy(enabled = enabled) }
                }
            )
        }

        // 预设风格选择
        Text(
            text = "光学预设",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MainViewModel.LiquidGlassPreset.Entries.forEach { preset ->
                val isSelected = config.preset == preset && config.enabled
                Surface(
                    modifier = Modifier.expressiveBounceClick(pressedScale = 0.94f) {
                        onSelectPreset(preset)
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = preset.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 1. 模糊半径
        LiquidGlassSliderItem(
            title = "背景模糊半径",
            valueText = "${config.blurRadiusDp.roundToInt()} dp",
            value = config.blurRadiusDp,
            valueRange = 0f..36f,
            enabled = config.enabled,
            onValueChange = { v ->
                onUpdateConfig { it.copy(blurRadiusDp = v, preset = null) }
            }
        )

        // 2. 透镜折射曲率
        LiquidGlassSliderItem(
            title = "透镜折射曲率",
            valueText = "${(config.refractionStrength * 100).roundToInt()}%",
            value = config.refractionStrength,
            valueRange = 0f..1f,
            enabled = config.enabled,
            onValueChange = { v ->
                onUpdateConfig { it.copy(refractionStrength = v, preset = null) }
            }
        )

        // 3. 边缘棱镜色散
        LiquidGlassSliderItem(
            title = "边缘棱镜色散",
            valueText = "${(config.dispersionStrength * 100).roundToInt()}%",
            value = config.dispersionStrength,
            valueRange = 0f..1f,
            enabled = config.enabled,
            onValueChange = { v ->
                onUpdateConfig { it.copy(dispersionStrength = v, preset = null) }
            }
        )

        // 4. 玻璃漫射浓度
        LiquidGlassSliderItem(
            title = "玻璃透光浓度",
            valueText = "${(config.tintAlpha * 100).roundToInt()}%",
            value = config.tintAlpha,
            valueRange = 0.05f..0.85f,
            enabled = config.enabled,
            onValueChange = { v ->
                onUpdateConfig { it.copy(tintAlpha = v, preset = null) }
            }
        )

        // 5. 菲涅尔边缘高光
        LiquidGlassSliderItem(
            title = "菲涅尔边缘高光",
            valueText = "${(config.specularStrength * 100).roundToInt()}%",
            value = config.specularStrength,
            valueRange = 0f..1f,
            enabled = config.enabled,
            onValueChange = { v ->
                onUpdateConfig { it.copy(specularStrength = v, preset = null) }
            }
        )

        // 6. 胶囊圆角弧度
        LiquidGlassSliderItem(
            title = "胶囊圆角弧度",
            valueText = "${config.cornerRadiusDp.roundToInt()} dp",
            value = config.cornerRadiusDp,
            valueRange = 16f..32f,
            enabled = config.enabled,
            onValueChange = { v ->
                onUpdateConfig { it.copy(cornerRadiusDp = v, preset = null) }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = onResetDefault,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("恢复默认参数")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun LiquidGlassSliderItem(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.2f MB", mb)
}

private fun shareMotionPhoto(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享动态照片"))
    } catch (_: Exception) {
    }
}
