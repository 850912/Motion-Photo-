package com.motionphoto.converter.ui

import android.app.Application
import android.graphics.Bitmap
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.motionphoto.converter.core.MotionPhotoBuilder
import com.motionphoto.converter.core.MotionPhotoInspector
import com.motionphoto.converter.ui.components.AppTab
import com.motionphoto.converter.ui.theme.ExpressivePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val motionPhotoBuilder = MotionPhotoBuilder(application)

    enum class LiquidGlassPreset(
        val title: String,
        val subtitle: String,
        val blurRadiusDp: Float,
        val refractionStrength: Float,
        val dispersionStrength: Float,
        val tintAlpha: Float,
        val specularStrength: Float,
        val cornerRadiusDp: Float
    ) {
        CRYSTAL_DROP(
            title = "灵动水滴",
            subtitle = "高折射清透水滴透镜质感",
            blurRadiusDp = 14f,
            refractionStrength = 0.72f,
            dispersionStrength = 0.55f,
            tintAlpha = 0.20f,
            specularStrength = 0.85f,
            cornerRadiusDp = 33f
        ),
        FROSTED_MIST(
            title = "柔雾磨砂",
            subtitle = "柔和漫射高模糊亚克力毛玻璃",
            blurRadiusDp = 28f,
            refractionStrength = 0.35f,
            dispersionStrength = 0.22f,
            tintAlpha = 0.44f,
            specularStrength = 0.55f,
            cornerRadiusDp = 33f
        ),
        PRISM_LENS(
            title = "高折棱镜",
            subtitle = "强曲面 SDF 折射与五通道光谱色散",
            blurRadiusDp = 10f,
            refractionStrength = 0.95f,
            dispersionStrength = 0.90f,
            tintAlpha = 0.16f,
            specularStrength = 0.98f,
            cornerRadiusDp = 33f
        ),
        CLASSIC_SOLID(
            title = "温润玉石",
            subtitle = "低透光高饱和温润凝脂质感",
            blurRadiusDp = 22f,
            refractionStrength = 0.42f,
            dispersionStrength = 0.28f,
            tintAlpha = 0.70f,
            specularStrength = 0.65f,
            cornerRadiusDp = 28f
        );

        companion object {
            val Entries: List<LiquidGlassPreset> = values().toList()
        }
    }

    @Immutable
    data class LiquidGlassConfig(
        val enabled: Boolean = true,
        val preset: LiquidGlassPreset? = LiquidGlassPreset.CRYSTAL_DROP,
        val blurRadiusDp: Float = 14f,
        val refractionStrength: Float = 0.72f,
        val dispersionStrength: Float = 0.55f,
        val tintAlpha: Float = 0.20f,
        val specularStrength: Float = 0.85f,
        val cornerRadiusDp: Float = 33f
    )

    @Immutable
    data class ThemeSettings(
        val palette: ExpressivePalette = ExpressivePalette.IRIS_VIOLET,
        val darkModeOverride: Boolean? = null,
        val useDynamicColor: Boolean = false,
        val liquidGlass: LiquidGlassConfig = LiquidGlassConfig()
    )

    @Immutable
    data class HistoryPhotoItem(
        val file: File,
        val key: String,
        val name: String,
        val formattedMeta: String,
        val lastModified: Long,
        val isGif: Boolean = false
    )

    @Immutable
    data class LocalMediaEntry(
        val id: Long,
        val uri: Uri,
        val displayName: String,
        val bucketName: String,
        val durationMs: Long,
        val width: Int,
        val height: Int,
        val sizeBytes: Long,
        val isVideo: Boolean
    )

    private val _themeSettings = MutableStateFlow(ThemeSettings())
    val themeSettings: StateFlow<ThemeSettings> = _themeSettings.asStateFlow()

    private val _currentTab = MutableStateFlow(AppTab.STUDIO)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    @Stable
    sealed class StudioState {
        object Idle : StudioState()

        @Stable
        data class Editing(
            val videoUri: Uri,
            val metadata: MotionPhotoBuilder.VideoMetadata,
            val timelineThumbnails: List<Bitmap> = emptyList(),
            val coverPreviewBitmap: Bitmap? = null,
            val coverTimeUs: Long = 0L,
            val trimStartMs: Long = 0L,
            val trimEndMs: Long = metadata.durationMs,
            val includeAudio: Boolean = true,
            val jpegQuality: Int = 95,
            val selectedFormat: MotionPhotoBuilder.OutputFormat = MotionPhotoBuilder.OutputFormat.UNIVERSAL,
            val customCoverImageUri: Uri? = null,
            val customCoverName: String? = null,
            val referenceExifImageUri: Uri? = null,
            val referenceExifName: String? = null,
            val longExposureMode: MotionPhotoBuilder.LongExposureMode = MotionPhotoBuilder.LongExposureMode.NONE,
            val coverFilter: MotionPhotoBuilder.CoverFilter = MotionPhotoBuilder.CoverFilter.ORIGINAL,
            val watermarkEnabled: Boolean = false,
            val isLoadingThumbnails: Boolean = true
        ) : StudioState()

        @Immutable
        data class Converting(
            val progress: Float,
            val stage: MotionPhotoBuilder.ConversionProgress.Stage,
            val detailMessage: String,
            val format: MotionPhotoBuilder.OutputFormat,
            val batchInfo: String? = null
        ) : StudioState()

        @Immutable
        data class Success(
            val result: MotionPhotoBuilder.ConversionResult,
            val batchCount: Int = 1,
            val isExportingGif: Boolean = false
        ) : StudioState()

        @Immutable
        data class Error(
            val message: String,
            val previousUri: Uri? = null
        ) : StudioState()
    }

    private val _studioState = MutableStateFlow<StudioState>(StudioState.Idle)
    val studioState: StateFlow<StudioState> = _studioState.asStateFlow()

    @Stable
    sealed class InspectorState {
        object Idle : InspectorState()
        object Inspecting : InspectorState()

        @Stable
        data class ResultReady(
            val sourceUriOrFile: Any,
            val report: MotionPhotoInspector.InspectionReport,
            val actionFeedback: String? = null,
            val isExportingGif: Boolean = false
        ) : InspectorState()

        @Immutable
        data class Error(val message: String) : InspectorState()
    }

    private val _inspectorState = MutableStateFlow<InspectorState>(InspectorState.Idle)
    val inspectorState: StateFlow<InspectorState> = _inspectorState.asStateFlow()

    private val _historyFiles = MutableStateFlow<List<HistoryPhotoItem>>(emptyList())
    val historyFiles: StateFlow<List<HistoryPhotoItem>> = _historyFiles.asStateFlow()

    private var framePreviewJob: Job? = null
    private var bannerJob: Job? = null

    init {
        refreshHistoryFiles()
    }

    fun showBannerMessage(msg: String) {
        bannerJob?.cancel()
        bannerJob = viewModelScope.launch {
            _statusBannerMessage.value = msg
            delay(2800)
            if (_statusBannerMessage.value == msg) {
                _statusBannerMessage.value = null
            }
        }
    }

    fun selectTab(tab: AppTab) {
        if (_currentTab.value == tab) return
        _currentTab.value = tab
        if (tab == AppTab.GALLERY) {
            refreshHistoryFiles()
        }
    }

    fun updatePalette(palette: ExpressivePalette) {
        _themeSettings.update { it.copy(palette = palette, useDynamicColor = false) }
    }

    fun toggleDynamicColor(enabled: Boolean) {
        _themeSettings.update { it.copy(useDynamicColor = enabled) }
    }

    fun updateLiquidGlassConfig(transform: (LiquidGlassConfig) -> LiquidGlassConfig) {
        _themeSettings.update { current ->
            val updated = transform(current.liquidGlass)
            if (updated == current.liquidGlass) current else current.copy(liquidGlass = updated)
        }
    }

    fun applyLiquidGlassPreset(preset: LiquidGlassPreset) {
        _themeSettings.update { current ->
            current.copy(
                liquidGlass = LiquidGlassConfig(
                    enabled = true,
                    preset = preset,
                    blurRadiusDp = preset.blurRadiusDp,
                    refractionStrength = preset.refractionStrength,
                    dispersionStrength = preset.dispersionStrength,
                    tintAlpha = preset.tintAlpha,
                    specularStrength = preset.specularStrength,
                    cornerRadiusDp = preset.cornerRadiusDp
                )
            )
        }
    }

    fun cycleDarkMode(currentlyDark: Boolean) {
        _themeSettings.update { current ->
            val next = when (current.darkModeOverride) {
                null -> !currentlyDark
                true -> false
                false -> null
            }
            current.copy(darkModeOverride = next)
        }
    }

    /**
     * 直接从本机 MediaStore 查询本地视频或图片，完全绕过 Google 相册云端选择器 (PICK_IMAGES)，
     * 彻底杜绝“部分照片无法加载”弹窗与云端未同步视频无法读取的问题。
     */
    suspend fun queryLocalDeviceMedia(isVideo: Boolean, limit: Int = 180): List<LocalMediaEntry> = withContext(Dispatchers.IO) {
        val list = ArrayList<LocalMediaEntry>()
        try {
            val resolver = getApplication<Application>().contentResolver
            if (isVideo) {
                val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.WIDTH,
                    MediaStore.Video.Media.HEIGHT,
                    MediaStore.Video.Media.SIZE
                )
                val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
                resolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                    val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                    val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                    val wCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                    val hCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                    val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)

                    while (cursor.moveToNext() && list.size < limit) {
                        val id = cursor.getLong(idCol)
                        val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                        if (size <= 0L) continue
                        val contentUri = ContentUris.withAppendedId(collection, id)
                        val name = if (nameCol >= 0) (cursor.getString(nameCol) ?: "VID_$id.mp4") else "VID_$id.mp4"
                        val bucket = if (bucketCol >= 0) (cursor.getString(bucketCol) ?: "本地视频") else "本地视频"
                        val duration = if (durCol >= 0) cursor.getLong(durCol).coerceAtLeast(0L) else 0L
                        val w = if (wCol >= 0) cursor.getInt(wCol) else 0
                        val h = if (hCol >= 0) cursor.getInt(hCol) else 0
                        list.add(
                            LocalMediaEntry(
                                id = id,
                                uri = contentUri,
                                displayName = name,
                                bucketName = bucket,
                                durationMs = duration,
                                width = w,
                                height = h,
                                sizeBytes = size,
                                isVideo = true
                            )
                        )
                    }
                }
            } else {
                val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.Media.WIDTH,
                    MediaStore.Images.Media.HEIGHT,
                    MediaStore.Images.Media.SIZE
                )
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
                resolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    val bucketCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    val wCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                    val hCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                    val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)

                    while (cursor.moveToNext() && list.size < limit) {
                        val id = cursor.getLong(idCol)
                        val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                        if (size <= 0L) continue
                        val contentUri = ContentUris.withAppendedId(collection, id)
                        val name = if (nameCol >= 0) (cursor.getString(nameCol) ?: "IMG_$id.jpg") else "IMG_$id.jpg"
                        val bucket = if (bucketCol >= 0) (cursor.getString(bucketCol) ?: "本地相册") else "本地相册"
                        val w = if (wCol >= 0) cursor.getInt(wCol) else 0
                        val h = if (hCol >= 0) cursor.getInt(hCol) else 0
                        list.add(
                            LocalMediaEntry(
                                id = id,
                                uri = contentUri,
                                displayName = name,
                                bucketName = bucket,
                                durationMs = 0L,
                                width = w,
                                height = h,
                                sizeBytes = size,
                                isVideo = false
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        list
    }

    fun selectVideo(uri: Uri, pendingCustomCoverUri: Uri? = null) {
        _currentTab.value = AppTab.STUDIO
        viewModelScope.launch {
            try {
                // 先将外部视频 Uri 稳定缓存至应用本地私有缓存文件，彻底避免 ContentProvider 管道或云端相册句柄失效
                val (localPlayableUri, originalName) = motionPhotoBuilder.cacheVideoUriToLocalFile(
                    sourceUri = uri,
                    cacheFileName = "studio_active_source.mp4"
                )
                val metadata = motionPhotoBuilder.inspectVideoMetadata(localPlayableUri, originalName)
                val initialTrimEnd = metadata.durationMs
                val defaultCoverUs = ((metadata.durationMs.coerceAtMost(2000L) / 4L) * 1000L).coerceAtLeast(1000L)
                val initialCover = motionPhotoBuilder.extractPreviewFrame(localPlayableUri, defaultCoverUs)
                val customCoverName = pendingCustomCoverUri?.let { queryFileNameAsync(it) }

                val editingState = StudioState.Editing(
                    videoUri = localPlayableUri,
                    metadata = metadata,
                    coverPreviewBitmap = initialCover,
                    coverTimeUs = defaultCoverUs,
                    trimStartMs = 0L,
                    trimEndMs = initialTrimEnd,
                    includeAudio = metadata.hasAudio,
                    customCoverImageUri = pendingCustomCoverUri,
                    customCoverName = customCoverName,
                    isLoadingThumbnails = true
                )
                _studioState.value = editingState

                val thumbs = motionPhotoBuilder.extractTimelineThumbnails(
                    videoUri = localPlayableUri,
                    durationMs = metadata.durationMs,
                    count = 8
                )
                val current = _studioState.value
                if (current is StudioState.Editing && current.videoUri == localPlayableUri) {
                    _studioState.value = current.copy(
                        timelineThumbnails = thumbs,
                        isLoadingThumbnails = false
                    )
                }
            } catch (e: Exception) {
                _studioState.value = StudioState.Error(
                    message = "无法解析所选视频：${e.localizedMessage ?: "请确认视频保存在手机本地"}",
                    previousUri = uri
                )
            }
        }
    }

    /**
     * 批量多视频一键转实况队列 (Batch Queue)
     */
    fun startBatchConversion(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (uris.size == 1) {
            selectVideo(uris.first())
            return
        }
        _currentTab.value = AppTab.STUDIO
        viewModelScope.launch {
            var lastSuccess: MotionPhotoBuilder.ConversionResult? = null
            var successCount = 0

            uris.forEachIndexed { idx, uri ->
                val batchLabel = "批量队列 (${idx + 1} / ${uris.size})"
                _studioState.value = StudioState.Converting(
                    progress = idx.toFloat() / uris.size.toFloat(),
                    stage = MotionPhotoBuilder.ConversionProgress.Stage.EXTRACTING_FRAME,
                    detailMessage = "$batchLabel：正在读取本地视频轨道...",
                    format = MotionPhotoBuilder.OutputFormat.UNIVERSAL,
                    batchInfo = batchLabel
                )

                val (localBatchUri, origName) = motionPhotoBuilder.cacheVideoUriToLocalFile(
                    sourceUri = uri,
                    cacheFileName = "studio_batch_source.mp4"
                )
                val meta = try {
                    motionPhotoBuilder.inspectVideoMetadata(localBatchUri, origName)
                } catch (_: Exception) {
                    null
                }
                val defaultCoverUs = (((meta?.durationMs ?: 2000L).coerceAtMost(2000L) / 4L) * 1000L).coerceAtLeast(1000L)

                val config = MotionPhotoBuilder.ConversionConfig(
                    frameTimeUs = defaultCoverUs,
                    format = MotionPhotoBuilder.OutputFormat.UNIVERSAL,
                    saveToSystemGallery = true
                )

                motionPhotoBuilder.convertVideoToMotionPhoto(
                    videoUri = localBatchUri,
                    config = config,
                    onProgress = { prog ->
                        val overall = (idx + prog.progress) / uris.size.toFloat()
                        _studioState.value = StudioState.Converting(
                            progress = overall,
                            stage = prog.stage,
                            detailMessage = "$batchLabel · ${prog.detailMessage}",
                            format = MotionPhotoBuilder.OutputFormat.UNIVERSAL,
                            batchInfo = batchLabel
                        )
                    }
                ).onSuccess { res ->
                    lastSuccess = res
                    successCount++
                }
            }

            refreshHistoryFiles()
            val finalResult = lastSuccess
            if (finalResult != null) {
                showBannerMessage("批量合成完成：共生成 $successCount 张，保存结果请以各文件为准")
                _studioState.value = StudioState.Success(
                    result = finalResult,
                    batchCount = successCount
                )
            } else {
                _studioState.value = StudioState.Error("批量转换失败，请检查所选视频格式")
            }
        }
    }

    fun updateCoverFrameTime(timeUs: Long) {
        val current = _studioState.value as? StudioState.Editing ?: return
        val clampedUs = timeUs.coerceIn(
            current.trimStartMs * 1000L,
            current.trimEndMs * 1000L
        )
        if (current.coverTimeUs == clampedUs && current.longExposureMode == MotionPhotoBuilder.LongExposureMode.NONE) {
            return
        }
        _studioState.value = current.copy(
            coverTimeUs = clampedUs,
            longExposureMode = MotionPhotoBuilder.LongExposureMode.NONE
        )
        refreshCoverPreviewDebounced()
    }

    fun updateTrimRange(startMs: Long, endMs: Long) {
        val current = _studioState.value as? StudioState.Editing ?: return
        val validStart = startMs.coerceIn(0L, (current.metadata.durationMs - 300L).coerceAtLeast(0L))
        val validEnd = endMs.coerceIn(validStart + 300L, current.metadata.durationMs)
        val clampedCoverUs = current.coverTimeUs.coerceIn(validStart * 1000L, validEnd * 1000L)

        if (current.trimStartMs == validStart && current.trimEndMs == validEnd && current.coverTimeUs == clampedCoverUs) {
            return
        }
        _studioState.value = current.copy(
            trimStartMs = validStart,
            trimEndMs = validEnd,
            coverTimeUs = clampedCoverUs
        )
    }

    fun updateIncludeAudio(include: Boolean) {
        val current = _studioState.value as? StudioState.Editing ?: return
        if (current.includeAudio == include) return
        _studioState.value = current.copy(includeAudio = include)
    }

    fun updateJpegQuality(quality: Int) {
        val current = _studioState.value as? StudioState.Editing ?: return
        if (current.jpegQuality == quality) return
        _studioState.value = current.copy(jpegQuality = quality)
    }

    fun updateOutputFormat(format: MotionPhotoBuilder.OutputFormat) {
        val current = _studioState.value as? StudioState.Editing ?: return
        if (current.selectedFormat == format) return
        _studioState.value = current.copy(selectedFormat = format)
    }

    fun setCustomCoverImage(uri: Uri?) {
        viewModelScope.launch {
            val name = uri?.let { queryFileNameAsync(it) }
            val current = _studioState.value as? StudioState.Editing ?: return@launch
            _studioState.value = current.copy(
                customCoverImageUri = uri,
                customCoverName = name
            )
            if (uri != null) {
                showBannerMessage("已启用图影混流：使用外部高清图 $name 作为实况主图")
            }
        }
    }

    fun setReferenceExifImage(uri: Uri?) {
        viewModelScope.launch {
            val name = uri?.let { queryFileNameAsync(it) }
            val current = _studioState.value as? StudioState.Editing ?: return@launch
            _studioState.value = current.copy(
                referenceExifImageUri = uri,
                referenceExifName = name
            )
            if (uri != null) {
                showBannerMessage("已载入 EXIF 样张：$name（合成时将自动克隆其相机参数）")
            }
        }
    }

    fun updateLongExposureMode(mode: MotionPhotoBuilder.LongExposureMode) {
        val current = _studioState.value as? StudioState.Editing ?: return
        if (current.longExposureMode == mode) return
        _studioState.value = current.copy(longExposureMode = mode)
        refreshCoverPreviewDebounced()
    }

    fun updateCoverFilter(filter: MotionPhotoBuilder.CoverFilter) {
        val current = _studioState.value as? StudioState.Editing ?: return
        if (current.coverFilter == filter) return
        _studioState.value = current.copy(coverFilter = filter)
        refreshCoverPreviewDebounced()
    }

    fun updateWatermarkEnabled(enabled: Boolean) {
        val current = _studioState.value as? StudioState.Editing ?: return
        if (current.watermarkEnabled == enabled) return
        _studioState.value = current.copy(watermarkEnabled = enabled)
    }

    private fun refreshCoverPreviewDebounced() {
        val current = _studioState.value as? StudioState.Editing ?: return
        framePreviewJob?.cancel()
        framePreviewJob = viewModelScope.launch {
            delay(45)
            val bitmap = motionPhotoBuilder.extractPreviewFrame(
                videoUri = current.videoUri,
                timeUs = current.coverTimeUs,
                filter = current.coverFilter,
                longExposureMode = current.longExposureMode,
                trimStartMs = current.trimStartMs,
                trimEndMs = current.trimEndMs
            )
            val latest = _studioState.value
            if (latest is StudioState.Editing && latest.videoUri == current.videoUri && bitmap != null) {
                _studioState.value = latest.copy(coverPreviewBitmap = bitmap)
            }
        }
    }

    fun startConversion() {
        val editing = _studioState.value as? StudioState.Editing ?: return
        val config = MotionPhotoBuilder.ConversionConfig(
            frameTimeUs = editing.coverTimeUs,
            trimStartMs = editing.trimStartMs,
            trimEndMs = editing.trimEndMs,
            includeAudio = editing.includeAudio,
            jpegQuality = editing.jpegQuality,
            format = editing.selectedFormat,
            customCoverImageUri = editing.customCoverImageUri,
            referenceExifImageUri = editing.referenceExifImageUri,
            longExposureMode = editing.longExposureMode,
            coverFilter = editing.coverFilter,
            watermarkEnabled = editing.watermarkEnabled,
            saveToSystemGallery = true
        )

        viewModelScope.launch {
            _studioState.value = StudioState.Converting(
                progress = 0.08f,
                stage = MotionPhotoBuilder.ConversionProgress.Stage.EXTRACTING_FRAME,
                detailMessage = "准备解码高清封面与视频轨道...",
                format = editing.selectedFormat
            )

            motionPhotoBuilder.convertVideoToMotionPhoto(
                videoUri = editing.videoUri,
                config = config,
                onProgress = { prog ->
                    _studioState.value = StudioState.Converting(
                        progress = prog.progress,
                        stage = prog.stage,
                        detailMessage = prog.detailMessage,
                        format = editing.selectedFormat
                    )
                }
            ).fold(
                onSuccess = { conversionResult ->
                    refreshHistoryFiles()
                    _studioState.value = StudioState.Success(conversionResult)
                },
                onFailure = { error ->
                    _studioState.value = StudioState.Error(
                        message = error.localizedMessage ?: "实况照片合成失败，请检查视频编码格式",
                        previousUri = editing.videoUri
                    )
                }
            )
        }
    }

    /**
     * 在合成成功页一键额外导出微信 GIF 动图
     */
    fun exportSuccessResultAsWeChatGif() {
        val current = _studioState.value as? StudioState.Success ?: return
        if (current.isExportingGif) return
        _studioState.value = current.copy(isExportingGif = true)
        viewModelScope.launch {
            MotionPhotoInspector.inspectFile(getApplication(), current.result.localFile).fold(
                onSuccess = { report ->
                    val videoSource = report.extractedVideoFile ?: current.result.localFile
                    motionPhotoBuilder.exportVideoToWeChatGif(
                        videoUriOrFile = videoSource,
                        trimStartMs = 0L,
                        trimEndMs = current.result.videoDurationMs.coerceAtMost(3200L)
                    ).fold(
                        onSuccess = { gifPath ->
                            refreshHistoryFiles()
                            showBannerMessage("已导出微信 GIF 动图至相册：$gifPath")
                        },
                        onFailure = { err ->
                            showBannerMessage("导出 GIF 失败：${err.localizedMessage}")
                        }
                    )
                },
                onFailure = {
                    showBannerMessage("导出 GIF 失败：无法读取内嵌视频")
                }
            )
            val latest = _studioState.value as? StudioState.Success
            if (latest != null) {
                _studioState.value = latest.copy(isExportingGif = false)
            }
        }
    }

    fun resetStudio() {
        _studioState.value = StudioState.Idle
    }

    fun inspectMotionPhotoUri(uri: Uri) {
        _currentTab.value = AppTab.INSPECTOR
        viewModelScope.launch {
            _inspectorState.value = InspectorState.Inspecting
            MotionPhotoInspector.inspectUri(getApplication(), uri).fold(
                onSuccess = { report ->
                    _inspectorState.value = InspectorState.ResultReady(uri, report)
                },
                onFailure = { err ->
                    _inspectorState.value = InspectorState.Error(err.localizedMessage ?: "无法解析该图片文件")
                }
            )
        }
    }

    fun inspectMotionPhotoFile(file: File) {
        if (file.name.endsWith(".gif", ignoreCase = true)) {
            showBannerMessage("这是微信 GIF 动图文件，可直接点击右侧分享按钮发送至微信")
            return
        }
        _currentTab.value = AppTab.INSPECTOR
        viewModelScope.launch {
            _inspectorState.value = InspectorState.Inspecting
            MotionPhotoInspector.inspectFile(getApplication(), file).fold(
                onSuccess = { report ->
                    _inspectorState.value = InspectorState.ResultReady(file, report)
                },
                onFailure = { err ->
                    _inspectorState.value = InspectorState.Error(err.localizedMessage ?: "无法解析该图片文件")
                }
            )
        }
    }

    fun exportInspectedVideo() {
        val current = _inspectorState.value as? InspectorState.ResultReady ?: return
        viewModelScope.launch {
            MotionPhotoInspector.exportEmbeddedVideoToGallery(getApplication(), current.report).fold(
                onSuccess = { path ->
                    val msg = "已无损提取内嵌 MP4 视频至系统相册：$path"
                    showBannerMessage(msg)
                    _inspectorState.value = current.copy(actionFeedback = msg)
                },
                onFailure = { err ->
                    showBannerMessage("导出视频失败：${err.localizedMessage}")
                }
            )
        }
    }

    fun exportInspectedStill() {
        val current = _inspectorState.value as? InspectorState.ResultReady ?: return
        viewModelScope.launch {
            MotionPhotoInspector.exportStillImageToGallery(
                getApplication(),
                current.sourceUriOrFile,
                current.report
            ).fold(
                onSuccess = { path ->
                    val msg = "已提取纯净静态 JPG 主图至系统相册：$path"
                    showBannerMessage(msg)
                    _inspectorState.value = current.copy(actionFeedback = msg)
                },
                onFailure = { err ->
                    showBannerMessage("导出静态图失败：${err.localizedMessage}")
                }
            )
        }
    }

    /**
     * 在「检验」页将任意实况照片中的内嵌视频一键导出为微信 GIF 动图
     */
    fun exportInspectedWeChatGif() {
        val current = _inspectorState.value as? InspectorState.ResultReady ?: return
        val videoFile = current.report.extractedVideoFile ?: return
        if (current.isExportingGif) return
        _inspectorState.value = current.copy(isExportingGif = true)
        viewModelScope.launch {
            motionPhotoBuilder.exportVideoToWeChatGif(
                videoUriOrFile = videoFile,
                trimStartMs = 0L,
                trimEndMs = 3000L
            ).fold(
                onSuccess = { path ->
                    refreshHistoryFiles()
                    val msg = "已导出微信 GIF 动图至相册：$path"
                    showBannerMessage(msg)
                    val latest = _inspectorState.value as? InspectorState.ResultReady
                    if (latest != null) {
                        _inspectorState.value = latest.copy(actionFeedback = msg, isExportingGif = false)
                    }
                },
                onFailure = { err ->
                    showBannerMessage("导出微信 GIF 失败：${err.localizedMessage}")
                    val latest = _inspectorState.value as? InspectorState.ResultReady
                    if (latest != null) {
                        _inspectorState.value = latest.copy(isExportingGif = false)
                    }
                }
            )
        }
    }

    fun repairInspectedMotionPhoto() {
        val current = _inspectorState.value as? InspectorState.ResultReady ?: return
        viewModelScope.launch {
            MotionPhotoInspector.repairMotionPhotoToGallery(
                getApplication(),
                current.sourceUriOrFile,
                current.report
            ).fold(
                onSuccess = { path ->
                    refreshHistoryFiles()
                    val msg = "修复成功！已重建全兼容 XMP 并保存至：$path"
                    showBannerMessage(msg)
                    _inspectorState.value = current.copy(actionFeedback = msg)
                },
                onFailure = { err ->
                    showBannerMessage("修复失败：${err.localizedMessage}")
                }
            )
        }
    }

    fun refreshHistoryFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(getApplication<Application>().getExternalFilesDir(null), "MotionPhotos")
            val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            val items = dir.listFiles { f ->
                f.isFile && (
                    f.name.endsWith(".jpg", ignoreCase = true) ||
                        f.name.endsWith(".jpeg", ignoreCase = true) ||
                        f.name.endsWith(".gif", ignoreCase = true)
                    )
            }?.map { f ->
                val lastMod = f.lastModified()
                val sizeStr = formatFileSize(f.length())
                val dateStr = dateFormat.format(Date(lastMod))
                val isGif = f.name.endsWith(".gif", ignoreCase = true)
                val typeTag = if (isGif) "微信GIF动图" else "实况照片"
                HistoryPhotoItem(
                    file = f,
                    key = f.absolutePath,
                    name = f.name,
                    formattedMeta = "$typeTag · $sizeStr · $dateStr",
                    lastModified = lastMod,
                    isGif = isGif
                )
            }?.sortedByDescending { it.lastModified } ?: emptyList()
            _historyFiles.value = items
        }
    }

    fun deleteHistoryFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                file.delete()
                refreshHistoryFiles()
            } catch (_: Exception) {}
        }
    }

    private suspend fun queryFileNameAsync(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            } ?: "selected_media"
        } catch (_: Exception) {
            "selected_media"
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.2f MB", mb)
    }
}
