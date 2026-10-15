package com.motionphoto.converter.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.motionphoto.converter.core.MotionPhotoBuilder
import com.motionphoto.converter.core.MotionPhotoInspector
import com.motionphoto.converter.ui.components.AppTab
import com.motionphoto.converter.ui.theme.ExpressivePalette
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val motionPhotoBuilder = MotionPhotoBuilder(application)

    // 主题与视觉个性化状态
    data class ThemeSettings(
        val palette: ExpressivePalette = ExpressivePalette.IRIS_VIOLET,
        val darkModeOverride: Boolean? = null, // null = 跟随系统
        val useDynamicColor: Boolean = false
    )

    private val _themeSettings = MutableStateFlow(ThemeSettings())
    val themeSettings: StateFlow<ThemeSettings> = _themeSettings.asStateFlow()

    // 当前底部悬浮导航栏 Tab
    private val _currentTab = MutableStateFlow(AppTab.STUDIO)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // 创作工坊主状态
    sealed class StudioState {
        object Idle : StudioState()

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
            val isLoadingThumbnails: Boolean = true
        ) : StudioState()

        data class Converting(
            val progress: Float,
            val stage: MotionPhotoBuilder.ConversionProgress.Stage,
            val detailMessage: String,
            val format: MotionPhotoBuilder.OutputFormat
        ) : StudioState()

        data class Success(
            val result: MotionPhotoBuilder.ConversionResult
        ) : StudioState()

        data class Error(
            val message: String,
            val previousUri: Uri? = null
        ) : StudioState()
    }

    private val _studioState = MutableStateFlow<StudioState>(StudioState.Idle)
    val studioState: StateFlow<StudioState> = _studioState.asStateFlow()

    // 实况检验器状态
    sealed class InspectorState {
        object Idle : InspectorState()
        object Inspecting : InspectorState()
        data class ResultReady(
            val sourceUriOrFile: Any,
            val report: MotionPhotoInspector.InspectionReport
        ) : InspectorState()
        data class Error(val message: String) : InspectorState()
    }

    private val _inspectorState = MutableStateFlow<InspectorState>(InspectorState.Idle)
    val inspectorState: StateFlow<InspectorState> = _inspectorState.asStateFlow()

    // 历史转换作品列表
    private val _historyFiles = MutableStateFlow<List<File>>(emptyList())
    val historyFiles: StateFlow<List<File>> = _historyFiles.asStateFlow()

    private var framePreviewJob: Job? = null

    init {
        refreshHistoryFiles()
    }

    fun selectTab(tab: AppTab) {
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
     * 选择源视频并加载元数据、首帧预览及 8 帧胶片时间轴
     */
    fun selectVideo(uri: Uri) {
        _currentTab.value = AppTab.STUDIO
        viewModelScope.launch {
            try {
                val metadata = motionPhotoBuilder.inspectVideoMetadata(uri)
                val initialTrimEnd = metadata.durationMs
                // 默认选取视频黄金前 1/4 处或 0ms 作为初始封面帧
                val defaultCoverUs = (metadata.durationMs.coerceAtMost(2000L) / 4L) * 1000L
                val initialCover = motionPhotoBuilder.extractPreviewFrame(uri, defaultCoverUs)

                val editingState = StudioState.Editing(
                    videoUri = uri,
                    metadata = metadata,
                    coverPreviewBitmap = initialCover,
                    coverTimeUs = defaultCoverUs,
                    trimStartMs = 0L,
                    trimEndMs = initialTrimEnd,
                    includeAudio = metadata.hasAudio,
                    isLoadingThumbnails = true
                )
                _studioState.value = editingState

                // 异步加载时间轴缩略图条
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
     * 更新封面帧时间戳（带防抖实时解码预览帧）
     */
    fun updateCoverFrameTime(timeUs: Long) {
        val current = _studioState.value as? StudioState.Editing ?: return
        val clampedUs = timeUs.coerceIn(
            current.trimStartMs * 1000L,
            current.trimEndMs * 1000L
        )
        _studioState.value = current.copy(coverTimeUs = clampedUs)

        framePreviewJob?.cancel()
        framePreviewJob = viewModelScope.launch {
            delay(90)
            val bitmap = motionPhotoBuilder.extractPreviewFrame(current.videoUri, clampedUs)
            val latest = _studioState.value
            if (latest is StudioState.Editing && latest.videoUri == current.videoUri && bitmap != null) {
                _studioState.value = latest.copy(coverPreviewBitmap = bitmap)
            }
        }
    }

    /**
     * 更新视频裁剪起止区间 (毫秒)
     */
    fun updateTrimRange(startMs: Long, endMs: Long) {
        val current = _studioState.value as? StudioState.Editing ?: return
        val validStart = startMs.coerceIn(0L, (current.metadata.durationMs - 300L).coerceAtLeast(0L))
        val validEnd = endMs.coerceIn(validStart + 300L, current.metadata.durationMs)
        val clampedCoverUs = current.coverTimeUs.coerceIn(validStart * 1000L, validEnd * 1000L)

        _studioState.value = current.copy(
            trimStartMs = validStart,
            trimEndMs = validEnd,
            coverTimeUs = clampedCoverUs
        )
    }

    fun updateIncludeAudio(include: Boolean) {
        val current = _studioState.value as? StudioState.Editing ?: return
        _studioState.value = current.copy(includeAudio = include)
    }

    fun updateJpegQuality(quality: Int) {
        val current = _studioState.value as? StudioState.Editing ?: return
        _studioState.value = current.copy(jpegQuality = quality)
    }

    fun updateOutputFormat(format: MotionPhotoBuilder.OutputFormat) {
        val current = _studioState.value as? StudioState.Editing ?: return
        _studioState.value = current.copy(selectedFormat = format)
    }

    /**
     * 启动 Motion Photo 合成流程
     */
    fun startConversion() {
        val editing = _studioState.value as? StudioState.Editing ?: return
        val config = MotionPhotoBuilder.ConversionConfig(
            frameTimeUs = editing.coverTimeUs,
            trimStartMs = editing.trimStartMs,
            trimEndMs = editing.trimEndMs,
            includeAudio = editing.includeAudio,
            jpegQuality = editing.jpegQuality,
            format = editing.selectedFormat,
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

    /**
     * 在「实况检验器」中检验任意图片 Uri 或本地文件
     */
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

    fun refreshHistoryFiles() {
        viewModelScope.launch {
            val dir = File(getApplication<Application>().getExternalFilesDir(null), "MotionPhotos")
            val files = dir.listFiles { f ->
                f.isFile && (f.name.endsWith(".jpg", ignoreCase = true) || f.name.endsWith(".jpeg", ignoreCase = true))
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
            _historyFiles.value = files
        }
    }

    fun deleteHistoryFile(file: File) {
        viewModelScope.launch {
            try {
                file.delete()
                refreshHistoryFiles()
            } catch (_: Exception) {}
        }
    }
}
