package com.motionphoto.converter.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
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
            subtitle = "高折射清透水滴质感",
            blurRadiusDp = 16f,
            refractionStrength = 0.65f,
            dispersionStrength = 0.50f,
            tintAlpha = 0.22f,
            specularStrength = 0.80f,
            cornerRadiusDp = 32f
        ),
        FROSTED_MIST(
            title = "柔雾磨砂",
            subtitle = "柔和漫射高模糊毛玻璃",
            blurRadiusDp = 28f,
            refractionStrength = 0.30f,
            dispersionStrength = 0.20f,
            tintAlpha = 0.45f,
            specularStrength = 0.50f,
            cornerRadiusDp = 32f
        ),
        PRISM_LENS(
            title = "高折棱镜",
            subtitle = "强曲面折射与边缘色散",
            blurRadiusDp = 12f,
            refractionStrength = 0.90f,
            dispersionStrength = 0.85f,
            tintAlpha = 0.18f,
            specularStrength = 0.95f,
            cornerRadiusDp = 32f
        ),
        CLASSIC_SOLID(
            title = "温润玉石",
            subtitle = "低透光高饱和温润质感",
            blurRadiusDp = 22f,
            refractionStrength = 0.40f,
            dispersionStrength = 0.25f,
            tintAlpha = 0.72f,
            specularStrength = 0.60f,
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
        val blurRadiusDp: Float = 16f,
        val refractionStrength: Float = 0.65f,
        val dispersionStrength: Float = 0.50f,
        val tintAlpha: Float = 0.22f,
        val specularStrength: Float = 0.80f,
        val cornerRadiusDp: Float = 32f
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
        val lastModified: Long
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
            val batchCount: Int = 1
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
            val actionFeedback: String? = null
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

    fun selectVideo(uri: Uri, pendingCustomCoverUri: Uri? = null) {
        _currentTab.value = AppTab.STUDIO
        viewModelScope.launch {
            try {
                val metadata = motionPhotoBuilder.inspectVideoMetadata(uri)
                val initialTrimEnd = metadata.durationMs
                val defaultCoverUs = (metadata.durationMs.coerceAtMost(2000L) / 4L) * 1000L
                val initialCover = motionPhotoBuilder.extractPreviewFrame(uri, defaultCoverUs)
                val customCoverName = pendingCustomCoverUri?.let { queryFileNameAsync(it) }

                val editingState = StudioState.Editing(
                    videoUri = uri,
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
                    videoUri = uri,
                    durationMs = metadata.durationMs,
                    count = 8
                )
                val current = _studioState.value
                if (current is StudioState.Editing && current.videoUri == uri) {
                    _studioState.value = current.copy(
                        timelineThumbnails = thumbs,
                        isLoadingThumbnails = false
                    )
                }
            } catch (e: Exception) {
                _studioState.value = StudioState.Error(
                    message = "无法解析所选视频：${e.localizedMessage ?: "未知错误"}",
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
                    detailMessage = "$batchLabel：正在解析视频轨道...",
                    format = MotionPhotoBuilder.OutputFormat.UNIVERSAL,
                    batchInfo = batchLabel
                )

                val meta = try {
                    motionPhotoBuilder.inspectVideoMetadata(uri)
                } catch (_: Exception) {
                    null
                }
                val defaultCoverUs = ((meta?.durationMs ?: 2000L).coerceAtMost(2000L) / 4L) * 1000L

                val config = MotionPhotoBuilder.ConversionConfig(
                    frameTimeUs = defaultCoverUs,
                    format = MotionPhotoBuilder.OutputFormat.UNIVERSAL,
                    saveToSystemGallery = true
                )

                motionPhotoBuilder.convertVideoToMotionPhoto(
                    videoUri = uri,
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
                showBannerMessage("批量合成完成！共成功导出 $successCount 张实况照片至系统相册")
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
                f.isFile && (f.name.endsWith(".jpg", ignoreCase = true) || f.name.endsWith(".jpeg", ignoreCase = true))
            }?.map { f ->
                val lastMod = f.lastModified()
                val sizeStr = formatFileSize(f.length())
                val dateStr = dateFormat.format(Date(lastMod))
                HistoryPhotoItem(
                    file = f,
                    key = f.absolutePath,
                    name = f.name,
                    formattedMeta = "$sizeStr · $dateStr",
                    lastModified = lastMod
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
