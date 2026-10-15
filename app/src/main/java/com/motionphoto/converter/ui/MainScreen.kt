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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoAwesomeMotion
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
import com.motionphoto.converter.core.MotionPhotoInspector
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
    val systemDark = isSystemInDarkTheme()
    val isDark = themeSettings.darkModeOverride ?: systemDark

    var showPaletteSheet by remember { mutableStateOf(false) }

    // 权限请求器
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    // 现代 Photo Picker 选视频 + GetContent 兜底
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

    // 实况检验器选图片
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
                            onUpdateCoverTime = viewModel::updateCoverFrameTime,
                            onUpdateTrimRange = viewModel::updateTrimRange,
                            onUpdateAudio = viewModel::updateIncludeAudio,
                            onUpdateQuality = viewModel::updateJpegQuality,
                            onUpdateFormat = viewModel::updateOutputFormat,
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
                            onShareFile = { file -> shareMotionPhoto(context, file) }
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
                            text = "MD3E",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = when (currentTab) {
                        AppTab.STUDIO -> "Android 16 实况照片无损合成工坊"
                        AppTab.INSPECTOR -> "XMP 容器结构与尾部视频流检验器"
                        AppTab.GALLERY -> "已生成实况作品与多品牌相册指南"
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
    onUpdateCoverTime: (Long) -> Unit,
    onUpdateTrimRange: (Long, Long) -> Unit,
    onUpdateAudio: (Boolean) -> Unit,
    onUpdateQuality: (Int) -> Unit,
    onUpdateFormat: (MotionPhotoBuilder.OutputFormat) -> Unit,
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
                StudioIdleHeroView(onPickVideo = onPickVideo)
            }

            is MainViewModel.StudioState.Editing -> {
                StudioEditorView(
                    state = current,
                    onUpdateCoverTime = onUpdateCoverTime,
                    onUpdateTrimRange = onUpdateTrimRange,
                    onUpdateAudio = onUpdateAudio,
                    onUpdateQuality = onUpdateQuality,
                    onUpdateFormat = onUpdateFormat,
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
                    onShare = { onShareFile(current.result.localFile) },
                    onInspect = { onInspectFile(current.result.localFile) },
                    onConvertAnother = {
                        onReset()
                        onPickVideo()
                    },
                    onBackHome = onReset
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

/**
 * 创作工坊：空闲态 MD3 Expressive 英雄主屏
 */
@Composable
private fun StudioIdleHeroView(
    onPickVideo: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 主视觉超椭圆英雄卡片
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(36.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ExpressiveMorphingBadge(
                    modifier = Modifier.size(168.dp),
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
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "LIVE · MP",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "让每一段视频\n化作鲜活的实况照片",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "任意挑选高光封面帧、自由裁剪片段，无损封装符合 Android Motion Photo 1.0 与各大国产手机相册标准的实况照片，自动保存至系统相册。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 特性胶囊标签行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    ExpressiveFeaturePill("微秒级选封面", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    ExpressiveFeaturePill("无损 MP4 封装", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                    ExpressiveFeaturePill("直存系统相册", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 主操作大尺寸弹簧超椭圆按钮
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
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
                            text = "从相册选择视频开始创作",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 多品牌协议支持展示矩阵
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BrandSupportMiniCard(
                title = "Google Pixel",
                subtitle = "Motion Photo 1.0 标准容器目录",
                badge = "PXL_*MP.jpg",
                modifier = Modifier.weight(1f)
            )
            BrandSupportMiniCard(
                title = "小米 / OPPO / 华为",
                subtitle = "MicroVideo + O-Live + EXIF 全注入",
                badge = "MVIMG_*MP.jpg",
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

/**
 * 创作工坊：交互式视频裁剪、封面帧精修与多格式合成编辑器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudioEditorView(
    state: MainViewModel.StudioState.Editing,
    onUpdateCoverTime: (Long) -> Unit,
    onUpdateTrimRange: (Long, Long) -> Unit,
    onUpdateAudio: (Boolean) -> Unit,
    onUpdateQuality: (Int) -> Unit,
    onUpdateFormat: (MotionPhotoBuilder.OutputFormat) -> Unit,
    onStartConvert: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var previewMode by remember { mutableStateOf("VIDEO") } // "VIDEO" or "COVER"
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
        // 1. 顶部模式切换：动态视频预览 vs 静态封面帧高清预览
        ExpressiveConnectedButtonGroup(
            items = listOf("VIDEO", "COVER"),
            selectedItem = previewMode,
            onSelect = { mode ->
                previewMode = mode
            },
            labelProvider = { mode ->
                if (mode == "VIDEO") "动态视频实时预览" else "静态封面帧定格预览"
            }
        )

        // 2. 主预览视窗 (超椭圆圆角容器)
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
                val bitmap = state.coverPreviewBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
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

            // 左上角媒体参数胶囊
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

            // 右下角播放/暂停或封面时间戳胶囊
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
                        text = "封面 @ ${state.coverTimeUs / 1000L} ms",
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

        // 3. 封面帧时间轴胶片精修卡片
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
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
                            imageVector = Icons.Default.CameraEnhance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "静态封面帧时间点",
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

                // 8 帧胶片缩略图条
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

                Text(
                    text = "拖动滑块或点击胶片帧挑选最清晰的一瞬作为相册静态主图，时间戳将精确写入 XMP。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 4. 视频裁剪区间 & 原声/画质控制卡片
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

                // 原声保留开关
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

                // JPEG 封面画质连通按钮组
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

        // 5. 目标厂商 XMP 协议选择器卡片
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

        // 6. 底部合成与取消双操作栏
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
                                    text = fmt.subtitle,
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

/**
 * 创作工坊：MD3 Expressive 波浪进度与多边形形变合成状态屏
 */
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
                MotionPhotoBuilder.ConversionProgress.Stage.EXTRACTING_FRAME -> "正在提取高清封面并写入相机 EXIF"
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

/**
 * 创作工坊：合成成功与实况即时交互预览页
 */
@Composable
private fun StudioSuccessView(
    result: MotionPhotoBuilder.ConversionResult,
    onShare: () -> Unit,
    onInspect: () -> Unit,
    onConvertAnother: () -> Unit,
    onBackHome: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部成功横幅
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
                        text = "实况照片已写入系统相册！",
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

        // 交互式实况预览卡片（默认自动播放或点击播放内嵌视频）
        LiveMotionPhotoPlayerCard(
            coverImageModel = result.localFile,
            videoFileOrUri = result.localFile,
            resolutionLabel = result.resolution,
            durationMs = result.videoDurationMs,
            autoPlayInitially = true
        )

        // 容器二进制结构明细卡
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
                    "文件结构分布",
                    "JPEG 主图 ${formatBytes(result.jpegSizeBytes)} + 尾部 MP4 ${formatBytes(result.videoSizeBytes)}"
                )
                MetricRow("总文件大小", formatBytes(result.totalSizeBytes))
                MetricRow("封面帧微秒时间戳", "${result.coverTimestampUs} μs (${result.coverTimestampUs / 1000L} ms)")
            }
        }

        // 操作按钮组
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
                Text("检验 XMP 结构", fontWeight = FontWeight.Bold)
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
 * Tab 2：实况检验器 (XMP 元数据与内嵌 MP4 深度检验 & 实况回放)
 */
@Composable
private fun InspectorTabContent(
    state: MainViewModel.InspectorState,
    onPickImage: () -> Unit,
    onShareFile: (File) -> Unit
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
        // 顶部选图检验卡片
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
                    text = "实况照片 XMP & 二进制检验器",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "选择任意 JPG / MP.jpg 照片，解析其内部 APP1 XMP 容器目录、检测尾部 MP4 ftyp 偏移量，并直接播放提取出的实况视频轨。",
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
                    Text("从相册选择照片进行实况检验", fontWeight = FontWeight.Bold)
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
                            text = "您可以在「创作工坊」合成后点击检验，或直接点击上方按钮挑选任意照片。",
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

                // 检验结论徽章卡
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
                                } else {
                                    "普通静态 JPEG（未检出完整实况轨道）"
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

                // 如果提取出了内嵌 MP4，展示实况交互播放器
                if (report.extractedVideoFile != null) {
                    LiveMotionPhotoPlayerCard(
                        coverImageModel = state.sourceUriOrFile,
                        videoFileOrUri = report.extractedVideoFile,
                        resolutionLabel = report.MakeModelSummary,
                        durationMs = 2500L,
                        autoPlayInitially = true
                    )
                }

                // 详细元数据报告卡
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

/**
 * Tab 3：已生成实况作品管理 & 国产手机相册兼容性指南
 */
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
                                    text = "实况播放 / 检验",
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

        // 多品牌相册识别说明卡
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "为什么重构后能被系统相册识别？",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "1. 标准 RDF 容器结构：修复了旧版将 XML 字符串转义写入导致 XMP 损坏的 Bug，完整构建 Container:Directory 序列。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "2. 相机 EXIF 与文件命名：自动写入相机 Make/Model/DateTimeOriginal 并采用官方 *MP.jpg 命名规范，直接通过 MediaStore 写入系统相册 DCIM/Camera。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "3. 竖屏旋转与时间戳对齐：自动保留视频旋转矩阵并写入精确封面微秒时间戳，杜绝竖屏拉伸与音画不同步。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
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
