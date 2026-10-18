package com.motionphoto.converter.core

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * 工业级 Android Motion Photo & 微信/社交动图合成引擎 (v3.3 全厂商深度核验版)
 *
 * 核心升级与修复：
 * 1. 全厂商容器深度核验：
 *    - Google Pixel (Android Motion Photo 1.0 官方规范 `PXL_*MP.jpg`)
 *    - 小米 / Redmi (澎湃OS HyperOS & MIUI 双轨 MicroVideo + MotionPhoto + 非零微秒时间戳)
 *    - OPPO / 一加 / Realme (ColorOS O-Live Photo v2 `OpCamera` 命名空间与双时间戳)
 *    - vivo / iQOO (OriginOS `VCamera` 元数据 + 240字节 `vivoMediaExtInfo` UUID 尾盒)
 *    - 华为 / 荣耀 (HarmonyOS / MagicOS `HwCamera:LivePhoto` 偏移声明 + MP4 `free` 盒内嵌 20 字节 `LIVE_264______` 尾标)
 *    - 三星 Galaxy (OneUI 专属 `MotionPhoto_Data` 24字节头 + `SEFH`/`SEFT` 32字节尾部索引，并同步设置 `Primary:Padding="24"` 使 `Item:Length` 精确指向 MP4 `ftyp` 起点)
 * 2. 微信与社交 App 实况 + 动图全场景支持：
 *    - 「微信 / 小红书实况 (`WECHAT_SOCIAL`)」：自动对齐 ≤3.0 秒黄金实况窗口、H.264+AAC、本机原生相机 EXIF 与全协议 XMP，直发微信朋友圈实况、小红书、微博、抖音
 *    - 「微信表情包动图 (`WECHAT_GIF`)」：自动控制尺寸 ≤300px 且体积 <950KB，100% 满足微信「添加到表情」严苛体积限制，绝不提示“表情太大”
 *    - 「高清社交 GIF 动图 (`SOCIAL_HD_GIF`)」：480p 14fps 帧间透明差值压缩 GIF，适配微信聊天窗口、QQ、微博、酷安等 App 动图直发
 * 3. 修复 Android 8.0+ `Bitmap.Config.HARDWARE` 硬件位图 `getPixels()` 崩溃问题及公有相册路径 `MediaScannerConnection` 扫描延迟问题
 */
class MotionPhotoBuilder(private val context: Context) {

    companion object {
        private const val IO_BUFFER_SIZE = 64 * 1024
        private const val MAX_CUSTOM_COVER_DIMENSION = 3840
    }

    data class ConversionProgress(
        val stage: Stage,
        val progress: Float,
        val detailMessage: String = ""
    ) {
        enum class Stage {
            EXTRACTING_FRAME,
            EXTRACTING_VIDEO,
            WRITING_METADATA,
            COMBINING,
            SAVING_TO_GALLERY,
            COMPLETE
        }
    }

    enum class OutputFormat(
        val displayName: String,
        val badge: String,
        val subtitle: String,
        val description: String,
        val makeTag: String,
        val modelTag: String,
        val lensTag: String,
        val filePrefix: String,
        val isAnimatedGif: Boolean = false
    ) {
        UNIVERSAL(
            displayName = "全品牌通用实况",
            badge = "推荐",
            subtitle = "小米 / OPPO / vivo / 华为 / 三星 / Google 相册",
            description = "同时注入 MotionPhoto 1.0、MicroVideo、O-Live、VCamera 与 HwCamera 多重标准元数据",
            makeTag = Build.MANUFACTURER.ifBlank { "Xiaomi" },
            modelTag = Build.MODEL.ifBlank { "25010PN30C" },
            lensTag = "24mm f/1.68 OIS",
            filePrefix = "MVIMG"
        ),
        WECHAT_SOCIAL(
            displayName = "微信 / 小红书实况",
            badge = "朋友圈",
            subtitle = "微信朋友圈实况 / 小红书 / 微博 / 抖音直发",
            description = "自动优化 ≤3.0 秒黄金实况时长并注入本机原生相机 EXIF + 全协议 XMP，朋友圈与小红书选图时直接点亮实况图标",
            makeTag = Build.MANUFACTURER.ifBlank { "Xiaomi" },
            modelTag = Build.MODEL.ifBlank { "Xiaomi 15 Pro" },
            lensTag = "24mm f/1.63 OIS",
            filePrefix = "IMG"
        ),
        WECHAT_GIF(
            displayName = "微信表情包动图",
            badge = "<1MB 表情",
            subtitle = "专为微信「添加到表情」与聊天自动播放定制 (≤300px, <950KB)",
            description = "启用帧间差值透明压缩与 <950KB 体积守卫，发给微信好友后可直接长按「添加到表情」永不报“表情太大”",
            makeTag = Build.MANUFACTURER.ifBlank { "Android" },
            modelTag = Build.MODEL.ifBlank { "MotionStudio" },
            lensTag = "GIF89a Sticker",
            filePrefix = "WXEMOJI",
            isAnimatedGif = true
        ),
        SOCIAL_HD_GIF(
            displayName = "高清社交 GIF 动图",
            badge = "聊天动图",
            subtitle = "适配微信聊天 / QQ / 微博 / 酷安 / 贴吧高清循环动图 (480p 14fps)",
            description = "更高分辨率与帧率的 256 色 Bayer 抖动循环 GIF 动图，在各大社交与社区 App 聊天窗口中清晰循环播放",
            makeTag = Build.MANUFACTURER.ifBlank { "Android" },
            modelTag = Build.MODEL.ifBlank { "MotionStudio" },
            lensTag = "GIF89a HD",
            filePrefix = "GIF",
            isAnimatedGif = true
        ),
        XIAOMI_ENHANCED(
            displayName = "小米 / Redmi (澎湃OS)",
            badge = "小米",
            subtitle = "小米澎湃OS / MIUI 相册长按动态播放与徕卡参数",
            description = "严格遵循小米澎湃OS 动态照片 SDK 规范，写入 GCamera:MicroVideo + MotionPhoto 双标签及非零微秒时间戳",
            makeTag = "Xiaomi",
            modelTag = "Xiaomi 15 Ultra",
            lensTag = "LEICA VARIO-SUMMILUX 1:1.63-2.5/14-100 ASPH.",
            filePrefix = "MVIMG"
        ),
        OPPO_OLIVE(
            displayName = "OPPO / 一加 / Realme",
            badge = "OPPO",
            subtitle = "ColorOS 相册 O-Live Photo v2 与哈苏光学参数",
            description = "包含 OpCamera O-Live Photo v2 专有命名空间、MotionPhotoOwner=oplus 与双时间戳同步",
            makeTag = "OPPO",
            modelTag = "OPPO Find X8 Pro",
            lensTag = "Hasselblad 23mm f/1.6 OIS",
            filePrefix = "IMG"
        ),
        VIVO_ORIGINOS(
            displayName = "vivo / iQOO (OriginOS)",
            badge = "vivo",
            subtitle = "OriginOS 实况相册 VCamera + vivo UUID 尾盒兼容",
            description = "注入 VCamera:VMotionPhotoVersion 与 MP4 尾部 240 字节 vivoMediaExtInfo 实况校验块，同时兼容 vivo 相册与 Google 相册",
            makeTag = "vivo",
            modelTag = "V2408A",
            lensTag = "ZEISS Vario-Apo-Sonnar 1:1.57-2.67/15-85 ASPH.",
            filePrefix = "IMG"
        ),
        HUAWEI_COMPATIBLE(
            displayName = "华为 / 荣耀 (HarmonyOS)",
            badge = "华为",
            subtitle = "HarmonyOS / MagicOS 图库动态照片与 LIVE_264 尾标",
            description = "包含 HwCamera:LivePhoto 偏移量、时长声明及 MP4 尾部 LIVE_264______ 兼容尾标",
            makeTag = "HUAWEI",
            modelTag = "HUAWEI Pura 70 Ultra",
            lensTag = "XMAGE Ultra Lighting 22.5mm f/1.6",
            filePrefix = "IMG"
        ),
        SAMSUNG_ONEUI(
            displayName = "三星 Galaxy (OneUI)",
            badge = "三星",
            subtitle = "注入三星 SEFH/SEFT MotionPhoto_Data 尾部校验块",
            description = "构建三星 MotionPhoto_Data 24字节头 (Primary:Padding=24) + SEFH/SEFT 尾块，三星相册与 Google Photos 双端无损识别",
            makeTag = "samsung",
            modelTag = "SM-S938B",
            lensTag = "Galaxy 200MP Adaptive Pixel 23mm f/1.7",
            filePrefix = "MotionPhoto"
        ),
        GOOGLE_STANDARD(
            displayName = "Google Pixel 原生",
            badge = "标准",
            subtitle = "Google 相册 / Pixel / 原生 Android 15+",
            description = "严格遵循 Android Motion Photo 1.0 官方规范 (PXL_*MP.jpg)",
            makeTag = "Google",
            modelTag = "Pixel 9 Pro XL",
            lensTag = "Pixel 50MP Octa PD 24mm f/1.68",
            filePrefix = "PXL"
        );

        companion object {
            val Entries: List<OutputFormat> = values().toList()
        }
    }

    /**
     * 动态长曝光堆栈模式 (参考 wszqkzqk/live-photo-conv --long-exposure)
     */
    enum class LongExposureMode(
        val title: String,
        val subtitle: String
    ) {
        NONE("单帧", "使用当前时间点的视频画面"),
        SILKY_BLUR("慢门", "多帧叠加模糊，适合流水与车流"),
        LIGHT_TRAIL("光轨", "保留画面亮部轨迹，适合夜景");

        companion object {
            val Entries: List<LongExposureMode> = values().toList()
        }
    }

    /**
     * 静态封面帧调色滤镜
     */
    enum class CoverFilter(
        val title: String
    ) {
        ORIGINAL("原图"),
        VIVID("鲜明"),
        WARM_FILM("暖色"),
        MONOCHROME("黑白");

        companion object {
            val Entries: List<CoverFilter> = values().toList()
        }
    }

    data class ConversionConfig(
        val frameTimeUs: Long = 0L,
        val trimStartMs: Long = 0L,
        val trimEndMs: Long = -1L,
        val includeAudio: Boolean = true,
        val jpegQuality: Int = 95,
        val format: OutputFormat = OutputFormat.UNIVERSAL,
        val customCoverImageUri: Uri? = null,
        val referenceExifImageUri: Uri? = null,
        val longExposureMode: LongExposureMode = LongExposureMode.NONE,
        val coverFilter: CoverFilter = CoverFilter.ORIGINAL,
        val watermarkEnabled: Boolean = false,
        val saveToSystemGallery: Boolean = true
    )

    data class VideoMetadata(
        val displayName: String,
        val width: Int,
        val height: Int,
        val rotationDegrees: Int,
        val durationMs: Long,
        val mimeType: String,
        val fileSizeBytes: Long,
        val hasAudio: Boolean,
        val frameRate: Float
    ) {
        val displayWidth: Int
            get() = if (rotationDegrees == 90 || rotationDegrees == 270) height else width
        val displayHeight: Int
            get() = if (rotationDegrees == 90 || rotationDegrees == 270) width else height
        val aspectRatio: Float
            get() = if (displayHeight > 0) displayWidth.toFloat() / displayHeight.toFloat() else 16f / 9f
    }

    data class ConversionResult(
        val localFile: File,
        val galleryUri: Uri?,
        val galleryDisplayPath: String,
        val format: OutputFormat,
        val jpegSizeBytes: Long,
        val videoSizeBytes: Long,
        val totalSizeBytes: Long,
        val coverTimestampUs: Long,
        val videoDurationMs: Long,
        val resolution: String,
        val usedCustomCover: Boolean = false,
        val clonedReferenceExif: Boolean = false,
        val longExposureMode: LongExposureMode = LongExposureMode.NONE,
        val isGif: Boolean = false
    )

    /**
     * 将任意外部 ContentUri / SAF Uri / 相册 Uri 稳定缓存至应用本地私有缓存文件，
     * 彻底杜绝 Google 相册云端选择器、第三方文件管理器权限过期或管道描述符无法 Seek 导致的加载失败。
     */
    suspend fun cacheVideoUriToLocalFile(
        sourceUri: Uri,
        cacheFileName: String = "studio_active_source.mp4"
    ): Pair<Uri, String?> = withContext(Dispatchers.IO) {
        if (sourceUri.scheme == "file") {
            val f = sourceUri.path?.let { File(it) }
            if (f != null && f.exists() && f.length() > 0L) {
                return@withContext Pair(sourceUri, f.name)
            }
        }

        var queriedName: String? = null
        try {
            context.contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIdx >= 0) {
                    queriedName = cursor.getString(nameIdx)
                }
            }
        } catch (_: Exception) {}

        val targetFile = File(context.cacheDir, cacheFileName)
        var copied = false

        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BufferedOutputStream(FileOutputStream(targetFile), IO_BUFFER_SIZE).use { output ->
                    input.copyTo(output, IO_BUFFER_SIZE)
                    output.flush()
                }
                copied = targetFile.exists() && targetFile.length() > 64L
            }
        } catch (_: Exception) {
            copied = false
        }

        if (!copied) {
            try {
                context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                    FileInputStream(pfd.fileDescriptor).use { input ->
                        BufferedOutputStream(FileOutputStream(targetFile), IO_BUFFER_SIZE).use { output ->
                            input.copyTo(output, IO_BUFFER_SIZE)
                            output.flush()
                        }
                    }
                    copied = targetFile.exists() && targetFile.length() > 64L
                }
            } catch (_: Exception) {
                copied = false
            }
        }

        if (copied) {
            Pair(Uri.fromFile(targetFile), queriedName)
        } else {
            Pair(sourceUri, queriedName)
        }
    }

    private fun bindRetrieverDataSource(retriever: MediaMetadataRetriever, videoUri: Uri) {
        if (videoUri.scheme == "file") {
            val path = videoUri.path
            if (!path.isNullOrBlank() && File(path).exists()) {
                retriever.setDataSource(path)
                return
            }
        }
        try {
            context.contentResolver.openFileDescriptor(videoUri, "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)
                return
            }
        } catch (_: Exception) {}
        try {
            retriever.setDataSource(context, videoUri)
        } catch (e: Exception) {
            throw IllegalStateException("无法读取所选视频，请确认文件可用后重试", e)
        }
    }

    private fun bindExtractorDataSource(extractor: MediaExtractor, videoUri: Uri) {
        if (videoUri.scheme == "file") {
            val path = videoUri.path
            if (!path.isNullOrBlank() && File(path).exists()) {
                extractor.setDataSource(path)
                return
            }
        }
        context.contentResolver.openFileDescriptor(videoUri, "r")?.use { pfd ->
            extractor.setDataSource(pfd.fileDescriptor)
            return
        }
        extractor.setDataSource(context, videoUri, null)
    }

    suspend fun inspectVideoMetadata(
        videoUri: Uri,
        displayNameHint: String? = null
    ): VideoMetadata = withContext(Dispatchers.IO) {
        var width = 0
        var height = 0
        var rotation = 0
        var duration = 0L
        var mimeType = "video/mp4"
        var hasAudio = false
        var captureFps = 30f

        val retriever = MediaMetadataRetriever()
        try {
            bindRetrieverDataSource(retriever, videoUri)
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/mp4"
            hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null
            captureFps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull() ?: 30f
        } catch (_: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        // 若 MediaMetadataRetriever 未能获取到宽高或时长，使用 MediaExtractor 轨道元数据兜底
        if (width <= 0 || height <= 0 || duration <= 0L) {
            val extractor = MediaExtractor()
            try {
                bindExtractorDataSource(extractor, videoUri)
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val trackMime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (trackMime.startsWith("video/")) {
                        if (width <= 0 && format.containsKey(MediaFormat.KEY_WIDTH)) {
                            width = format.getInteger(MediaFormat.KEY_WIDTH)
                        }
                        if (height <= 0 && format.containsKey(MediaFormat.KEY_HEIGHT)) {
                            height = format.getInteger(MediaFormat.KEY_HEIGHT)
                        }
                        if (rotation == 0 && format.containsKey(MediaFormat.KEY_ROTATION)) {
                            rotation = format.getInteger(MediaFormat.KEY_ROTATION)
                        }
                        if (duration <= 0L && format.containsKey(MediaFormat.KEY_DURATION)) {
                            duration = format.getLong(MediaFormat.KEY_DURATION) / 1000L
                        }
                    } else if (trackMime.startsWith("audio/")) {
                        hasAudio = true
                    }
                }
            } catch (_: Exception) {
            } finally {
                try {
                    extractor.release()
                } catch (_: Exception) {}
            }
        }

        var name = displayNameHint ?: "video_clip.mp4"
        var size = 0L
        if (videoUri.scheme == "file") {
            val f = videoUri.path?.let { File(it) }
            if (f != null && f.exists()) {
                if (displayNameHint.isNullOrBlank()) name = f.name
                size = f.length()
            }
        } else {
            try {
                context.contentResolver.query(videoUri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0 && displayNameHint.isNullOrBlank()) {
                            name = cursor.getString(nameIdx) ?: name
                        }
                        if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                    }
                }
            } catch (_: Exception) {}
        }

        VideoMetadata(
            displayName = name,
            width = if (width > 0) width else 1080,
            height = if (height > 0) height else 1920,
            rotationDegrees = rotation,
            durationMs = duration.coerceAtLeast(300L),
            mimeType = mimeType,
            fileSizeBytes = size,
            hasAudio = hasAudio,
            frameRate = captureFps
        )
    }

    suspend fun extractTimelineThumbnails(
        videoUri: Uri,
        durationMs: Long,
        count: Int = 8
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val thumbnails = ArrayList<Bitmap>(count)
        try {
            bindRetrieverDataSource(retriever, videoUri)
            val safeDurationUs = (durationMs.coerceAtLeast(100L)) * 1000L
            for (i in 0 until count) {
                val fraction = if (count > 1) i.toFloat() / (count - 1).toFloat() else 0f
                val timeUs = (safeDurationUs * fraction).toLong()
                val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        200,
                        200
                    )
                } else {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
                if (raw != null) {
                    thumbnails.add(ensureSoftwareBitmap(raw))
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
        thumbnails
    }

    suspend fun extractPreviewFrame(
        videoUri: Uri,
        timeUs: Long,
        filter: CoverFilter = CoverFilter.ORIGINAL,
        longExposureMode: LongExposureMode = LongExposureMode.NONE,
        trimStartMs: Long = 0L,
        trimEndMs: Long = 3000L
    ): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            bindRetrieverDataSource(retriever, videoUri)
            val baseBitmap = if (longExposureMode != LongExposureMode.NONE) {
                synthesizeLongExposureBitmap(
                    retriever = retriever,
                    startUs = trimStartMs * 1000L,
                    endUs = trimEndMs * 1000L,
                    mode = longExposureMode,
                    targetMaxDim = 840
                )
            } else {
                val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(
                        timeUs.coerceAtLeast(0L),
                        MediaMetadataRetriever.OPTION_CLOSEST,
                        840,
                        840
                    ) ?: retriever.getFrameAtTime(timeUs.coerceAtLeast(0L), MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } else {
                    retriever.getFrameAtTime(timeUs.coerceAtLeast(0L), MediaMetadataRetriever.OPTION_CLOSEST)
                        ?: retriever.getFrameAtTime(timeUs.coerceAtLeast(0L), MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
                raw?.let { ensureSoftwareBitmap(it) }
            } ?: return@withContext null

            if (filter == CoverFilter.ORIGINAL) {
                baseBitmap
            } else {
                val filtered = applyFilterAndWatermark(baseBitmap, filter, false, OutputFormat.UNIVERSAL)
                if (filtered !== baseBitmap) {
                    baseBitmap.recycle()
                }
                filtered
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * 一键将视频或内嵌 MP4 导出为微信聊天/表情包兼容的循环 GIF 动图
     */
    suspend fun exportVideoToWeChatGif(
        videoUriOrFile: Any,
        trimStartMs: Long = 0L,
        trimEndMs: Long = 2800L,
        stickerMode: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val prefix = if (stickerMode) "WXEMOJI" else "GIF"
            val gifName = "${prefix}_${sdf.format(Date())}.gif"
            val appOutputDir = File(context.getExternalFilesDir(null), "MotionPhotos").apply {
                if (!exists()) mkdirs()
            }
            val localGifFile = File(appOutputDir, gifName)

            val ok = GifEncoder.encodeVideoToGif(
                context = context,
                videoUriOrFile = videoUriOrFile,
                outputGifFile = localGifFile,
                startMs = trimStartMs,
                endMs = trimEndMs,
                targetFps = if (stickerMode) 11 else 14,
                maxDimension = if (stickerMode) 300 else 480,
                maxBytesBudget = if (stickerMode) 950L * 1024L else 4500L * 1024L
            )
            if (!ok || !localGifFile.exists()) {
                return@withContext Result.failure(IllegalStateException("GIF 动图编码失败"))
            }

            saveGifToMediaStore(localGifFile, gifName)
            val sizeKb = localGifFile.length() / 1024L
            Result.success("Pictures/MotionStudio/$gifName (${sizeKb}KB)")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 核心合成流程：支持全品牌实况照片、微信朋友圈/小红书实况、微信表情包 GIF (<1MB) 与高清社交 GIF 动图直接生成
     */
    suspend fun convertVideoToMotionPhoto(
        videoUri: Uri,
        config: ConversionConfig = ConversionConfig(),
        onProgress: ((ConversionProgress) -> Unit)? = null
    ): Result<ConversionResult> = withContext(Dispatchers.IO) {
        var frameFile: File? = null
        var videoFile: File? = null
        var jpegWithXmp: File? = null

        try {
            val videoMetadata = inspectVideoMetadata(videoUri)
            var actualTrimStartMs = config.trimStartMs.coerceIn(0L, (videoMetadata.durationMs - 100L).coerceAtLeast(0L))
            var actualTrimEndMs = if (config.trimEndMs <= actualTrimStartMs) {
                videoMetadata.durationMs
            } else {
                config.trimEndMs.coerceAtMost(videoMetadata.durationMs)
            }

            // 若选择「微信 / 小红书实况」模式且视频超过 3.1 秒，自动围绕所选封面帧截取 3.0 秒黄金实况窗口
            if (config.format == OutputFormat.WECHAT_SOCIAL && (actualTrimEndMs - actualTrimStartMs) > 3100L) {
                val centerMs = (config.frameTimeUs / 1000L).coerceIn(actualTrimStartMs, actualTrimEndMs)
                val halfWindow = 1500L
                actualTrimStartMs = (centerMs - halfWindow).coerceAtLeast(0L)
                actualTrimEndMs = (actualTrimStartMs + 3000L).coerceAtMost(videoMetadata.durationMs)
                if (actualTrimEndMs - actualTrimStartMs < 3000L) {
                    actualTrimStartMs = (actualTrimEndMs - 3000L).coerceAtLeast(0L)
                }
            }

            val trimmedDurationMs = (actualTrimEndMs - actualTrimStartMs).coerceAtLeast(100L)

            // 分支 A：若用户选择了「微信表情包动图」或「高清社交 GIF 动图」，直接走 GIF89a 动图编码流水线
            if (config.format.isAnimatedGif) {
                val isSticker = config.format == OutputFormat.WECHAT_GIF
                onProgress?.invoke(
                    ConversionProgress(
                        ConversionProgress.Stage.EXTRACTING_VIDEO,
                        0.25f,
                        if (isSticker) {
                            "正在提取视频帧并编码为 <1MB 微信表情包 GIF 动图..."
                        } else {
                            "正在提取视频帧并编码为 480p 高清社交 GIF 动图..."
                        }
                    )
                )
                val gifName = buildCompliantFileName(config.format)
                val appOutputDir = File(context.getExternalFilesDir(null), "MotionPhotos").apply {
                    if (!exists()) mkdirs()
                }
                val finalGifFile = File(appOutputDir, gifName)
                val encoded = GifEncoder.encodeVideoToGif(
                    context = context,
                    videoUriOrFile = videoUri,
                    outputGifFile = finalGifFile,
                    startMs = actualTrimStartMs,
                    endMs = actualTrimEndMs,
                    targetFps = if (isSticker) 11 else 14,
                    maxDimension = if (isSticker) 300 else 480,
                    maxBytesBudget = if (isSticker) 950L * 1024L else 4500L * 1024L,
                    onProgress = { p ->
                        onProgress?.invoke(
                            ConversionProgress(
                                ConversionProgress.Stage.COMBINING,
                                0.25f + p * 0.65f,
                                "正在编码 ${config.format.displayName} (${(p * 100).toInt()}%)..."
                            )
                        )
                    }
                )
                if (!encoded || !finalGifFile.exists()) {
                    throw IllegalStateException("GIF 动图编码失败，请检查所选视频")
                }

                var galleryUri: Uri? = null
                var galleryPath = finalGifFile.absolutePath
                if (config.saveToSystemGallery) {
                    onProgress?.invoke(
                        ConversionProgress(
                            ConversionProgress.Stage.SAVING_TO_GALLERY,
                            0.95f,
                            "正在保存 GIF 动图至系统相册 (Pictures/MotionStudio/$gifName)..."
                        )
                    )
                    galleryUri = saveGifToMediaStore(finalGifFile, gifName)
                    if (galleryUri != null) {
                        galleryPath = "Pictures/MotionStudio/$gifName"
                    }
                }

                onProgress?.invoke(
                    ConversionProgress(
                        ConversionProgress.Stage.COMPLETE,
                        1.0f,
                        "${config.format.displayName}制作完成！"
                    )
                )

                val tagLabel = if (isSticker) "微信表情 GIF ≤300px" else "高清 GIF 480p"
                return@withContext Result.success(
                    ConversionResult(
                        localFile = finalGifFile,
                        galleryUri = galleryUri,
                        galleryDisplayPath = galleryPath,
                        format = config.format,
                        jpegSizeBytes = finalGifFile.length(),
                        videoSizeBytes = 0L,
                        totalSizeBytes = finalGifFile.length(),
                        coverTimestampUs = 0L,
                        videoDurationMs = trimmedDurationMs.coerceAtMost(3600L),
                        resolution = tagLabel,
                        isGif = true
                    )
                )
            }

            // 分支 B：标准及各品牌定制实况照片合成流水线
            val frameStageDesc = when {
                config.customCoverImageUri != null -> "正在编码外部高清自定义封面图并写入 EXIF..."
                config.longExposureMode != LongExposureMode.NONE -> "正在进行多帧堆栈合成 (${config.longExposureMode.title})..."
                else -> "正在提取 ${(config.frameTimeUs / 1000L)}ms 处的高清静态封面帧..."
            }
            onProgress?.invoke(
                ConversionProgress(
                    ConversionProgress.Stage.EXTRACTING_FRAME,
                    0.18f,
                    frameStageDesc
                )
            )

            frameFile = preparePrimaryJpegWithExif(
                videoUri = videoUri,
                config = config,
                trimStartUs = actualTrimStartMs * 1000L,
                trimEndUs = actualTrimEndMs * 1000L
            )

            // 2. 无损重封装并裁剪 MP4 视频流
            onProgress?.invoke(
                ConversionProgress(
                    ConversionProgress.Stage.EXTRACTING_VIDEO,
                    0.45f,
                    "正在无损封装动态视频流 (${trimmedDurationMs}ms)..."
                )
            )
            videoFile = extractVideoWithMuxer(
                videoUri = videoUri,
                trimStartUs = actualTrimStartMs * 1000L,
                trimEndUs = actualTrimEndMs * 1000L,
                includeAudio = config.includeAudio,
                rotationDegrees = videoMetadata.rotationDegrees
            )

            // 2a. 若为 vivo / iQOO 模式或当前手机为 vivo/iQOO，在 MP4 尾部追加 240 字节 vivo 专属实况 UUID 校验块
            val isVivoDevice = Build.MANUFACTURER.contains("vivo", ignoreCase = true) ||
                Build.BRAND.contains("vivo", ignoreCase = true) ||
                Build.BRAND.contains("iqoo", ignoreCase = true)
            if (config.format == OutputFormat.VIVO_ORIGINOS ||
                (isVivoDevice && (config.format == OutputFormat.UNIVERSAL || config.format == OutputFormat.WECHAT_SOCIAL))
            ) {
                appendVivoLivePhotoUuidBox(videoFile)
            }

            // 2b. 若为华为 / 荣耀模式，在 MP4 尾部追加标准 MP4 free 盒包裹的 20 字节 LIVE_264______ 尾标
            if (config.format == OutputFormat.HUAWEI_COMPATIBLE) {
                appendHuaweiLivePhotoFreeBox(videoFile)
            }

            // 2c. 若为三星 OneUI 模式，构建 24 字节 MotionPhoto_Data 头 + MP4 + 32 字节 SEFH/SEFT 尾块
            // 重要：XMP 中的 Item:Length 必须等于 (rawMp4Length + 32)，Primary:Padding 设为 24，
            // 使 (totalFileSize - Item:Length) 精确命中 MP4 的 ftyp 起始字节，同时三星 SEF 解析器从尾部回跳精准命中 MotionPhoto_Data！
            val isSamsungSef = config.format == OutputFormat.SAMSUNG_ONEUI
            val rawMp4Length = videoFile.length()
            val finalVideoPayloadFile = if (isSamsungSef) {
                buildSamsungSefVideoPayload(videoFile)
            } else {
                videoFile
            }
            val declaredXmpVideoLength = if (isSamsungSef) {
                rawMp4Length + 32L
            } else {
                finalVideoPayloadFile.length()
            }
            val primaryPaddingBytes = if (isSamsungSef) 24 else 0

            // 3. 计算封面帧相对微秒时间戳（确保 >= 1000L，兼容小米澎湃OS SDK 与微信实况解析器）
            val relativePresentationUs = (config.frameTimeUs - actualTrimStartMs * 1000L)
                .coerceIn(1000L, (trimmedDurationMs * 1000L).coerceAtLeast(1000L))

            onProgress?.invoke(
                ConversionProgress(
                    ConversionProgress.Stage.WRITING_METADATA,
                    0.72f,
                    "正在注入 ${config.format.displayName} XMP 容器目录..."
                )
            )
            val xmpPacket = XmpMetadataWriter.createXmpPacket(
                format = config.format,
                videoLength = declaredXmpVideoLength,
                presentationTimestampUs = relativePresentationUs,
                videoDurationMs = trimmedDurationMs,
                videoMimeType = "video/mp4",
                primaryPadding = primaryPaddingBytes
            )

            jpegWithXmp = File(context.cacheDir, "temp_xmp_${System.currentTimeMillis()}.jpg")
            XmpMetadataWriter.injectXmpIntoJpeg(frameFile, xmpPacket, jpegWithXmp)
            val jpegPortionLength = jpegWithXmp.length()
            val videoPortionLength = finalVideoPayloadFile.length()

            // 4. 合并主静态图像与尾部视频流
            onProgress?.invoke(
                ConversionProgress(
                    ConversionProgress.Stage.COMBINING,
                    0.86f,
                    "正在紧密拼接 JPEG 主图像与 MP4 媒体轨..."
                )
            )
            val fileName = buildCompliantFileName(config.format)
            val appOutputDir = File(context.getExternalFilesDir(null), "MotionPhotos").apply {
                if (!exists()) mkdirs()
            }
            val finalLocalFile = File(appOutputDir, fileName)
            combineJpegAndVideo(jpegWithXmp, finalVideoPayloadFile, finalLocalFile)
            if (finalVideoPayloadFile !== videoFile) {
                finalVideoPayloadFile.delete()
            }

            // 5. 写入系统相册 MediaStore (DCIM/Camera) 并对公有相册路径主动触发 MediaScanner 索引以便微信/小红书即刻识别
            var galleryUri: Uri? = null
            var galleryPath = finalLocalFile.absolutePath
            if (config.saveToSystemGallery) {
                onProgress?.invoke(
                    ConversionProgress(
                        ConversionProgress.Stage.SAVING_TO_GALLERY,
                        0.95f,
                        "正在同步至系统相册 (DCIM/Camera/$fileName)..."
                    )
                )
                galleryUri = saveToMediaStoreGallery(finalLocalFile, fileName)
                galleryPath = if (galleryUri != null) {
                    "DCIM/Camera/$fileName"
                } else {
                    "相册保存失败，可分享本地生成文件"
                }
            }

            onProgress?.invoke(
                ConversionProgress(
                    ConversionProgress.Stage.COMPLETE,
                    1.0f,
                    "实况照片合成完成！"
                )
            )

            Result.success(
                ConversionResult(
                    localFile = finalLocalFile,
                    galleryUri = galleryUri,
                    galleryDisplayPath = galleryPath,
                    format = config.format,
                    jpegSizeBytes = jpegPortionLength,
                    videoSizeBytes = videoPortionLength,
                    totalSizeBytes = finalLocalFile.length(),
                    coverTimestampUs = relativePresentationUs,
                    videoDurationMs = trimmedDurationMs,
                    resolution = "${videoMetadata.displayWidth} × ${videoMetadata.displayHeight}",
                    usedCustomCover = config.customCoverImageUri != null,
                    clonedReferenceExif = config.referenceExifImageUri != null,
                    longExposureMode = config.longExposureMode,
                    isGif = false
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            frameFile?.delete()
            videoFile?.delete()
            jpegWithXmp?.delete()
        }
    }

    /**
     * 各品牌规范文件名生成：
     * 严格匹配 Android 官方动态照片文件名正则 `^([^\s\/\\][^\/\\]*MP)\.(JPG|jpg)`
     */
    private fun buildCompliantFileName(format: OutputFormat): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)
        val timestamp = sdf.format(Date())
        return if (format.isAnimatedGif) {
            "${format.filePrefix}_${timestamp}.gif"
        } else {
            "${format.filePrefix}_${timestamp}.MP.jpg"
        }
    }

    /**
     * 确保 Bitmap 为软件可读写配置 (ARGB_8888)，防止 Android 8.0+ Hardware Bitmap 调用 getPixels() 崩溃
     */
    private fun ensureSoftwareBitmap(bmp: Bitmap): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bmp.config == Bitmap.Config.HARDWARE) {
            val copy = bmp.copy(Bitmap.Config.ARGB_8888, false)
            bmp.recycle()
            copy ?: bmp
        } else {
            bmp
        }
    }

    /**
     * 为 vivo / iQOO OriginOS 在 MP4 文件尾部追加 240 字节标准 `uuid` 扩展信息盒
     * 包含 `vivoMediaExtInfo`、`com.vivo.gallery.file.convert: 10004`、`com.android.camera.livephoto`、`cameralbum!`
     */
    private fun appendVivoLivePhotoUuidBox(mp4File: File) {
        try {
            val boxSize = 240
            val buf = ByteBuffer.allocate(boxSize).order(ByteOrder.BIG_ENDIAN)
            buf.putInt(boxSize)
            buf.put("uuid".toByteArray(Charsets.US_ASCII))
            val vivoUuid = byteArrayOf(
                0x76, 0x69, 0x76, 0x6F, 0x4D, 0x65, 0x64, 0x69,
                0x61, 0x45, 0x78, 0x74, 0x49, 0x6E, 0x66, 0x6F
            )
            buf.put(vivoUuid)
            val payload = "vivoMediaExtInfo\u0000com.vivo.gallery.file.convert: 10004\u0000com.android.camera.livephoto\u0000cameralbum!".toByteArray(Charsets.UTF_8)
            buf.put(payload)
            while (buf.position() < boxSize - 12) {
                buf.put(0.toByte())
            }
            buf.put("cameralbum!".toByteArray(Charsets.US_ASCII), 0, 11)
            buf.put(0.toByte())

            FileOutputStream(mp4File, true).use { out ->
                out.write(buf.array())
                out.flush()
            }
        } catch (_: Exception) {
        }
    }

    /**
     * 为华为 / 荣耀在 MP4 文件尾部追加 28 字节标准 MP4 `free` 盒，其末尾 20 字节为华为实况尾标：
     * `[8字节: MP4 视频长度 ASCII 十进制对齐或大端偏移] + ["LIVE_264______"]`
     * 由于包裹在合法的 MP4 `free` 盒中，任何标准 MP4 解复用器均可无损跳过，同时兼容旧版 EMUI / HarmonyOS 图库尾标扫描
     */
    private fun appendHuaweiLivePhotoFreeBox(mp4File: File) {
        try {
            val totalVideoLen = mp4File.length() + 28L
            val boxSize = 28
            val buf = ByteBuffer.allocate(boxSize).order(ByteOrder.BIG_ENDIAN)
            buf.putInt(boxSize)
            buf.put("free".toByteArray(Charsets.US_ASCII))
            // 20-byte Huawei LIVE trailer inside the free box: 6-byte padding/len + 14-byte "LIVE_264______"
            val lenStr = String.format(Locale.US, "%06d", (totalVideoLen % 1000000L))
            buf.put(lenStr.toByteArray(Charsets.US_ASCII), 0, 6)
            buf.put("LIVE_264______".toByteArray(Charsets.US_ASCII), 0, 14)

            FileOutputStream(mp4File, true).use { out ->
                out.write(buf.array())
                out.flush()
            }
        } catch (_: Exception) {
        }
    }

    /**
     * 为三星 OneUI 构建包含 `MotionPhoto_Data` 24字节头与 `SEFH` / `SEFT` 32字节尾部校验索引的混合视频载荷
     */
    private fun buildSamsungSefVideoPayload(rawMp4File: File): File {
        val sefFile = File(context.cacheDir, "temp_samsung_sef_${System.currentTimeMillis()}.mp4")
        val markerName = "MotionPhoto_Data".toByteArray(Charsets.US_ASCII) // 16 bytes
        val headerBuf = ByteBuffer.allocate(2 + 2 + 4 + markerName.size).order(ByteOrder.LITTLE_ENDIAN)
        headerBuf.put(0.toByte())
        headerBuf.put(0.toByte())
        headerBuf.putShort(0x0A30.toShort()) // marker 0x0A30 -> \x30\x0A
        headerBuf.putInt(markerName.size)    // 16
        headerBuf.put(markerName)

        val tsbsef = headerBuf.capacity() + rawMp4File.length().toInt()

        val footerBuf = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
        footerBuf.put("SEFH".toByteArray(Charsets.US_ASCII))
        footerBuf.putInt(106)                // SEF version 106
        footerBuf.putInt(1)                  // 1 field
        footerBuf.put(0.toByte())
        footerBuf.put(0.toByte())
        footerBuf.putShort(0x0A30.toShort()) // field marker 0x0A30
        footerBuf.putInt(tsbsef)             // offset from SEFH backwards to start of MotionPhoto_Data
        footerBuf.putInt(tsbsef)             // field byte length
        footerBuf.putInt(20)                 // SEF directory length
        footerBuf.put("SEFT".toByteArray(Charsets.US_ASCII))

        BufferedOutputStream(FileOutputStream(sefFile), IO_BUFFER_SIZE).use { out ->
            out.write(headerBuf.array())
            BufferedInputStream(FileInputStream(rawMp4File), IO_BUFFER_SIZE).use { input ->
                input.copyTo(out, IO_BUFFER_SIZE)
            }
            out.write(footerBuf.array())
            out.flush()
        }
        return sefFile
    }

    /**
     * 准备静态主图 JPEG（支持外部高清图片混流、多帧长曝光合成、滤镜与机型水印，以及样张 EXIF 克隆）
     */
    private fun preparePrimaryJpegWithExif(
        videoUri: Uri,
        config: ConversionConfig,
        trimStartUs: Long,
        trimEndUs: Long
    ): File {
        val rawBitmap: Bitmap = when {
            config.customCoverImageUri != null -> {
                decodeBitmapFromUri(config.customCoverImageUri)
                    ?: extractSingleVideoFrame(videoUri, config.frameTimeUs)
            }
            config.longExposureMode != LongExposureMode.NONE -> {
                val retriever = MediaMetadataRetriever()
                try {
                    bindRetrieverDataSource(retriever, videoUri)
                    synthesizeLongExposureBitmap(
                        retriever = retriever,
                        startUs = trimStartUs,
                        endUs = trimEndUs,
                        mode = config.longExposureMode,
                        targetMaxDim = 1920
                    ) ?: extractSingleVideoFrame(videoUri, config.frameTimeUs)
                } finally {
                    try {
                        retriever.release()
                    } catch (_: Exception) {}
                }
            }
            else -> extractSingleVideoFrame(videoUri, config.frameTimeUs)
        }

        val styledBitmap = if (config.coverFilter != CoverFilter.ORIGINAL || config.watermarkEnabled) {
            val processed = applyFilterAndWatermark(
                source = rawBitmap,
                filter = config.coverFilter,
                watermarkEnabled = config.watermarkEnabled,
                format = config.format
            )
            if (processed !== rawBitmap) {
                rawBitmap.recycle()
            }
            processed
        } else {
            rawBitmap
        }

        val width = styledBitmap.width
        val height = styledBitmap.height
        val tempFile = File(context.cacheDir, "temp_frame_${System.currentTimeMillis()}.jpg")

        BufferedOutputStream(FileOutputStream(tempFile), IO_BUFFER_SIZE).use { out ->
            styledBitmap.compress(Bitmap.CompressFormat.JPEG, config.jpegQuality.coerceIn(75, 100), out)
        }
        styledBitmap.recycle()

        try {
            val exif = ExifInterface(tempFile.absolutePath)
            val exifDate = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date())
            exif.setAttribute(ExifInterface.TAG_MAKE, config.format.makeTag)
            exif.setAttribute(ExifInterface.TAG_MODEL, config.format.modelTag)
            exif.setAttribute(ExifInterface.TAG_LENS_MODEL, config.format.lensTag)
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, Build.DISPLAY.ifBlank { "Motion Photo 1.0" })
            exif.setAttribute(ExifInterface.TAG_DATETIME, exifDate)
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, exifDate)
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, exifDate)
            exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, "120")
            exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, "120")
            exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME_DIGITIZED, "120")
            exif.setAttribute(ExifInterface.TAG_F_NUMBER, "1.68")
            exif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, "1/125")
            exif.setAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS, "100")
            exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, "680/100")
            exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, "24")
            exif.setAttribute(ExifInterface.TAG_WHITE_BALANCE, ExifInterface.WHITE_BALANCE_AUTO.toString())
            exif.setAttribute(ExifInterface.TAG_FLASH, "0")
            exif.setAttribute(ExifInterface.TAG_COLOR_SPACE, ExifInterface.COLOR_SPACE_S_RGB.toString())
            exif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, width.toString())
            exif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, height.toString())
            exif.setAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION, width.toString())
            exif.setAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION, height.toString())
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            exif.saveAttributes()
        } catch (_: Exception) {}

        if (config.referenceExifImageUri != null) {
            cloneExifFromReferenceUri(config.referenceExifImageUri, tempFile, width, height)
        } else if (config.customCoverImageUri != null) {
            cloneExifFromReferenceUri(config.customCoverImageUri, tempFile, width, height)
        }

        return tempFile
    }

    private fun cloneExifFromReferenceUri(referenceUri: Uri, targetJpeg: File, width: Int, height: Int) {
        try {
            context.contentResolver.openInputStream(referenceUri)?.use { input ->
                val srcExif = ExifInterface(input)
                val dstExif = ExifInterface(targetJpeg.absolutePath)

                val tagsToCopy = listOf(
                    ExifInterface.TAG_MAKE,
                    ExifInterface.TAG_MODEL,
                    ExifInterface.TAG_SOFTWARE,
                    ExifInterface.TAG_DATETIME,
                    ExifInterface.TAG_DATETIME_ORIGINAL,
                    ExifInterface.TAG_DATETIME_DIGITIZED,
                    ExifInterface.TAG_SUBSEC_TIME,
                    ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
                    ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
                    ExifInterface.TAG_F_NUMBER,
                    ExifInterface.TAG_EXPOSURE_TIME,
                    ExifInterface.TAG_ISO_SPEED_RATINGS,
                    ExifInterface.TAG_FOCAL_LENGTH,
                    ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
                    ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
                    ExifInterface.TAG_WHITE_BALANCE,
                    ExifInterface.TAG_FLASH,
                    ExifInterface.TAG_METERING_MODE,
                    ExifInterface.TAG_COLOR_SPACE,
                    ExifInterface.TAG_LENS_MAKE,
                    ExifInterface.TAG_LENS_MODEL,
                    ExifInterface.TAG_GPS_LATITUDE,
                    ExifInterface.TAG_GPS_LATITUDE_REF,
                    ExifInterface.TAG_GPS_LONGITUDE,
                    ExifInterface.TAG_GPS_LONGITUDE_REF,
                    ExifInterface.TAG_USER_COMMENT
                )

                for (tag in tagsToCopy) {
                    val value = srcExif.getAttribute(tag)
                    if (!value.isNullOrBlank()) {
                        dstExif.setAttribute(tag, value)
                    }
                }
                dstExif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, width.toString())
                dstExif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, height.toString())
                dstExif.setAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION, width.toString())
                dstExif.setAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION, height.toString())
                dstExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                dstExif.saveAttributes()
            }
        } catch (_: Exception) {}
    }

    private fun decodeBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOptions)
            }
            val rawW = boundsOptions.outWidth
            val rawH = boundsOptions.outHeight
            if (rawW <= 0 || rawH <= 0) return null

            var sampleSize = 1
            while (rawW / sampleSize > MAX_CUSTOM_COVER_DIMENSION || rawH / sampleSize > MAX_CUSTOM_COVER_DIMENSION) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(BufferedInputStream(input, IO_BUFFER_SIZE), null, decodeOptions)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractSingleVideoFrame(videoUri: Uri, timeUs: Long): Bitmap {
        val retriever = MediaMetadataRetriever()
        try {
            bindRetrieverDataSource(retriever, videoUri)
            val raw = retriever.getFrameAtTime(
                timeUs.coerceAtLeast(0L),
                MediaMetadataRetriever.OPTION_CLOSEST
            ) ?: retriever.getFrameAtTime(
                timeUs.coerceAtLeast(0L),
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: retriever.frameAtTime
                ?: throw IllegalStateException("无法从视频中解码静态封面帧")
            return ensureSoftwareBitmap(raw)
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * 流式多帧堆栈长曝光合成算法（支持 Hardware Bitmap 自动转 ARGB_8888）
     */
    private fun synthesizeLongExposureBitmap(
        retriever: MediaMetadataRetriever,
        startUs: Long,
        endUs: Long,
        mode: LongExposureMode,
        targetMaxDim: Int
    ): Bitmap? {
        val safeStart = startUs.coerceAtLeast(0L)
        val safeEnd = endUs.coerceAtLeast(safeStart + 200_000L)
        val sampleCount = 6

        var w = 0
        var h = 0
        var totalPixels = 0
        var rAcc: IntArray? = null
        var gAcc: IntArray? = null
        var bAcc: IntArray? = null
        var rowPixels: IntArray? = null
        var validFrames = 0

        for (i in 0 until sampleCount) {
            val tUs = safeStart + ((safeEnd - safeStart) * i) / (sampleCount - 1)
            val rawBmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    tUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    targetMaxDim,
                    targetMaxDim
                )
            } else {
                retriever.getFrameAtTime(tUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } ?: continue

            val bmp = ensureSoftwareBitmap(rawBmp)

            if (validFrames == 0) {
                w = bmp.width
                h = bmp.height
                totalPixels = w * h
                rAcc = IntArray(totalPixels)
                gAcc = IntArray(totalPixels)
                bAcc = IntArray(totalPixels)
                rowPixels = IntArray(totalPixels)
            }

            val pixels = rowPixels ?: break
            val rArr = rAcc ?: break
            val gArr = gAcc ?: break
            val bArr = bAcc ?: break

            val scaled = if (bmp.width == w && bmp.height == h) {
                bmp
            } else {
                Bitmap.createScaledBitmap(bmp, w, h, true)
            }
            scaled.getPixels(pixels, 0, w, 0, 0, w, h)

            if (mode == LongExposureMode.SILKY_BLUR) {
                for (p in 0 until totalPixels) {
                    val c = pixels[p]
                    rArr[p] += (c shr 16) and 0xFF
                    gArr[p] += (c shr 8) and 0xFF
                    bArr[p] += c and 0xFF
                }
            } else {
                for (p in 0 until totalPixels) {
                    val c = pixels[p]
                    val r = (c shr 16) and 0xFF
                    val g = (c shr 8) and 0xFF
                    val b = c and 0xFF
                    if (validFrames == 0) {
                        rArr[p] = r
                        gArr[p] = g
                        bArr[p] = b
                    } else {
                        rArr[p] = max(rArr[p], r)
                        gArr[p] = max(gArr[p], g)
                        bArr[p] = max(bArr[p], b)
                    }
                }
            }

            validFrames++
            if (scaled !== bmp) scaled.recycle()
            bmp.recycle()
        }

        if (validFrames == 0 || w <= 0 || h <= 0) return null
        val finalPixels = rowPixels ?: return null
        val rArr = rAcc ?: return null
        val gArr = gAcc ?: return null
        val bArr = bAcc ?: return null

        for (p in 0 until totalPixels) {
            val r = if (mode == LongExposureMode.SILKY_BLUR) (rArr[p] / validFrames) else rArr[p]
            val g = if (mode == LongExposureMode.SILKY_BLUR) (gArr[p] / validFrames) else gArr[p]
            val b = if (mode == LongExposureMode.SILKY_BLUR) (bArr[p] / validFrames) else bArr[p]
            finalPixels[p] = (0xFF shl 24) or
                (r.coerceIn(0, 255) shl 16) or
                (g.coerceIn(0, 255) shl 8) or
                b.coerceIn(0, 255)
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(finalPixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun applyFilterAndWatermark(
        source: Bitmap,
        filter: CoverFilter,
        watermarkEnabled: Boolean,
        format: OutputFormat
    ): Bitmap {
        val safeSource = ensureSoftwareBitmap(source)
        val w = safeSource.width
        val h = safeSource.height
        val footerHeight = if (watermarkEnabled) (h * 0.10f).toInt().coerceAtLeast(64) else 0
        val out = Bitmap.createBitmap(w, h + footerHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        when (filter) {
            CoverFilter.ORIGINAL -> {}
            CoverFilter.VIVID -> {
                val cm = ColorMatrix().apply { setSaturation(1.32f) }
                imagePaint.colorFilter = ColorMatrixColorFilter(cm)
            }
            CoverFilter.WARM_FILM -> {
                val cm = ColorMatrix(
                    floatArrayOf(
                        1.10f, 0.04f, 0f, 0f, 8f,
                        0f, 1.03f, 0f, 0f, 4f,
                        0f, 0f, 0.90f, 0f, -6f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                imagePaint.colorFilter = ColorMatrixColorFilter(cm)
            }
            CoverFilter.MONOCHROME -> {
                val cm = ColorMatrix().apply { setSaturation(0f) }
                imagePaint.colorFilter = ColorMatrixColorFilter(cm)
            }
        }
        canvas.drawBitmap(safeSource, 0f, 0f, imagePaint)

        if (watermarkEnabled && footerHeight > 0) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
            }
            canvas.drawRect(RectF(0f, h.toFloat(), w.toFloat(), (h + footerHeight).toFloat()), bgPaint)

            val padX = w * 0.04f
            val centerY = h + footerHeight / 2f

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#18181F")
                textSize = (footerHeight * 0.30f).coerceAtLeast(18f)
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#6E6E77")
                textSize = (footerHeight * 0.21f).coerceAtLeast(13f)
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }

            canvas.drawText(
                "${format.makeTag.uppercase()} · ${format.modelTag}",
                padX,
                centerY - footerHeight * 0.06f,
                titlePaint
            )
            canvas.drawText(
                "${format.lensTag}   |   24mm  f/1.68  1/125s  ISO100",
                padX,
                centerY + footerHeight * 0.24f,
                subPaint
            )
        }

        return out
    }

    private fun extractVideoWithMuxer(
        videoUri: Uri,
        trimStartUs: Long,
        trimEndUs: Long,
        includeAudio: Boolean,
        rotationDegrees: Int
    ): File {
        return try {
            remuxVideoInternal(
                videoUri = videoUri,
                trimStartUs = trimStartUs,
                trimEndUs = trimEndUs,
                includeAudio = includeAudio,
                rotationDegrees = rotationDegrees
            )
        } catch (firstError: Exception) {
            if (!includeAudio) {
                throw IllegalStateException("视频无法封装为 MP4，请转换为 H.264/AAC MP4 后重试", firstError)
            }
            try {
                remuxVideoInternal(
                    videoUri = videoUri,
                    trimStartUs = trimStartUs,
                    trimEndUs = trimEndUs,
                    includeAudio = false,
                    rotationDegrees = rotationDegrees
                )
            } catch (videoOnlyError: Exception) {
                throw IllegalStateException(
                    "视频无法封装为 MP4，请转换为 H.264/AAC MP4 后重试",
                    videoOnlyError
                )
            }
        }
    }

    /**
     * 无损视频轨/音轨重封装：
     * 保留含 B-Frame 视频的真实相对显示时间戳 (PTS)，避免强制递增破坏 B 帧顺序引起相册与微信播放跳帧
     */
    private fun remuxVideoInternal(
        videoUri: Uri,
        trimStartUs: Long,
        trimEndUs: Long,
        includeAudio: Boolean,
        rotationDegrees: Int
    ): File {
        val extractor = MediaExtractor()
        bindExtractorDataSource(extractor, videoUri)

        val videoTrackIndex = (0 until extractor.trackCount).firstOrNull { index ->
            val format = extractor.getTrackFormat(index)
            format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
        } ?: throw IllegalStateException("未在源文件中找到视频轨道")

        val audioTrackIndex = if (includeAudio) {
            (0 until extractor.trackCount).firstOrNull { index ->
                val format = extractor.getTrackFormat(index)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                mime.startsWith("audio/mp4a") || mime == "audio/aac"
            }
        } else {
            null
        }

        val tempFile = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
        val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        try {
            if (rotationDegrees in listOf(90, 180, 270)) {
                muxer.setOrientationHint(rotationDegrees)
            }

            val videoFormat = extractor.getTrackFormat(videoTrackIndex)
            val muxerVideoTrack = muxer.addTrack(videoFormat)

            var muxerAudioTrack = -1
            if (audioTrackIndex != null) {
                try {
                    val audioFormat = extractor.getTrackFormat(audioTrackIndex)
                    muxerAudioTrack = muxer.addTrack(audioFormat)
                } catch (_: Exception) {
                    muxerAudioTrack = -1
                }
            }

            muxer.start()

            val maxSampleSize = if (videoFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(2 * 1024 * 1024)
            } else {
                2 * 1024 * 1024
            }
            val buffer = ByteBuffer.allocateDirect(maxSampleSize)
            val bufferInfo = MediaCodec.BufferInfo()

            extractor.selectTrack(videoTrackIndex)
            extractor.seekTo(trimStartUs.coerceAtLeast(0L), MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            var videoBasePtsUs = -1L
            var writtenVideoSamples = 0

            while (true) {
                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs < 0) {
                    extractor.advance()
                    continue
                }
                if (trimEndUs > trimStartUs && sampleTimeUs > trimEndUs && writtenVideoSamples > 2) {
                    break
                }

                if (videoBasePtsUs < 0L) {
                    videoBasePtsUs = sampleTimeUs
                }
                val relativePtsUs = (sampleTimeUs - videoBasePtsUs).coerceAtLeast(0L)

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = relativePtsUs
                bufferInfo.flags = sanitizeCodecFlags(extractor.sampleFlags)

                muxer.writeSampleData(muxerVideoTrack, buffer, bufferInfo)
                writtenVideoSamples++
                extractor.advance()
            }

            if (audioTrackIndex != null && muxerAudioTrack >= 0 && videoBasePtsUs >= 0L) {
                extractor.unselectTrack(videoTrackIndex)
                extractor.selectTrack(audioTrackIndex)
                extractor.seekTo(videoBasePtsUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                var lastAudioPtsUs = -1L
                while (true) {
                    buffer.clear()
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    val sampleTimeUs = extractor.sampleTime
                    if (sampleTimeUs < videoBasePtsUs) {
                        extractor.advance()
                        continue
                    }
                    if (trimEndUs > trimStartUs && sampleTimeUs > trimEndUs) {
                        break
                    }

                    var normalizedPts = (sampleTimeUs - videoBasePtsUs).coerceAtLeast(0L)
                    if (normalizedPts <= lastAudioPtsUs) {
                        normalizedPts = lastAudioPtsUs + 100L
                    }
                    lastAudioPtsUs = normalizedPts

                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = normalizedPts
                    bufferInfo.flags = sanitizeCodecFlags(extractor.sampleFlags)

                    muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                    extractor.advance()
                }
            }

            muxer.stop()
        } finally {
            try {
                muxer.release()
            } catch (_: Exception) {}
            try {
                extractor.release()
            } catch (_: Exception) {}
        }

        return tempFile
    }

    private fun sanitizeCodecFlags(sampleFlags: Int): Int {
        var flags = 0
        if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
            flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
        }
        return flags
    }

    private fun combineJpegAndVideo(jpegFile: File, videoFile: File, outputFile: File) {
        BufferedOutputStream(FileOutputStream(outputFile), IO_BUFFER_SIZE).use { out ->
            BufferedInputStream(FileInputStream(jpegFile), IO_BUFFER_SIZE).use { it.copyTo(out, IO_BUFFER_SIZE) }
            BufferedInputStream(FileInputStream(videoFile), IO_BUFFER_SIZE).use { it.copyTo(out, IO_BUFFER_SIZE) }
            out.flush()
        }
    }

    private fun saveToMediaStoreGallery(sourceFile: File, displayName: String): Uri? {
        val resolver = context.contentResolver
        var pendingUri: Uri? = null
        return try {
            val nowMillis = System.currentTimeMillis()
            val nowSeconds = nowMillis / 1000L
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, nowSeconds)
                put(MediaStore.Images.Media.DATE_MODIFIED, nowSeconds)
                put(MediaStore.Images.Media.DATE_TAKEN, nowMillis)
                put(MediaStore.Images.Media.SIZE, sourceFile.length())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/Camera")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val insertedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return null

            pendingUri = insertedUri
            val stream = resolver.openOutputStream(insertedUri)
                ?: throw IllegalStateException("无法打开相册写入流")
            stream.use { outputStream ->
                BufferedInputStream(FileInputStream(sourceFile), IO_BUFFER_SIZE).use { inputStream ->
                    inputStream.copyTo(outputStream, IO_BUFFER_SIZE)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val completeValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                check(resolver.update(insertedUri, completeValues, null, null) > 0) {
                    "无法发布相册文件"
                }
            }

            // 解析公有相册真实文件路径并触发 MediaScanner，确保微信朋友圈、小红书与系统相册即刻扫描到新生成的实况照片
            val publicCameraFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "Camera/$displayName"
            )
            MediaScannerConnection.scanFile(
                context,
                arrayOf(publicCameraFile.absolutePath),
                arrayOf("image/jpeg"),
                null
            )
            try {
                @Suppress("DEPRECATION")
                context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, insertedUri))
            } catch (_: Exception) {}

            insertedUri
        } catch (_: Exception) {
            pendingUri?.let { uri -> runCatching { resolver.delete(uri, null, null) } }
            null
        }
    }

    private fun saveGifToMediaStore(sourceGifFile: File, displayName: String): Uri? {
        val resolver = context.contentResolver
        var pendingUri: Uri? = null
        return try {
            val nowMillis = System.currentTimeMillis()
            val nowSeconds = nowMillis / 1000L
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/gif")
                put(MediaStore.Images.Media.DATE_ADDED, nowSeconds)
                put(MediaStore.Images.Media.DATE_MODIFIED, nowSeconds)
                put(MediaStore.Images.Media.DATE_TAKEN, nowMillis)
                put(MediaStore.Images.Media.SIZE, sourceGifFile.length())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/MotionStudio")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val insertedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return null

            pendingUri = insertedUri
            val stream = resolver.openOutputStream(insertedUri)
                ?: throw IllegalStateException("无法打开相册写入流")
            stream.use { outputStream ->
                BufferedInputStream(FileInputStream(sourceGifFile), IO_BUFFER_SIZE).use { inputStream ->
                    inputStream.copyTo(outputStream, IO_BUFFER_SIZE)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val completeValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                check(resolver.update(insertedUri, completeValues, null, null) > 0) {
                    "无法发布相册文件"
                }
            }

            val publicPicturesFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "MotionStudio/$displayName"
            )
            MediaScannerConnection.scanFile(
                context,
                arrayOf(publicPicturesFile.absolutePath),
                arrayOf("image/gif"),
                null
            )
            try {
                @Suppress("DEPRECATION")
                context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, insertedUri))
            } catch (_: Exception) {}

            insertedUri
        } catch (_: Exception) {
            pendingUri?.let { uri -> runCatching { resolver.delete(uri, null, null) } }
            null
        }
    }
}
