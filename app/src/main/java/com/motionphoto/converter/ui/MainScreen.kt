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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraEnhance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VideoLibrary
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.motionphoto.converter.core.MotionPhotoBuilder
import com.motionphoto.converter.ui.components.AppTab
import com.motionphoto.converter.ui.components.ExpressiveConnectedButtonGroup
import com.motionphoto.converter.ui.components.ExpressiveFloatingToolbar
import com.motionphoto.converter.ui.components.ExpressiveMorphingBadge
import com.motionphoto.converter.ui.components.ExpressiveWavyCircularHalo
import com.motionphoto.converter.ui.components.ExpressiveWavyLinearProgress
import com.motionphoto.converter.ui.components.LiveMotionPhotoPlayerCard
import com.motionphoto.converter.ui.theme.ExpressiveMotion
import com.motionphoto.converter.ui.theme.ExpressivePalette
import com.motionphoto.converter.ui.theme.expressiveBounceClick
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val studioState by viewModel.studioState.collectAsState()
    val inspectorState by viewModel.inspectorState.collectAsState()
    val historyFiles by viewModel.historyFiles.collectAsState()
    val themeSettings by viewModel.themeSettings.collectAsState()
    val statusBanner by viewModel.statusBannerMessage.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val isDark = themeSettings.darkModeOverride ?: systemDark

    var showPaletteSheet by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    // 1. 单视频选择器
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

    // 2. 批量多视频选择器 (v3.0 新增：Batch Queue)
    val pickMultipleVideos = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(12)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.startBatchConversion(uris)
        }
    }

    // 3. 外部高清静态封面选择器 (v3.0 新增：Photo + Video Muxer)
    val pickCustomCoverPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setCustomCoverImage(uri)
        }
    }

    // 4. 本机相机参考样张 EXIF 克隆选择器 (v3.0 新增：copy-img-meta)
    val pickReferenceExifPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setReferenceExifImage(uri)
        }
    }

    // 5. 实况检验器选图片
    val pickImageForInspector = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.inspectMotionPhotoUri(uri)
        }
    }

    val launchVideoPicker = {
        try {
            pickVideoVisualMedia.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            )
        } catch (_: Exception) {
            pickVideoFallback.launch("video/*")
        }
    }

    val launchBatchVideoPicker = {
        try {
            pickMultipleVideos.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            )
        } catch (_: Exception) {
            pickVideoFallback.launch("video/*")
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
                    onSelectTab = { viewModel.selectTab(it) },
                    onQuickPickVideo = launchVideoPicker
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(240)) +
                        scaleIn(initialScale = 0.94f, animationSpec = ExpressiveMotion.defaultSpatial()))
                        .togetherWith(
                            fadeOut(animationSpec = tween(180)) +
                                scaleOut(targetScale = 0.96f, animationSpec = tween(180))
                        )
                },
                label = "tab_content_transition"
            ) { tab ->
                when (tab) {
                    AppTab.STUDIO -> {
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
                            onInspectFile = { file ->
                                viewModel.inspectMotionPhotoFile(file)
                            },
                            onShareFile = { file ->
                                shareMotionPhoto(context, file)
                            }
                        )
                    }

                    AppTab.INSPECTOR -> {
                        InspectorTabContent(
                            state = inspectorState,
                            onPickImage = { pickImageForInspector.launch("image/*") },
                            onExportVideo = viewModel::exportInspectedVideo,
                            onExportStill = viewModel::exportInspectedStill,
                            onRepairMotionPhoto = viewModel::repairInspectedMotionPhoto
                        )
                    }

                    AppTab.GALLERY -> {
                        GalleryAndGuideTabContent(
                            historyFiles = historyFiles,
                            onInspectFile = { file -> viewModel.inspectMotionPhotoFile(file) },
                            onShareFile = { file -> shareMotionPhoto(context, file) },
                            onDeleteFile = { file -> viewModel.deleteHistoryFile(file) },
                            onPickVideo = launchVideoPicker
                        )
                    }
                }
            }

            // 顶部浮动状态胶囊提示
            AnimatedVisibility(
                visible = statusBanner != null,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f),
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
                onSelectPalette = { viewModel.updatePalette(it) },
                onToggleDynamic = { viewModel.toggleDynamicColor(it) },
                onDismiss = { showPaletteSheet = false }
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MotionPhotosOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Motion Studio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "v3.0 Release",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = when (currentTab) {
                        AppTab.STUDIO -> "图影混流 · 长曝光 · 批量实况合成工坊"
                        AppTab.INSPECTOR -> "XMP 检验 · 无损拆解 MP4/JPG · 一键修复"
                        AppTab.GALLERY -> "本机已生成实况作品库与开源协议矩阵"
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
                    .size(42.dp)
                    .expressiveBounceClick { onOpenThemeSheet() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "主题色彩",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .expressiveBounceClick { onToggleDark() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "深浅色模式",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
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
        transitionSpec = {
            (fadeIn(animationSpec = tween(260)) +
                scaleIn(initialScale = 0.92f, animationSpec = ExpressiveMotion.defaultSpatial()))
                .togetherWith(
                    fadeOut(animationSpec = tween(180)) +
                        scaleOut(targetScale = 0.95f, animationSpec = tween(180))
                )
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
                    detailMessage = current.detailMessage,
                    format = current.format
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
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(36.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ExpressiveMorphingBadge(
                    modifier = Modifier.size(160.dp),
                    lobes = 8f,
                    waveAmplitude = 0.11f
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MotionPhotosOn,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "LIVE · v3.0",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "全能 Android 实况照片工坊",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "融合 MotionPhoto2、libmphoto 与 live-photo-conv 三大开源项目精髓：支持视频转实况、外部高清图+视频混流、动态长曝光封面、本机样张 EXIF 克隆、批量队列以及无损拆解修复。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    ExpressiveFeaturePill("图影混流合成", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    ExpressiveFeaturePill("长曝光流光封面", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                    ExpressiveFeaturePill("样张 EXIF 克隆", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 主操作：单视频精修转实况
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .expressiveBounceClick(pressedScale = 0.94f) {
                            onPickVideo()
                        },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "选择视频精修 / 图影混流创作",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 副操作：多视频批量一键合成队列
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .expressiveBounceClick(pressedScale = 0.95f) {
                            onPickBatchVideos()
                        },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "批量选择多段视频一键队列转实况",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BrandSupportMiniCard(
                title = "图影混流 & EXIF 克隆",
                subtitle = "支持外部高像素 JPG + MP4 混流及相机样张 EXIF 完整移植",
                badge = "MotionPhoto2",
                modifier = Modifier.weight(1f)
            )
            BrandSupportMiniCard(
                title = "无损拆解 & 实况修复",
                subtitle = "一键导出独立 MP4 / JPG 或修复损坏的 XMP 偏移量",
                badge = "libmphoto",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun ExpressiveFeaturePill(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = CircleShape,
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun BrandSupportMiniCard(
    title: String,
    subtitle: String,
    badge: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
    var showFormatSheet by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ExpressiveConnectedButtonGroup(
            items = listOf("VIDEO", "COVER"),
            selectedItem = previewMode,
            onSelect = { mode -> previewMode = mode },
            labelProvider = { mode ->
                if (mode == "VIDEO") "动态视频实时预览" else "静态封面帧定格预览"
            }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(30.dp))
                .background(Color.Black)
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(30.dp)
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
                    update = { view -> view.player = exoPlayer },
                    modifier = Modifier
                        .fillMaxSize()
                        .expressiveBounceClick(pressedScale = 0.98f) {
                            isVideoPlaying = !isVideoPlaying
                        }
                )
            } else {
                if (state.customCoverImageUri != null) {
                    AsyncImage(
                        model = state.customCoverImageUri,
                        contentDescription = "外部高清封面",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (state.coverPreviewBitmap != null) {
                    Image(
                        bitmap = state.coverPreviewBitmap.asImageBitmap(),
                        contentDescription = "封面帧预览",
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
                    imageVector = if (previewMode == "VIDEO") Icons.Default.MotionPhotosOn else Icons.Default.CameraEnhance,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "${state.metadata.displayWidth}×${state.metadata.displayHeight} · ${
                        String.format("%.1fs", (state.trimEndMs - state.trimStartMs) / 1000f)
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
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = when {
                            state.customCoverImageUri != null -> "外部高清主图混流"
                            state.longExposureMode != MotionPhotoBuilder.LongExposureMode.NONE -> state.longExposureMode.title
                            else -> "封面 @ ${state.coverTimeUs / 1000L} ms"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                if (previewMode == "VIDEO") {
                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .expressiveBounceClick { isVideoPlaying = !isVideoPlaying },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // 封面帧时间轴胶片精修卡
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
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
                            imageVector = Icons.Default.CameraEnhance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "静态封面帧时间点与长曝光堆栈",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = String.format("%.2f 秒", state.coverTimeUs / 1_000_000f),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (state.timelineThumbnails.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        state.timelineThumbnails.forEachIndexed { index, bmp ->
                            val count = state.timelineThumbnails.size
                            val frameTimeUs = if (count > 1) {
                                (state.metadata.durationMs * 1000L * index) / (count - 1)
                            } else 0L
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .expressiveBounceClick {
                                        previewMode = "COVER"
                                        onUpdateCoverTime(frameTimeUs)
                                    }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val minUs = (state.trimStartMs * 1000L).toFloat()
                val maxUs = (state.trimEndMs * 1000L).coerceAtLeast(state.trimStartMs * 1000L + 1000L).toFloat()

                Slider(
                    value = state.coverTimeUs.toFloat().coerceIn(minUs, maxUs),
                    onValueChange = { newUs ->
                        previewMode = "COVER"
                        onUpdateCoverTime(newUs.toLong())
                    },
                    valueRange = minUs..maxUs
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 动态长曝光模式选择 (参考 wszqkzqk/live-photo-conv --long-exposure)
                Text(
                    text = "封面帧合成模式 (--long-exposure 多帧堆栈)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                ExpressiveConnectedButtonGroup(
                    items = MotionPhotoBuilder.LongExposureMode.values().toList(),
                    selectedItem = state.longExposureMode,
                    onSelect = { mode ->
                        previewMode = "COVER"
                        onUpdateLongExposure(mode)
                    },
                    labelProvider = { it.title }
                )
            }
        }

        // 新增：开源进阶工坊卡片 (图影混流 + 样张 EXIF 克隆 + 滤镜与机型水印)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "开源生态进阶工坊 (图影混流 · EXIF 克隆 · 滤镜水印)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // 1. 外部高清静态主图混流 (PetrVys/MotionPhoto2 & libmphoto)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "图影混流：外部高清静态主图",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.customCoverName?.let { "已选外部主图：$it" }
                                ?: "可选任意高像素 JPG/PNG/HEIC 照片与当前视频合成为实况",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (state.customCoverImageUri != null) {
                        OutlinedButton(
                            onClick = onClearCustomCover,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("还原视频帧")
                        }
                    } else {
                        FilledTonalButton(
                            onClick = {
                                previewMode = "COVER"
                                onPickCustomCover()
                            },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("选外部照片")
                        }
                    }
                }

                // 2. 本机相机参考样张 EXIF 克隆 (wszqkzqk/live-photo-conv copy-img-meta)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "本机样张 EXIF 克隆 (copy-img-meta)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.referenceExifName?.let { "已克隆样张：$it" }
                                ?: "从本机相机拍摄的照片复制 25+ 项 EXIF 参数，100% 绕过厂商相册校验",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (state.referenceExifImageUri != null) {
                        OutlinedButton(
                            onClick = onClearReferenceExif,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("清除样张")
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onPickReferenceExif,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("选本机样张")
                        }
                    }
                }

                // 3. 封面调色滤镜
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "封面色彩风格滤镜",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ExpressiveConnectedButtonGroup(
                        items = MotionPhotoBuilder.CoverFilter.values().toList(),
                        selectedItem = state.coverFilter,
                        onSelect = { filter ->
                            previewMode = "COVER"
                            onUpdateFilter(filter)
                        },
                        labelProvider = { it.title }
                    )
                }

                // 4. 光学机型边框铭牌水印开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "底部旗舰影像铭牌水印 (Leica / Hasselblad / Pixel)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "在静态封面底部叠加所选协议对应的相机型号与光学参数横幅",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.watermarkEnabled,
                        onCheckedChange = onUpdateWatermark
                    )
                }
            }
        }

        // 视频裁剪区间 & 原声/画质控制卡片
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "实况片段截取",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${String.format("%.1fs", state.trimStartMs / 1000f)} — ${
                            String.format("%.1fs", state.trimEndMs / 1000f)
                        } (共 ${String.format("%.1fs", (state.trimEndMs - state.trimStartMs) / 1000f)})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val totalDurationFloat = state.metadata.durationMs.coerceAtLeast(500L).toFloat()
                RangeSlider(
                    value = state.trimStartMs.toFloat()..state.trimEndMs.toFloat(),
                    onValueChange = { range ->
                        onUpdateTrimRange(range.start.toLong(), range.endInclusive.toLong())
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
                            imageVector = if (state.includeAudio) Icons.Default.MusicNote else Icons.Default.MusicOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Column {
                            Text(
                                text = "保留视频环境原声 (AAC)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (state.includeAudio) "长按实况播放时同步还原现场声音" else "已设为静音实况照片",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = state.includeAudio,
                        onCheckedChange = onUpdateAudio
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "主图 JPEG 编码品质",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ExpressiveConnectedButtonGroup(
                        items = listOf(88, 95, 100),
                        selectedItem = state.jpegQuality,
                        onSelect = onUpdateQuality,
                        labelProvider = { q ->
                            when (q) {
                                88 -> "轻量 88%"
                                95 -> "高画质 95%"
                                else -> "无损级 100%"
                            }
                        }
                    )
                }
            }
        }

        // 目标厂商 XMP 协议选择器卡片
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .expressiveBounceClick { showFormatSheet = true },
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "XMP 实况容器协议",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = state.selectedFormat.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.selectedFormat.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = state.selectedFormat.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "切换",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 底部合成与取消双操作栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(0.38f)
                    .height(60.dp),
                shape = RoundedCornerShape(22.dp)
            ) {
                Text(
                    text = "重选",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                modifier = Modifier
                    .weight(0.62f)
                    .height(60.dp)
                    .expressiveBounceClick(pressedScale = 0.94f) {
                        onStartConvert()
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
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "合成实况照片",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
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
                    text = "选择目标设备 XMP 实况格式",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "所有格式均以符合 Android Motion Photo 1.0 规范的 *MP.jpg 命名并保存至系统相册 DCIM/Camera。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                MotionPhotoBuilder.OutputFormat.values().forEach { fmt ->
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = fmt.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = fmt.badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${fmt.subtitle} · ${fmt.modelTag}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = fmt.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
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
    detailMessage: String,
    format: MotionPhotoBuilder.OutputFormat
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            ExpressiveMorphingBadge(
                modifier = Modifier.size(165.dp),
                lobes = when (stage) {
                    MotionPhotoBuilder.ConversionProgress.Stage.EXTRACTING_FRAME -> 6f
                    MotionPhotoBuilder.ConversionProgress.Stage.EXTRACTING_VIDEO -> 8f
                    MotionPhotoBuilder.ConversionProgress.Stage.WRITING_METADATA -> 10f
                    MotionPhotoBuilder.ConversionProgress.Stage.COMBINING -> 12f
                    else -> 8f
                },
                waveAmplitude = 0.13f
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = format.filePrefix,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }

            ExpressiveWavyCircularHalo(
                progress = progress,
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 8.dp
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = when (stage) {
                MotionPhotoBuilder.ConversionProgress.Stage.EXTRACTING_FRAME -> "正在处理高清封面并写入相机 EXIF"
                MotionPhotoBuilder.ConversionProgress.Stage.EXTRACTING_VIDEO -> "正在无损重封装 MP4 动态轨道"
                MotionPhotoBuilder.ConversionProgress.Stage.WRITING_METADATA -> "正在注入标准 XMP 容器目录"
                MotionPhotoBuilder.ConversionProgress.Stage.COMBINING -> "正在拼接 JPEG 与尾部 MP4 流"
                MotionPhotoBuilder.ConversionProgress.Stage.SAVING_TO_GALLERY -> "正在同步至系统相册 DCIM/Camera"
                MotionPhotoBuilder.ConversionProgress.Stage.COMPLETE -> "实况照片合成完成！"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = detailMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

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
            shape = RoundedCornerShape(28.dp),
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
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (batchCount > 1) {
                            "批量合成成功！共导出 $batchCount 张实况照片"
                        } else {
                            "实况照片已写入系统相册！"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "路径：${result.galleryDisplayPath}",
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
            autoPlayInitially = true
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Motion Photo 容器封装指标",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                MetricRow("输出文件名", result.localFile.name)
                MetricRow("协议标准", "${result.format.displayName} (${result.format.badge})")
                MetricRow(
                    "封面来源",
                    when {
                        result.usedCustomCover -> "外部高清图片混流 (Photo + Video Muxer)"
                        result.longExposureMode != MotionPhotoBuilder.LongExposureMode.NONE -> result.longExposureMode.title
                        else -> "视频关键帧提取 (@ ${result.coverTimestampUs / 1000L} ms)"
                    }
                )
                if (result.clonedReferenceExif) {
                    MetricRow("EXIF 增强", "已完整克隆本机相机样张 EXIF 参数")
                }
                MetricRow(
                    "文件结构分布",
                    "JPEG 主图 ${formatBytes(result.jpegSizeBytes)} + 尾部 MP4 ${formatBytes(result.videoSizeBytes)}"
                )
                MetricRow("总文件大小", formatBytes(result.totalSizeBytes))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = onInspect,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(imageVector = Icons.Default.Verified, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("检验与无损拆解", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onShare,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("分享实况照片", fontWeight = FontWeight.Bold)
            }
        }

        OutlinedButton(
            onClick = onConvertAnother,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("继续选择另一个视频创作", fontWeight = FontWeight.Bold)
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
            modifier = Modifier.size(130.dp),
            lobes = 5f,
            primaryColor = MaterialTheme.colorScheme.errorContainer,
            secondaryColor = MaterialTheme.colorScheme.errorContainer
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(54.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "实况合成遇到问题",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("返回工作台")
            }
            Button(
                onClick = onRetry,
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("重新选择视频")
            }
        }
    }
}

/**
 * Tab 2：实况检验器、无损拆解器 (Demuxer) 与损坏实况一键修复器 (live-photo-repair)
 */
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
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "XMP 检验 · 无损拆解 · 损坏修复",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "集成 libmphoto 与 live-photo-conv 拆解修复能力：可检验任意实况照片、提取内嵌 MP4 到相册、提取纯净静态 JPG，或一键修复损坏的实况元数据。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onPickImage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("从相册选择照片进行检验 / 拆解 / 修复", fontWeight = FontWeight.Bold)
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
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "尚未加载待检照片",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "挑选任意实况照片即可在此回放实况视频、单独导出 MP4 视频流，或修复无法被相册识别的动态照片。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
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
                    shape = RoundedCornerShape(26.dp),
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
                                imageVector = if (report.isValidMotionPhoto) Icons.Default.Verified else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (report.isValidMotionPhoto) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                            Text(
                                text = if (report.isValidMotionPhoto) {
                                    "检验通过：有效的 Android 实况照片"
                                } else if (report.canBeRepaired) {
                                    "检测到内嵌视频但 XMP 元数据异常（可一键修复）"
                                } else {
                                    "普通静态 JPEG（未检出内嵌视频轨道）"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        if (report.detectedStandards.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                report.detectedStandards.forEach { std ->
                                    ExpressiveFeaturePill(
                                        text = std,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                if (report.extractedVideoFile != null) {
                    LiveMotionPhotoPlayerCard(
                        coverImageModel = state.sourceUriOrFile,
                        videoFileOrUri = report.extractedVideoFile,
                        resolutionLabel = report.MakeModelSummary,
                        durationMs = 2500L,
                        autoPlayInitially = true
                    )

                    // v3.0 新增：无损拆解 (Demuxer) 与损坏实况一键修复面板
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "无损拆解导出 & 实况修复工具箱 (libmphoto / live-photo-conv)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = onExportVideo,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("提取视频 (.mp4)", fontWeight = FontWeight.Bold)
                                }

                                FilledTonalButton(
                                    onClick = onExportStill,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PhotoAlbum, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("提取主图 (.jpg)", fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = onRepairMotionPhoto,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("一键重建全兼容 XMP 并修复导出至相册", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "容器偏移量与 XMP 诊断明细",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        MetricRow("文件名", report.fileName)
                        MetricRow("EXIF 相机信息", report.MakeModelSummary)
                        MetricRow("文件总大小", formatBytes(report.totalFileSize))
                        MetricRow("JPEG 图像段大小", formatBytes(report.jpegPortionSize))
                        MetricRow("MP4 起始偏移量", if (report.actualMp4Offset > 0) "${report.actualMp4Offset} bytes" else "未检出")
                        MetricRow("实际内嵌 MP4 长度", formatBytes(report.actualMp4Length))
                        MetricRow("XMP 声明视频长度", formatBytes(report.declaredVideoLength))
                        MetricRow("封面帧时间戳", "${report.presentationTimestampUs} μs")

                        Spacer(modifier = Modifier.height(6.dp))
                        report.diagnosticNotes.forEach { note ->
                            Text(
                                text = "• $note",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (report.rawXmpSnippet.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FilledTonalButton(
                                onClick = { showRawXmp = !showRawXmp },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Code, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (showRawXmp) "收起原始 XMP RDF 报文" else "查看原始 XMP RDF 报文")
                            }

                            AnimatedVisibility(visible = showRawXmp) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = report.rawXmpSnippet,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
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

@Composable
private fun GalleryAndGuideTabContent(
    historyFiles: List<File>,
    onInspectFile: (File) -> Unit,
    onShareFile: (File) -> Unit,
    onDeleteFile: (File) -> Unit,
    onPickVideo: () -> Unit
) {
    val scrollState = rememberScrollState()
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "本机已生成实况照片 (${historyFiles.size})",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )

        if (historyFiles.isEmpty()) {
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
                        imageVector = Icons.Default.PhotoAlbum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "暂无历史实况照片",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "转换后的所有 Motion Photo 均会同时存入系统相册 DCIM/Camera 与此处作品库。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(
                        onClick = onPickVideo,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("立即去创作第一张实况照片")
                    }
                }
            }
        } else {
            historyFiles.forEach { file ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = ExpressiveMotion.defaultSpatial()),
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
                            model = file,
                            contentDescription = file.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .expressiveBounceClick { onInspectFile(file) }
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${formatBytes(file.length())} · ${dateFormat.format(Date(file.lastModified()))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ExpressiveFeaturePill(
                                    text = "播放 / 拆解 / 修复",
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onInspectFile(file) }) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "播放检验",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { onShareFile(file) }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "分享",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                            IconButton(onClick = { onDeleteFile(file) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ThemeCustomizationSheet(
    settings: MainViewModel.ThemeSettings,
    onSelectPalette: (ExpressivePalette) -> Unit,
    onToggleDynamic: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Material 3 Expressive 个性化色彩",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
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
                            text = "Material You 壁纸动态取色",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "跟随 Android 12–16 系统壁纸自动生成高对比度色板",
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

        ExpressivePalette.values().forEach { palette ->
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
                            .size(42.dp)
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
                        Text(
                            text = palette.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.2f MB", mb)
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
        context.startActivity(Intent.createChooser(intent, "分享实况照片 (${file.name})"))
    } catch (_: Exception) {
    }
}
