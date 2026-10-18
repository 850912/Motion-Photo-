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
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CameraEnhance
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GifBox
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MotionPhotosOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCode2
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
import androidx.compose.runtime.rememberCoroutineScope
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
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.size.Precision
import com.motionphoto.converter.R
import com.motionphoto.converter.core.MotionPhotoBuilder
import com.motionphoto.converter.ui.components.AnimatedGifPreviewCard
import com.motionphoto.converter.ui.components.AppTab
import com.motionphoto.converter.ui.components.ExpressiveConnectedButtonGroup
import com.motionphoto.converter.ui.components.ExpressiveFloatingToolbar
import com.motionphoto.converter.ui.components.ExpressiveMorphingBadge
import com.motionphoto.converter.ui.components.ExpressiveWavyCircularHalo
import com.motionphoto.converter.ui.components.ExpressiveWavyLinearProgress
import com.motionphoto.converter.ui.components.LiveMotionPhotoPlayerCard
import com.motionphoto.converter.ui.components.liquidGlassBackdropSource
import com.motionphoto.converter.ui.components.rememberLiquidGlassBackdropState
import com.motionphoto.converter.ui.theme.ExpressiveMotion
import com.motionphoto.converter.ui.theme.ExpressivePalette
import com.motionphoto.converter.ui.theme.expressiveBounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

private val PreviewModes = listOf("VIDEO", "COVER")
private val QualityOptions = listOf(88, 95, 100)

private enum class LocalMediaPickerPurpose(
    val title: String,
    val subtitle: String,
    val isVideo: Boolean,
    val isBatch: Boolean
) {
    SINGLE_VIDEO(
        title = "选择本地视频",
        subtitle = "直接读取本机 MediaStore 视频库，零云端延迟",
        isVideo = true,
        isBatch = false
    ),
    BATCH_VIDEOS(
        title = "批量选择本地视频 (最多 12 个)",
        subtitle = "勾选多个本机视频一键排队合成实况照片",
        isVideo = true,
        isBatch = true
    ),
    CUSTOM_COVER_IMAGE(
        title = "选择本地高清封面图",
        subtitle = "挑选一张本机静态照片作为实况封面",
        isVideo = false,
        isBatch = false
    ),
    REFERENCE_EXIF_IMAGE(
        title = "选择本机相机样张 (克隆 EXIF)",
        subtitle = "挑选一张本机原生相机拍的照片以克隆机型与镜头参数",
        isVideo = false,
        isBatch = false
    ),
    INSPECTOR_IMAGE(
        title = "选择要解析的实况照片或动图",
        subtitle = "挑选本机实况照片或 GIF 动图进行拆解、转 GIF 或修复",
        isVideo = false,
        isBatch = false
    )
}

/**
 * 纯本地系统文件选择器 Contract (使用 ACTION_OPEN_DOCUMENT + EXTRA_LOCAL_ONLY = true)，
 * 彻底绕过 Android 13/14+ PickVisualMedia 触发的 Google 相册云端选择器“部分照片无法加载”故障。
 */
private class LocalOpenSingleMediaContract : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = input
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) intent?.data else null
    }
}

private class LocalOpenMultipleMediaContract : ActivityResultContract<String, List<Uri>>() {
    override fun createIntent(context: Context, input: String): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = input
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (resultCode != Activity.RESULT_OK || intent == null) return emptyList()
        val list = ArrayList<Uri>()
        intent.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) {
                clip.getItemAt(i)?.uri?.let { uri ->
                    if (list.size < 12) list.add(uri)
                }
            }
        }
        if (list.isEmpty()) {
            intent.data?.let { list.add(it) }
        }
        return list
    }
}

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

    val liquidBackdropState = rememberLiquidGlassBackdropState()

    var showPaletteSheet by remember { mutableStateOf(false) }
    var showLiquidGlassSheet by remember { mutableStateOf(false) }
    var activePickerPurpose by remember { mutableStateOf<LocalMediaPickerPurpose?>(null) }
    var pendingPurposeAfterPerm by remember { mutableStateOf<LocalMediaPickerPurpose?>(null) }
    var permissionRefreshTrigger by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        permissionRefreshTrigger++
        val pending = pendingPurposeAfterPerm
        if (pending != null) {
            activePickerPurpose = pending
            pendingPurposeAfterPerm = null
        }
    }

    val systemSingleDocLauncher = rememberLauncherForActivityResult(
        LocalOpenSingleMediaContract()
    ) { uri: Uri? ->
        if (uri != null) {
            val purpose = activePickerPurpose
            activePickerPurpose = null
            when (purpose) {
                LocalMediaPickerPurpose.CUSTOM_COVER_IMAGE -> viewModel.setCustomCoverImage(uri)
                LocalMediaPickerPurpose.REFERENCE_EXIF_IMAGE -> viewModel.setReferenceExifImage(uri)
                LocalMediaPickerPurpose.INSPECTOR_IMAGE -> viewModel.inspectMotionPhotoUri(uri)
                else -> viewModel.selectVideo(uri)
            }
        }
    }

    val systemBatchVideoDocLauncher = rememberLauncherForActivityResult(
        LocalOpenMultipleMediaContract()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            activePickerPurpose = null
            viewModel.startBatchConversion(uris)
        }
    }

    val getContentFallbackLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val purpose = activePickerPurpose
            activePickerPurpose = null
            when (purpose) {
                LocalMediaPickerPurpose.CUSTOM_COVER_IMAGE -> viewModel.setCustomCoverImage(uri)
                LocalMediaPickerPurpose.REFERENCE_EXIF_IMAGE -> viewModel.setReferenceExifImage(uri)
                LocalMediaPickerPurpose.INSPECTOR_IMAGE -> viewModel.inspectMotionPhotoUri(uri)
                else -> viewModel.selectVideo(uri)
            }
        }
    }

    fun hasMediaPermission(isVideo: Boolean): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (isVideo) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    fun openLocalPicker(purpose: LocalMediaPickerPurpose) {
        if (hasMediaPermission(purpose.isVideo)) {
            activePickerPurpose = purpose
        } else {
            pendingPurposeAfterPerm = purpose
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
    }

    val launchVideoPicker = { openLocalPicker(LocalMediaPickerPurpose.SINGLE_VIDEO) }
    val launchBatchVideoPicker = { openLocalPicker(LocalMediaPickerPurpose.BATCH_VIDEOS) }

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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        // 沉浸式全屏容器：页面内容延伸至底部 Dock 栏下方，供液态玻璃实时捕获并折射背景
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .liquidGlassBackdropSource(
                        state = liquidBackdropState,
                        enabled = themeSettings.liquidGlass.enabled
                    )
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    contentKey = { it.name },
                    transitionSpec = {
                        val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                        (slideInHorizontally(animationSpec = ExpressiveMotion.defaultSpatial()) { fullWidth ->
                            direction * (fullWidth / 5)
                        } + scaleIn(
                            initialScale = 0.93f,
                            animationSpec = ExpressiveMotion.defaultSpatial()
                        ) + fadeIn(
                            animationSpec = ExpressiveMotion.defaultEffects()
                        )) togetherWith (slideOutHorizontally(animationSpec = ExpressiveMotion.defaultSpatial()) { fullWidth ->
                            -direction * (fullWidth / 6)
                        } + scaleOut(
                            targetScale = 0.94f,
                            animationSpec = ExpressiveMotion.defaultSpatial()
                        ) + fadeOut(
                            animationSpec = ExpressiveMotion.fastEffects()
                        ))
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
                                onPickCustomCover = { openLocalPicker(LocalMediaPickerPurpose.CUSTOM_COVER_IMAGE) },
                                onClearCustomCover = { viewModel.setCustomCoverImage(null) },
                                onPickReferenceExif = { openLocalPicker(LocalMediaPickerPurpose.REFERENCE_EXIF_IMAGE) },
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
                                onExportWeChatGif = viewModel::exportSuccessResultAsWeChatGif,
                                onReset = viewModel::resetStudio,
                                onInspectFile = viewModel::inspectMotionPhotoFile,
                                onShareFile = { file -> shareMediaFile(context, file) }
                            )
                        }

                        AppTab.INSPECTOR -> {
                            val inspectorState by viewModel.inspectorState.collectAsState()
                            InspectorTabContent(
                                state = inspectorState,
                                onPickImage = { openLocalPicker(LocalMediaPickerPurpose.INSPECTOR_IMAGE) },
                                onExportVideo = viewModel::exportInspectedVideo,
                                onExportStill = viewModel::exportInspectedStill,
                                onExportWeChatGif = viewModel::exportInspectedWeChatGif,
                                onRepairMotionPhoto = viewModel::repairInspectedMotionPhoto
                            )
                        }

                        AppTab.GALLERY -> {
                            val historyItems by viewModel.historyFiles.collectAsState()
                            GalleryTabContent(
                                historyItems = historyItems,
                                onInspectFile = viewModel::inspectMotionPhotoFile,
                                onShareFile = { file -> shareMediaFile(context, file) },
                                onDeleteFile = viewModel::deleteHistoryFile,
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
            }

            // 悬浮于页面底部的液态玻璃 Dock 栏
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                ExpressiveFloatingToolbar(
                    currentTab = currentTab,
                    onSelectTab = viewModel::selectTab,
                    backdropState = liquidBackdropState,
                    liquidGlassConfig = themeSettings.liquidGlass,
                    isDark = isDark,
                    onOpenLiquidGlassTuner = { showLiquidGlassSheet = true }
                )
            }

            AnimatedVisibility(
                visible = statusBanner != null,
                enter = slideInVertically(animationSpec = ExpressiveMotion.fastSpatial()) { -it } +
                    scaleIn(initialScale = 0.88f, animationSpec = ExpressiveMotion.fastSpatial()) +
                    fadeIn(animationSpec = ExpressiveMotion.fastEffects()),
                exit = slideOutVertically(animationSpec = ExpressiveMotion.fastSpatial()) { -it } +
                    scaleOut(targetScale = 0.88f, animationSpec = ExpressiveMotion.fastSpatial()) +
                    fadeOut(animationSpec = ExpressiveMotion.fastEffects()),
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

    val currentPickerPurpose = activePickerPurpose
    if (currentPickerPurpose != null) {
        ModalBottomSheet(
            onDismissRequest = { activePickerPurpose = null },
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            InAppLocalMediaPickerSheet(
                purpose = currentPickerPurpose,
                refreshKey = permissionRefreshTrigger,
                onLoadMedia = { isVid -> viewModel.queryLocalDeviceMedia(isVid) },
                onSelectSingle = { uri ->
                    activePickerPurpose = null
                    when (currentPickerPurpose) {
                        LocalMediaPickerPurpose.CUSTOM_COVER_IMAGE -> viewModel.setCustomCoverImage(uri)
                        LocalMediaPickerPurpose.REFERENCE_EXIF_IMAGE -> viewModel.setReferenceExifImage(uri)
                        LocalMediaPickerPurpose.INSPECTOR_IMAGE -> viewModel.inspectMotionPhotoUri(uri)
                        else -> viewModel.selectVideo(uri)
                    }
                },
                onSelectBatch = { uris ->
                    activePickerPurpose = null
                    viewModel.startBatchConversion(uris)
                },
                onOpenSystemDocumentPicker = {
                    val mime = if (currentPickerPurpose.isVideo) "video/*" else "image/*"
                    try {
                        if (currentPickerPurpose.isBatch) {
                            systemBatchVideoDocLauncher.launch(mime)
                        } else {
                            systemSingleDocLauncher.launch(mime)
                        }
                    } catch (_: Exception) {
                        getContentFallbackLauncher.launch(mime)
                    }
                },
                onRequestPermission = {
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
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        (slideInVertically(animationSpec = ExpressiveMotion.fastSpatial()) { it / 2 } +
                            fadeIn(animationSpec = ExpressiveMotion.fastEffects())) togetherWith
                            (slideOutVertically(animationSpec = ExpressiveMotion.fastSpatial()) { -it / 2 } +
                                fadeOut(animationSpec = ExpressiveMotion.fastEffects()))
                    },
                    label = "topbar_subtitle"
                ) { tab ->
                    Text(
                        text = when (tab) {
                            AppTab.STUDIO -> "视频转实况照片 · 微信动图"
                            AppTab.INSPECTOR -> "查看信息与提取视频"
                            AppTab.GALLERY -> "历史记录"
                            AppTab.ABOUT -> "关于作者与使用指南"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
    onExportWeChatGif: () -> Unit,
    onReset: () -> Unit,
    onInspectFile: (File) -> Unit,
    onShareFile: (File) -> Unit
) {
    AnimatedContent(
        targetState = state,
        contentKey = { it::class.simpleName },
        transitionSpec = {
            (scaleIn(
                initialScale = 0.91f,
                animationSpec = ExpressiveMotion.defaultSpatial()
            ) + slideInVertically(
                animationSpec = ExpressiveMotion.defaultSpatial()
            ) { it / 8 } + fadeIn(
                animationSpec = ExpressiveMotion.defaultEffects()
            )) togetherWith (scaleOut(
                targetScale = 0.94f,
                animationSpec = ExpressiveMotion.fastSpatial()
            ) + fadeOut(
                animationSpec = ExpressiveMotion.fastEffects()
            ))
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
                    isExportingGif = current.isExportingGif,
                    onShare = { onShareFile(current.result.localFile) },
                    onInspect = { onInspectFile(current.result.localFile) },
                    onExportWeChatGif = onExportWeChatGif,
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
                    modifier = Modifier.size(138.dp),
                    lobes = 8f,
                    waveAmplitude = 0.11f,
                    isAnimating = true
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
                    text = "全品牌相册实况兼容（小米/OPPO/vivo/华为/三星/Google），支持微信朋友圈实况与微信聊天 GIF 动图。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
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
                        .expressiveBounceClick(pressedScale = 0.95f) {
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

        Spacer(modifier = Modifier.height(104.dp))
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

    val timelineImageBitmaps: List<ImageBitmap> = remember(state.timelineThumbnails) {
        state.timelineThumbnails.map { it.asImageBitmap() }
    }
    val coverPreviewImageBitmap: ImageBitmap? = remember(state.coverPreviewBitmap) {
        state.coverPreviewBitmap?.asImageBitmap()
    }

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

        // 输出格式与社交动图快捷选择栏（提升至主视野，方便直接选择全品牌实况或微信 GIF 动图）
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .expressiveBounceClick(pressedScale = 0.96f) { showFormatSheet = true },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "输出格式：${state.selectedFormat.displayName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
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
                    Spacer(modifier = Modifier.height(2.dp))
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
                        text = "切换格式",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
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
                    .expressiveBounceClick(pressedScale = 0.94f) {
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
                        text = if (state.selectedFormat == MotionPhotoBuilder.OutputFormat.WECHAT_GIF) {
                            "生成微信 GIF 动图"
                        } else {
                            "生成动态照片"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // 更多设置折叠栏
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .expressiveBounceClick(pressedScale = 0.96f) {
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
                            text = "截取片段、替换封面、相机参数克隆、长曝光与滤镜",
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
            enter = expandVertically(animationSpec = ExpressiveMotion.defaultSpatial()) +
                fadeIn(animationSpec = ExpressiveMotion.defaultEffects()),
            exit = shrinkVertically(animationSpec = ExpressiveMotion.defaultSpatial()) +
                fadeOut(animationSpec = ExpressiveMotion.fastEffects())
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
                                        ?: "从本机拍的照片复制机型信息，提高相册与微信识别率",
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
            }
        }

        Spacer(modifier = Modifier.height(104.dp))
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
                    text = "选择兼容模式与微信动图格式",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "支持各大手机品牌原生相册实况、微信朋友圈/小红书实况直发、以及微信聊天 GIF 表情包动图",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
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
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        }
                                    ) {
                                        Text(
                                            text = fmt.badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = fmt.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = fmt.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            if (selected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
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
            modifier = Modifier.size(204.dp),
            contentAlignment = Alignment.Center
        ) {
            ExpressiveMorphingBadge(
                modifier = Modifier.size(150.dp),
                lobes = 10f,
                waveAmplitude = 0.12f,
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
    isExportingGif: Boolean,
    onShare: () -> Unit,
    onInspect: () -> Unit,
    onExportWeChatGif: () -> Unit,
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
                        text = when {
                            result.galleryUri == null -> "文件已生成，相册保存失败"
                            result.isGif -> "已保存微信 GIF 动图到相册"
                            batchCount > 1 -> "批量合成完成（$batchCount 张）"
                            else -> "已保存到相册 (${result.format.displayName})"
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

        if (!result.isGif) {
            LiveMotionPhotoPlayerCard(
                coverImageModel = result.localFile,
                videoFileOrUri = result.localFile,
                resolutionLabel = result.resolution,
                durationMs = result.videoDurationMs,
                autoPlayInitially = false
            )
        } else {
            AnimatedGifPreviewCard(
                gifFile = result.localFile,
                resolutionLabel = result.resolution,
                fileSizeBytes = result.totalSizeBytes
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!result.isGif) {
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
                Text(if (result.isGif) "发送微信动图" else "分享", fontWeight = FontWeight.Bold)
            }
        }

        if (!result.isGif) {
            FilledTonalButton(
                onClick = onExportWeChatGif,
                enabled = !isExportingGif,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(imageVector = Icons.Rounded.GifBox, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isExportingGif) "正在生成微信 GIF 动图..." else "同时导出微信聊天 GIF 动图",
                    fontWeight = FontWeight.Bold
                )
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

        Spacer(modifier = Modifier.height(104.dp))
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
    onExportWeChatGif: () -> Unit,
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
                    text = "选择一张动态照片，可无损提取视频、导出静态主图、一键转微信 GIF 动图，或修复无法识别的实况照片。",
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
                            Column {
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
                                if (report.detectedStandards.isNotEmpty()) {
                                    Text(
                                        text = "已识别协议：${report.detectedStandards.joinToString(" · ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (report.primaryMime == "image/gif" && state.sourceUriOrFile is File) {
                    AnimatedGifPreviewCard(
                        gifFile = state.sourceUriOrFile,
                        resolutionLabel = report.MakeModelSummary,
                        fileSizeBytes = report.totalFileSize
                    )
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

                            FilledTonalButton(
                                onClick = onExportWeChatGif,
                                enabled = !state.isExportingGif,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.GifBox, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.isExportingGif) "正在编码微信 GIF..." else "导出微信 GIF 动图",
                                    fontWeight = FontWeight.Bold
                                )
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

                            AnimatedVisibility(
                                visible = showRawXmp,
                                enter = expandVertically(animationSpec = ExpressiveMotion.defaultSpatial()) +
                                    fadeIn(animationSpec = ExpressiveMotion.defaultEffects()),
                                exit = shrinkVertically(animationSpec = ExpressiveMotion.defaultSpatial()) +
                                    fadeOut(animationSpec = ExpressiveMotion.fastEffects())
                            ) {
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

        Spacer(modifier = Modifier.height(104.dp))
    }
}

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
                                    imageVector = if (item.isGif) Icons.Rounded.GifBox else Icons.Rounded.PlayArrow,
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
            Spacer(modifier = Modifier.height(104.dp))
        }
    }
}

/**
 * Tab 4：关于与使用说明页面
 * 完整保留原作者信息（黑白君 · 抖音号 heibaijunbenjun · 抖音二维码），并附带全品牌与微信实况/GIF动图使用说明
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
        // 1. 原作者信息主卡片（黑白君 · 抖音号 heibaijunbenjun）
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = "黑白君",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "黑白君",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
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

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("抖音号", "heibaijunbenjun"))
                            onShowToast("已复制抖音号：heibaijunbenjun")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("复制抖音号", fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                val savedPath = saveQrDrawableToGallery(context)
                                if (savedPath != null) {
                                    onShowToast("已保存到相册：$savedPath")
                                } else {
                                    onShowToast("保存失败，请检查相册权限")
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("保存二维码", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. 抖音二维码卡片（点击可放大查看）
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .expressiveBounceClick(pressedScale = 0.97f) {
                    showZoomQrDialog = true
                },
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
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
                        imageVector = Icons.Rounded.QrCode2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "打开抖音扫一扫",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Image(
                    painter = painterResource(id = R.drawable.douyin_qr_heibaijun),
                    contentDescription = "黑白君抖音二维码",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .clip(RoundedCornerShape(24.dp))
                )
            }
        }

        // 3. 使用说明：全品牌实况与微信动图指南
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesomeMotion,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "使用说明",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                GuideStepItem(
                    stepNumber = "1",
                    title = "全品牌原生相册实况兼容",
                    description = "内置全品牌通用、小米澎湃OS (MicroVideo+MotionPhoto)、OPPO/一加 (O-Live v2)、vivo/iQOO (VCamera+UUID尾盒)、华为/荣耀 (HwCamera) 与三星 OneUI (SEFH/SEFT 尾部块) 专属协议，生成后保存在相册 DCIM/Camera 中长按即可播放。"
                )

                GuideStepItem(
                    stepNumber = "2",
                    title = "微信朋友圈实况 & 微信聊天 GIF 动图",
                    description = "• 发朋友圈/小红书实况：选择「微信 / 小红书实况」模式，自动截取 ≤3 秒黄金实况片段并匹配本机相机参数，发朋友圈时左下角直接点亮实况图标。\n• 发微信聊天/表情包：选择「微信 GIF 动图」或在合成成功页点击「同时导出微信聊天 GIF 动图」，即可生成循环播放的高帧率 GIF 动图。"
                )

                GuideStepItem(
                    stepNumber = "3",
                    title = "导入本机相机样张参数 (EXIF 克隆)",
                    description = "若部分手机系统相册对第三方合成的实况照片校验严苛，可在「更多设置 → 导入相机参数」中任选一张本机原生相机拍的照片，自动克隆 25+ 项机型与镜头参数。"
                )

                GuideStepItem(
                    stepNumber = "4",
                    title = "无损拆解视频、转 GIF 与一键修复",
                    description = "在「检验」页导入任意动态照片，支持一键无损提取内嵌 MP4 视频、提取高清静态 JPG、导出微信 GIF 动图，或修复无法动态播放的实况照片。"
                )
            }
        }

        // 4. 作者与版本信息底卡
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricRow("作者", "黑白君")
                MetricRow("抖音号", "heibaijunbenjun")
                MetricRow("版本", "1.4.0")
            }
        }

        Spacer(modifier = Modifier.height(104.dp))
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
                    contentDescription = "黑白君抖音二维码",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                )
            }
        }
    }
}

@Composable
private fun GuideStepItem(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(26.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stepNumber,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private suspend fun saveQrDrawableToGallery(context: Context): String? = withContext(Dispatchers.IO) {
    try {
        val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.douyin_qr_heibaijun)
            ?: return@withContext null
        val outName = "Douyin_Heibaijun_QR.jpg"
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
                        text = "自由调节模糊度、SDF 折射曲率、光谱色散与透光率",
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
                    text = "基于 Kyant0/AndroidLiquidGlass 实时背景捕获 + SDF 四分之一圆弧透镜折射与五通道光谱色散",
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

        LiquidGlassSliderItem(
            title = "胶囊圆角弧度",
            valueText = "${config.cornerRadiusDp.roundToInt()} dp",
            value = config.cornerRadiusDp,
            valueRange = 16f..33f,
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

private fun shareMediaFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val isGif = file.name.endsWith(".gif", ignoreCase = true)
        val mime = if (isGif) "image/gif" else "image/jpeg"
        val title = if (isGif) "发送微信表情包 / 社交 GIF 动图" else "分享实况动态照片"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    } catch (_: Exception) {
    }
}

/**
 * App 内置极速本地视频/相册选择器 (InAppLocalMediaPickerSheet)：
 * - 直接查询手机本地 MediaStore 数据库，毫秒级加载本机相机视频、录屏、微信/抖音保存的视频与实况照片
 * - 100% 绕过 Android 系统 PickVisualMedia 调用的 Google 相册云端选择器，彻底杜绝“部分照片无法加载”报错弹窗
 * - 同时提供「系统文件管理」入口 (ACTION_OPEN_DOCUMENT + EXTRA_LOCAL_ONLY)，支持从任意本地文件夹选取
 */
@Composable
private fun InAppLocalMediaPickerSheet(
    purpose: LocalMediaPickerPurpose,
    refreshKey: Int,
    onLoadMedia: suspend (Boolean) -> List<MainViewModel.LocalMediaEntry>,
    onSelectSingle: (Uri) -> Unit,
    onSelectBatch: (List<Uri>) -> Unit,
    onOpenSystemDocumentPicker: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var mediaItems by remember { mutableStateOf<List<MainViewModel.LocalMediaEntry>>(emptyList()) }
    var selectedBucket by remember { mutableStateOf("全部") }
    var batchSelectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    LaunchedEffect(purpose, refreshKey) {
        isLoading = true
        mediaItems = onLoadMedia(purpose.isVideo)
        selectedBucket = "全部"
        isLoading = false
    }

    val buckets = remember(mediaItems) {
        val names = mediaItems.map { it.bucketName }.distinct()
        listOf("全部") + names
    }

    val filteredItems = remember(mediaItems, selectedBucket) {
        if (selectedBucket == "全部") {
            mediaItems
        } else {
            mediaItems.filter { it.bucketName == selectedBucket }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.84f)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = purpose.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = purpose.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalButton(
                onClick = onOpenSystemDocumentPicker,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("系统文件", fontWeight = FontWeight.Bold)
            }
        }

        if (buckets.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                buckets.forEach { bucket ->
                    val isSel = bucket == selectedBucket
                    Surface(
                        modifier = Modifier.expressiveBounceClick { selectedBucket = bucket },
                        shape = CircleShape,
                        color = if (isSel) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = bucket,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        if (purpose.isBatch && batchSelectedUris.isNotEmpty()) {
            Button(
                onClick = { onSelectBatch(batchSelectedUris) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(22.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "开始批量转换 (已选 ${batchSelectedUris.size} 个视频)",
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    ExpressiveWavyLinearProgress(
                        progress = 0.65f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    )
                }
            }

            filteredItems.isEmpty() -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VideoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (purpose.isVideo) "未扫描到本机视频或尚未授予媒体权限" else "未扫描到本机照片或尚未授予相册权限",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "您可点击下方按钮授权读取本机相册，或直接打开系统本地文件管理器挑选文件。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = onRequestPermission,
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Text("授予读取权限", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onOpenSystemDocumentPicker,
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("打开系统文件管理", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    items(
                        items = filteredItems,
                        key = { "${it.isVideo}_${it.id}" }
                    ) { entry ->
                        val selectedIndex = if (purpose.isBatch) batchSelectedUris.indexOf(entry.uri) else -1
                        val isSelected = selectedIndex >= 0

                        val thumbReq = remember(context, entry.uri, entry.isVideo) {
                            ImageRequest.Builder(context)
                                .data(entry.uri)
                                .apply {
                                    if (entry.isVideo) {
                                        decoderFactory(VideoFrameDecoder.Factory())
                                    }
                                }
                                .size(220, 220)
                                .precision(Precision.INEXACT)
                                .allowHardware(false)
                                .crossfade(false)
                                .build()
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .expressiveBounceClick(pressedScale = 0.97f) {
                                    if (purpose.isBatch) {
                                        batchSelectedUris = if (isSelected) {
                                            batchSelectedUris - entry.uri
                                        } else if (batchSelectedUris.size < 12) {
                                            batchSelectedUris + entry.uri
                                        } else {
                                            batchSelectedUris
                                        }
                                    } else {
                                        onSelectSingle(entry.uri)
                                    }
                                },
                            shape = RoundedCornerShape(22.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    AsyncImage(
                                        model = thumbReq,
                                        contentDescription = entry.displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (entry.isVideo && entry.durationMs > 0L) {
                                        val totalSec = (entry.durationMs / 1000L).coerceAtLeast(1L)
                                        val minPart = totalSec / 60L
                                        val secPart = totalSec % 60L
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Black.copy(alpha = 0.68f))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = String.format(Locale.US, "%02d:%02d", minPart, secPart),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val dimLabel = if (entry.width > 0 && entry.height > 0) {
                                        "${entry.width}×${entry.height} · "
                                    } else ""
                                    Text(
                                        text = "${entry.bucketName} · $dimLabel${formatBytes(entry.sizeBytes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (purpose.isBatch) {
                                    Surface(
                                        modifier = Modifier.size(30.dp),
                                        shape = CircleShape,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (isSelected) {
                                                Text(
                                                    text = "${selectedIndex + 1}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (entry.isVideo) Icons.Rounded.PlayArrow else Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
