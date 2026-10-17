package com.motionphoto.converter.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.motionphoto.converter.R
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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
                contentKey = { it.name },
                transitionSpec = {
                    fadeIn(animationSpec = tween(160)) togetherWith
                        fadeOut(animationSpec = tween(130))
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

                    AppTab.ABOUT -> {
                        AboutAuthorTabContent(
                            onShowToast = viewModel::showBannerMessage
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = statusBanner != null,
                enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.92f, animationSpec = tween(150)),
                exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.92f, animationSpec = tween(150)),
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
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MotionPhotosOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(25.dp)
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
                            text = "黑白君定制版",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = when (currentTab) {
                        AppTab.STUDIO -> "极简一键合成 · 支持展开专业精修"
                        AppTab.INSPECTOR -> "XMP 检验 · 无损拆解 MP4/JPG · 一键修复"
                        AppTab.GALLERY -> "本机已生成实况作品库"
                        AppTab.ABOUT -> "关于作者 · 黑白君 (抖音: heibaijunbenjun)"
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
                        imageVector = Icons.Default.Palette,
                        contentDescription = "主题色彩",
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
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "深浅色模式",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

/**
 * 关键性能优化：使用 contentKey = { it::class.simpleName }
 * 确保在 Editing 内部修改任何参数时绝不触发整页重绘或重建 ExoPlayer！
 */
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
            fadeIn(animationSpec = tween(180)) togetherWith
                fadeOut(animationSpec = tween(140))
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
            shape = RoundedCornerShape(34.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ExpressiveMorphingBadge(
                    modifier = Modifier.size(144.dp),
                    lobes = 8f,
                    waveAmplitude = 0.10f,
                    isAnimating = false
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MotionPhotosOn,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "LIVE · 实况",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "视频一键转实况照片",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "零门槛傻瓜式操作：选好视频即可一键生成全品牌兼容的动态照片，自动存入系统相册；进阶玩家亦可展开图影混流、长曝光与滤镜工具。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .expressiveBounceClick(pressedScale = 0.95f) {
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
                            text = "选择一段视频开始制作",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
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
                    shape = RoundedCornerShape(18.dp),
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
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "批量选择多段视频一键转实况",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 三步极简新手引导卡
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StepBadgeItem("1", "挑选视频", "从相册选一段视频")
                StepBadgeItem("2", "选封面帧", "滑动定格最美瞬间")
                StepBadgeItem("3", "一键保存", "自动存入系统相册")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StepBadgeItem(step: String, title: String, desc: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = step,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

/**
 * 极简默认 + 可折叠专业精修工作台 (120Hz 防卡顿优化版)
 * - 默认只显示「视频预览」「滑动选封面」「一键合成实况照片」，小白零认知负担
 * - 拖动滑块时仅更新本地 Float 状态并实时预览胶片缩略图，松手才触发高清帧解码，彻底告别卡顿
 */
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

    // 本地瞬时滑块状态：保证拖动滑块 120Hz 零卡顿，不触发全量 ViewModel 重组
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

    // 拖动滑块时，立即从已缓存的 8 帧缩略图中取最近的一帧用于 0 延迟反馈
    val displayedCoverBitmap: Bitmap? = if (isDraggingCoverSlider && state.timelineThumbnails.isNotEmpty()) {
        val ratio = (localCoverTimeUs / (state.metadata.durationMs * 1000f).coerceAtLeast(1f)).coerceIn(0f, 1f)
        val idx = (ratio * (state.timelineThumbnails.size - 1)).roundToInt()
            .coerceIn(0, state.timelineThumbnails.lastIndex)
        state.timelineThumbnails[idx]
    } else {
        state.coverPreviewBitmap ?: state.timelineThumbnails.firstOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 顶部预览切换：动态视频 vs 静态封面
        ExpressiveConnectedButtonGroup(
            items = listOf("VIDEO", "COVER"),
            selectedItem = previewMode,
            onSelect = { mode -> previewMode = mode },
            labelProvider = { mode ->
                if (mode == "VIDEO") "动态效果预览" else "静态封面预览"
            }
        )

        // 2. 主预览视窗
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
                    update = { view -> view.player = exoPlayer },
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
                        contentDescription = "外部高清封面",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (displayedCoverBitmap != null) {
                    Image(
                        bitmap = displayedCoverBitmap.asImageBitmap(),
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
                        String.format("%.1fs", (localTrimEndMs - localTrimStartMs) / 1000f)
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
                            state.customCoverImageUri != null -> "外部高清主图"
                            state.longExposureMode != MotionPhotoBuilder.LongExposureMode.NONE -> state.longExposureMode.title
                            else -> "封面 @ ${(localCoverTimeUs / 1000f).toLong()} ms"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

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
                                imageVector = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. 极简第 1 步：滑动挑选封面瞬间
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraEnhance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = "挑选封面帧（不滑则使用默认最佳帧）",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = String.format("%.2f 秒", localCoverTimeUs / 1_000_000f),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (state.timelineThumbnails.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
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
                        previewMode = "COVER"
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

        // 4. 极简主操作按钮（放在最显眼位置，小白无需往下翻即可一键完成！）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(0.32f)
                    .height(58.dp),
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
                    .weight(0.68f)
                    .height(58.dp)
                    .expressiveBounceClick(pressedScale = 0.95f) {
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
                        text = "一键生成实况照片",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // 5. 可折叠「高级精修选项 (Pro)」抽屉（默认收起，界面清爽不懵圈！）
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .expressiveBounceClick(pressedScale = 0.98f) {
                    showAdvancedOptions = !showAdvancedOptions
                },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
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
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = if (showAdvancedOptions) "收起高级创作选项 (Pro)" else "展开高级创作选项 (Pro · 选填)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "默认已自动开启：${state.selectedFormat.displayName} · 保留原声 · 95% 高画质",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                Icon(
                    imageVector = if (showAdvancedOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        AnimatedVisibility(
            visible = showAdvancedOptions,
            enter = fadeIn(tween(180)) + expandVertically(tween(200)),
            exit = fadeOut(tween(140)) + shrinkVertically(tween(160))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 进阶卡片 A：片段裁剪与原声/画质
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
                                    imageVector = Icons.Default.ContentCut,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                                Text(
                                    text = "视频片段截取",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${String.format("%.1fs", localTrimStartMs / 1000f)} — ${
                                    String.format("%.1fs", localTrimEndMs / 1000f)
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
                                    imageVector = if (state.includeAudio) Icons.Default.MusicNote else Icons.Default.MusicOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "保留视频原声 (AAC)",
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
                                text = "主图 JPEG 编码品质",
                                style = MaterialTheme.typography.labelMedium,
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

                // 进阶卡片 B：开源特色玩法（图影混流 / 长曝光 / 滤镜 / 水印 / 样张 EXIF 克隆）
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
                            text = "进阶玩法：换高清封面 · 长曝光 · 滤镜水印",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "外部高清封面混流 (Photo + Video)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = state.customCoverName?.let { "已选：$it" }
                                        ?: "任选相册高清照片替代视频帧作为实况封面",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (state.customCoverImageUri != null) {
                                OutlinedButton(onClick = onClearCustomCover, shape = RoundedCornerShape(14.dp)) {
                                    Text("还原")
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = {
                                        previewMode = "COVER"
                                        onPickCustomCover()
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("选外部照片")
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
                                    text = "克隆本机相机样张 EXIF",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = state.referenceExifName?.let { "已克隆：$it" }
                                        ?: "从本机实拍照片复制镜头与机型参数",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (state.referenceExifImageUri != null) {
                                OutlinedButton(onClick = onClearReferenceExif, shape = RoundedCornerShape(14.dp)) {
                                    Text("清除")
                                }
                            } else {
                                FilledTonalButton(onClick = onPickReferenceExif, shape = RoundedCornerShape(14.dp)) {
                                    Text("选本机样张")
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "动态长曝光堆栈封面",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "封面色彩滤镜",
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "叠加底部光学机型铭牌水印",
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

                // 进阶卡片 C：目标厂商 XMP 协议切换
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
                                text = "目标手机相册 XMP 协议：${state.selectedFormat.displayName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = state.selectedFormat.description,
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
                                text = "切换",
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
                    text = "选择目标设备 XMP 实况格式",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
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
                                Text(
                                    text = "${fmt.displayName} (${fmt.badge})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
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
            modifier = Modifier.size(210.dp),
            contentAlignment = Alignment.Center
        ) {
            ExpressiveMorphingBadge(
                modifier = Modifier.size(156.dp),
                lobes = 10f,
                waveAmplitude = 0.11f,
                isAnimating = true
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
                strokeWidth = 7.dp
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        Text(
            text = when (stage) {
                MotionPhotoBuilder.ConversionProgress.Stage.EXTRACTING_FRAME -> "正在处理高清封面并写入相机 EXIF"
                MotionPhotoBuilder.ConversionProgress.Stage.EXTRACTING_VIDEO -> "正在无损封装 MP4 动态轨道"
                MotionPhotoBuilder.ConversionProgress.Stage.WRITING_METADATA -> "正在注入标准 XMP 容器目录"
                MotionPhotoBuilder.ConversionProgress.Stage.COMBINING -> "正在拼接 JPEG 与尾部 MP4 流"
                MotionPhotoBuilder.ConversionProgress.Stage.SAVING_TO_GALLERY -> "正在保存至系统相册 DCIM/Camera"
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
                            "实况照片已保存至系统相册！"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "相册位置：${result.galleryDisplayPath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // 默认静态显示封面，点击后按需启动 ExoPlayer，确保打开结果页瞬间丝滑不卡顿
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
                Icon(imageVector = Icons.Default.Verified, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("检验与拆解", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onShare,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
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
                .height(52.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("继续制作下一张", fontWeight = FontWeight.Bold)
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
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "合成遇到问题",
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
        Spacer(modifier = Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.height(50.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("返回")
            }
            Button(
                onClick = onRetry,
                modifier = Modifier.height(50.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("重新选择视频")
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
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "实况检验 · 提取视频 · 修复",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "选择任意实况照片，可在此播放内部视频、单独提取 MP4 视频到相册，或一键修复无法识别的实况照片。",
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
                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("从相册选择照片进行检验 / 提取", fontWeight = FontWeight.Bold)
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
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "尚未加载待检照片",
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
                                    "检测到内嵌视频（可一键修复 XMP）"
                                } else {
                                    "普通静态 JPEG（不含内嵌视频流）"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
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
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "无损拆解导出 & 实况修复工具箱",
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
                                        .height(48.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("提取视频 (.mp4)", fontWeight = FontWeight.Bold)
                                }

                                FilledTonalButton(
                                    onClick = onExportStill,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("提取主图 (.jpg)", fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = onRepairMotionPhoto,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("一键修复 XMP 并重新保存至相册", fontWeight = FontWeight.Bold)
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
                            text = "容器诊断指标",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        MetricRow("文件名", report.fileName)
                        MetricRow("文件总大小", formatBytes(report.totalFileSize))
                        MetricRow("内嵌 MP4 长度", formatBytes(report.actualMp4Length))
                        MetricRow("封面帧时间戳", "${report.presentationTimestampUs} μs")

                        if (report.rawXmpSnippet.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            FilledTonalButton(
                                onClick = { showRawXmp = !showRawXmp },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Code, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (showRawXmp) "收起 XMP 报文" else "查看 XMP 报文")
                            }

                            AnimatedVisibility(visible = showRawXmp) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    shape = RoundedCornerShape(14.dp),
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    Spacer(modifier = Modifier.height(14.dp))
                    FilledTonalButton(
                        onClick = onPickVideo,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("立即去制作第一张实况照片")
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
                                .size(68.dp)
                                .clip(RoundedCornerShape(16.dp))
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
                        }

                        Row {
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

/**
 * Tab 4：关于作者页面 (黑白君 · 抖音号 heibaijunbenjun · 专属抖音二维码展示与保存)
 */
@Composable
private fun AboutAuthorTabContent(
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var showZoomQrDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 作者个人品牌名片卡 (MD3 Expressive)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "黑白君",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "黑白君",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = "抖音号：heibaijunbenjun",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "“为共产主义而奋斗...”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 一键复制抖音号 + 保存二维码双按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("抖音号", "heibaijunbenjun"))
                            onShowToast("已复制抖音号：heibaijunbenjun，快去抖音搜索关注吧！")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("复制抖音号", fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                val savedPath = saveQrDrawableToGallery(context)
                                if (savedPath != null) {
                                    onShowToast("二维码已保存到相册：$savedPath")
                                } else {
                                    onShowToast("保存二维码失败，请检查相册权限")
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("保存二维码", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. 抖音扫码名片大图卡（点击可全屏放大）
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .expressiveBounceClick(pressedScale = 0.98f) {
                    showZoomQrDialog = true
                },
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "抖音扫一扫关注「黑白君」（点击图片可全屏放大）",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Image(
                    painter = painterResource(id = R.drawable.douyin_qr_heibaijun),
                    contentDescription = "黑白君抖音二维码 (heibaijunbenjun)",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .clip(RoundedCornerShape(24.dp))
                )
            }
        }

        // 3. 关于应用版本说明
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "关于 Motion Studio (黑白君定制版)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                MetricRow("当前版本", "v3.5.0-md3e-release (120Hz 丝滑优化版)")
                MetricRow("作者 / 出品", "黑白君")
                MetricRow("抖音号", "heibaijunbenjun")
                MetricRow("设计语言", "Google Material 3 Expressive (MD3E)")
                MetricRow("支持标准", "Google / 小米 / OPPO / 华为 / 三星全兼容")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showZoomQrDialog) {
        Dialog(onDismissRequest = { showZoomQrDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .expressiveBounceClick { showZoomQrDialog = false },
                shape = RoundedCornerShape(28.dp),
                color = Color.Transparent
            ) {
                Image(
                    painter = painterResource(id = R.drawable.douyin_qr_heibaijun),
                    contentDescription = "黑白君抖音二维码大图",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                )
            }
        }
    }
}

private suspend fun saveQrDrawableToGallery(context: Context): String? = withContext(Dispatchers.IO) {
    try {
        val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.douyin_qr_heibaijun)
            ?: return@withContext null
        val outName = "Douyin_Heibaijun_heibaijunbenjun_QR.jpg"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, outName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/MotionStudio")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext null
        resolver.openOutputStream(uri)?.use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        bmp.recycle()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        }
        "Pictures/MotionStudio/$outName"
    } catch (_: Exception) {
        null
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
