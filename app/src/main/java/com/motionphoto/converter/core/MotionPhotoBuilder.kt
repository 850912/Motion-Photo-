package com.motionphoto.converter.core

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 工业级 Android Motion Photo 合成引擎 (v2.0 MD3E)
 *
 * 核心升级：
 * 1. 支持任意时间点封面帧提取 + 时间轴多帧胶片缩略图生成
 * 2. 支持视频片段裁剪 (trimStartMs .. trimEndMs) 与原声保留/静音切换
 * 3. 自动读取并写入视频旋转角度 (setOrientationHint) 与单调递增时间戳保护，彻底解决竖屏视频变形与 MediaMuxer 崩溃
 * 4. 自动写入相机 EXIF 标签 (Make/Model/DateTimeOriginal/Width/Height)，解决国产手机系统相册不识别问题
 * 5. 自动遵循 *MP.jpg 命名规范并通过 MediaStore 写入系统相册 (DCIM/Camera)，转换后立即可在系统相册与 Google Photos 查看
 */
class MotionPhotoBuilder(private val context: Context) {

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
        val filePrefix: String
    ) {
        UNIVERSAL(
            displayName = "全品牌通用格式",
            badge = "推荐 · 全兼容",
            subtitle = "Google / 小米 / OPPO / vivo / 华为",
            description = "同时注入 MotionPhoto 1.0 容器目录、MicroVideo 偏移量与 O-Live 多重元数据",
            makeTag = Build.MANUFACTURER.ifBlank { "Google" },
            modelTag = Build.MODEL.ifBlank { "Pixel 9 Pro" },
            filePrefix = "MVIMG"
        ),
        GOOGLE_STANDARD(
            displayName = "Google Pixel 原生",
            badge = "Android 16 标准",
            subtitle = "Google 相册 / Pixel / 原生 Android",
            description = "严格遵循 Android Motion Photo 1.0 官方规范 (PXL_*MP.jpg)",
            makeTag = "Google",
            modelTag = "Pixel 9 Pro",
            filePrefix = "PXL"
        ),
        XIAOMI_ENHANCED(
            displayName = "小米澎湃 HyperOS",
            badge = "小米 / Redmi",
            subtitle = "小米相册长按动态播放优化",
            description = "注入 GCamera:MicroVideo + MotionPhoto 双标签及小米相机 EXIF 标识",
            makeTag = "Xiaomi",
            modelTag = "Xiaomi 15 Pro",
            filePrefix = "MVIMG"
        ),
        OPPO_OLIVE(
            displayName = "OPPO / 一加 O-Live",
            badge = "ColorOS / 氧OS",
            subtitle = "OPPO / OnePlus / Realme 相册实况",
            description = "包含 OpCamera O-Live Photo v2 专有命名空间与双时间戳同步",
            makeTag = "OPPO",
            modelTag = "OPPO Find X8 Pro",
            filePrefix = "IMG"
        ),
        HUAWEI_COMPATIBLE(
            displayName = "华为 / 荣耀实况",
            badge = "HarmonyOS 兼容",
            subtitle = "华为与荣耀图库动态照片兼容",
            description = "包含 HwCamera:LivePhoto 偏移量及标准 Container 目录描述",
            makeTag = "HUAWEI",
            modelTag = "HUAWEI Pura 70 Ultra",
            filePrefix = "IMG"
        )
    }

    data class ConversionConfig(
        val frameTimeUs: Long = 0L,
        val trimStartMs: Long = 0L,
        val trimEndMs: Long = -1L,
        val includeAudio: Boolean = true,
        val jpegQuality: Int = 95,
        val format: OutputFormat = OutputFormat.UNIVERSAL,
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
        val resolution: String
    )

    /**
     * 读取视频详细元数据
     */
    suspend fun inspectVideoMetadata(videoUri: Uri): VideoMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1080
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1920
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 3000L
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/mp4"
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null
            val captureFps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull() ?: 30f

            var name = "video_clip.mp4"
            var size = 0L
            context.contentResolver.query(videoUri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                    if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                }
            }

            VideoMetadata(
                displayName = name,
                width = width,
                height = height,
                rotationDegrees = rotation,
                durationMs = duration.coerceAtLeast(100L),
                mimeType = mimeType,
                fileSizeBytes = size,
                hasAudio = hasAudio,
                frameRate = captureFps
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * 提取视频时间轴缩略图序列（用于封面选择器胶片条）
     */
    suspend fun extractTimelineThumbnails(
        videoUri: Uri,
        durationMs: Long,
        count: Int = 8
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val thumbnails = mutableListOf<Bitmap>()
        try {
            retriever.setDataSource(context, videoUri)
            val safeDurationUs = (durationMs.coerceAtLeast(100L)) * 1000L
            for (i in 0 until count) {
                val fraction = if (count > 1) i.toFloat() / (count - 1).toFloat() else 0f
                val timeUs = (safeDurationUs * fraction).toLong()
                val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        240,
                        240
                    )
                } else {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
                if (raw != null) {
                    thumbnails.add(raw)
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

    /**
     * 提取指定微秒时间点的预览封面帧
     */
    suspend fun extractPreviewFrame(videoUri: Uri, timeUs: Long): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    timeUs.coerceAtLeast(0L),
                    MediaMetadataRetriever.OPTION_CLOSEST,
                    960,
                    960
                ) ?: retriever.getFrameAtTime(timeUs.coerceAtLeast(0L), MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } else {
                retriever.getFrameAtTime(timeUs.coerceAtLeast(0L), MediaMetadataRetriever.OPTION_CLOSEST)
                    ?: retriever.getFrameAtTime(timeUs.coerceAtLeast(0L), MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
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
     * 核心合成流程：将源视频转换为标准 Android Motion Photo 并同步保存到系统相册
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
            val actualTrimStartMs = config.trimStartMs.coerceIn(0L, (videoMetadata.durationMs - 100L).coerceAtLeast(0L))
            val actualTrimEndMs = if (config.trimEndMs <= actualTrimStartMs) {
                videoMetadata.durationMs
            } else {
                config.trimEndMs.coerceAtMost(videoMetadata.durationMs)
            }
            val trimmedDurationMs = (actualTrimEndMs - actualTrimStartMs).coerceAtLeast(100L)

            // 1. 提取高清静态封面帧并写入相机 EXIF 元数据
            onProgress?.invoke(
                ConversionProgress(
                    ConversionProgress.Stage.EXTRACTING_FRAME,
                    0.15f,
                    "正在提取 ${(config.frameTimeUs / 1000L)}ms 处的高清静态封面帧..."
                )
            )
            frameFile = extractFrameAsJpegWithExif(
                videoUri = videoUri,
                timeUs = config.frameTimeUs,
                quality = config.jpegQuality.coerceIn(70, 100),
                format = config.format
            )

            // 2. 无损重封装并裁剪 MP4 视频流
            onProgress?.invoke(
                ConversionProgress(
                    ConversionProgress.Stage.EXTRACTING_VIDEO,
                    0.42f,
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

            // 3. 计算封面帧在裁剪后视频中的相对时间戳并注入 XMP 元数据
            val relativePresentationUs = (config.frameTimeUs - actualTrimStartMs * 1000L)
                .coerceIn(0L, trimmedDurationMs * 1000L)

            onProgress?.invoke(
                ConversionProgress(
                    ConversionProgress.Stage.WRITING_METADATA,
                    0.70f,
                    "正在写入 ${config.format.displayName} XMP 容器目录..."
                )
            )
            val xmpPacket = XmpMetadataWriter.createXmpPacket(
                format = config.format,
                videoLength = videoFile.length(),
                presentationTimestampUs = relativePresentationUs,
                videoDurationMs = trimmedDurationMs,
                videoMimeType = "video/mp4"
            )

            jpegWithXmp = File(context.cacheDir, "temp_xmp_${System.currentTimeMillis()}.jpg")
            XmpMetadataWriter.injectXmpIntoJpeg(frameFile, xmpPacket, jpegWithXmp)
            val jpegPortionLength = jpegWithXmp.length()
            val videoPortionLength = videoFile.length()

            // 4. 合并主静态图像与尾部 MP4 视频流（生成符合 *MP.jpg 规范的文件名）
            onProgress?.invoke(
                ConversionProgress(
                    ConversionProgress.Stage.COMBINING,
                    0.85f,
                    "正在紧密拼接 JPEG 主图像与 MP4 媒体轨..."
                )
            )
            val fileName = buildCompliantFileName(config.format)
            val appOutputDir = File(context.getExternalFilesDir(null), "MotionPhotos").apply {
                if (!exists()) mkdirs()
            }
            val finalLocalFile = File(appOutputDir, fileName)
            combineJpegAndVideo(jpegWithXmp, videoFile, finalLocalFile)

            // 5. 写入系统相册 MediaStore (DCIM/Camera) 以便系统相册与 Google Photos 立即识别
            var galleryUri: Uri? = null
            var galleryPath = finalLocalFile.absolutePath
            if (config.saveToSystemGallery) {
                onProgress?.invoke(
                    ConversionProgress(
                        ConversionProgress.Stage.SAVING_TO_GALLERY,
                        0.94f,
                        "正在同步至系统相册 (DCIM/Camera/$fileName)..."
                    )
                )
                galleryUri = saveToMediaStoreGallery(finalLocalFile, fileName)
                if (galleryUri != null) {
                    galleryPath = "DCIM/Camera/$fileName"
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
                    resolution = "${videoMetadata.displayWidth} × ${videoMetadata.displayHeight}"
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
     * 生成符合 Android Motion Photo 1.0 正则规范 ^([^\s\/\\][^\/\\]*MP)\.(JPG|jpg) 的文件名
     */
    private fun buildCompliantFileName(format: OutputFormat): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)
        val timestamp = sdf.format(Date())
        return "${format.filePrefix}_${timestamp}.MP.jpg"
    }

    /**
     * 提取关键帧为高质量 JPEG，并写入标准相机 EXIF 信息
     */
    private fun extractFrameAsJpegWithExif(
        videoUri: Uri,
        timeUs: Long,
        quality: Int,
        format: OutputFormat
    ): File {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val bitmap = retriever.getFrameAtTime(
                timeUs.coerceAtLeast(0L),
                MediaMetadataRetriever.OPTION_CLOSEST
            ) ?: retriever.getFrameAtTime(
                timeUs.coerceAtLeast(0L),
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: retriever.frameAtTime
                ?: throw IllegalStateException("无法从视频中解码静态封面帧")

            val width = bitmap.width
            val height = bitmap.height
            val tempFile = File(context.cacheDir, "temp_frame_${System.currentTimeMillis()}.jpg")

            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            bitmap.recycle()

            // 写入标准相机 EXIF 标签，确保小米/OPPO/华为/Google 相册将其识别为相机实拍照片
            try {
                val exif = ExifInterface(tempFile.absolutePath)
                val exifDate = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date())
                exif.setAttribute(ExifInterface.TAG_MAKE, format.makeTag)
                exif.setAttribute(ExifInterface.TAG_MODEL, format.modelTag)
                exif.setAttribute(ExifInterface.TAG_SOFTWARE, "Motion Studio MD3E")
                exif.setAttribute(ExifInterface.TAG_DATETIME, exifDate)
                exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, exifDate)
                exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, exifDate)
                exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, "000")
                exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, "000")
                exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME_DIGITIZED, "000")
                exif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, width.toString())
                exif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, height.toString())
                exif.setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL.toString()
                )
                exif.saveAttributes()
            } catch (_: Exception) {
                // EXIF 写入失败不影响主流程
            }

            return tempFile
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * 使用 MediaExtractor + MediaMuxer 无损裁剪并封装 MP4 视频流
     * 包含：旋转角度保留、时间戳归零与单调递增保护、不兼容音频轨降级重试
     */
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
        } catch (e: Exception) {
            if (includeAudio) {
                // 如果部分特殊编码音频轨导致 MediaMuxer 异常，自动降级为纯视频轨重封装
                try {
                    remuxVideoInternal(
                        videoUri = videoUri,
                        trimStartUs = trimStartUs,
                        trimEndUs = trimEndUs,
                        includeAudio = false,
                        rotationDegrees = rotationDegrees
                    )
                } catch (_: Exception) {
                    copyRawVideoFallback(videoUri)
                }
            } else {
                copyRawVideoFallback(videoUri)
            }
        }
    }

    private fun remuxVideoInternal(
        videoUri: Uri,
        trimStartUs: Long,
        trimEndUs: Long,
        includeAudio: Boolean,
        rotationDegrees: Int
    ): File {
        val extractor = MediaExtractor()
        context.contentResolver.openFileDescriptor(videoUri, "r")?.use { pfd ->
            extractor.setDataSource(pfd.fileDescriptor)
        } ?: throw IllegalStateException("无法打开视频文件描述符")

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

            // 1. 写入视频轨道数据
            extractor.selectTrack(videoTrackIndex)
            extractor.seekTo(trimStartUs.coerceAtLeast(0L), MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            var videoBasePtsUs = -1L
            var lastVideoPtsUs = -1L

            while (true) {
                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs < 0) {
                    extractor.advance()
                    continue
                }
                if (trimEndUs > trimStartUs && sampleTimeUs > trimEndUs) {
                    break
                }

                if (videoBasePtsUs < 0L) {
                    videoBasePtsUs = sampleTimeUs
                }
                var normalizedPts = (sampleTimeUs - videoBasePtsUs).coerceAtLeast(0L)
                if (normalizedPts <= lastVideoPtsUs) {
                    normalizedPts = lastVideoPtsUs + 100L
                }
                lastVideoPtsUs = normalizedPts

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = normalizedPts
                bufferInfo.flags = sanitizeCodecFlags(extractor.sampleFlags)

                muxer.writeSampleData(muxerVideoTrack, buffer, bufferInfo)
                extractor.advance()
            }

            // 2. 写入音频轨道数据（若开启）
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

    private fun copyRawVideoFallback(videoUri: Uri): File {
        val tempFile = File(context.cacheDir, "temp_raw_video_${System.currentTimeMillis()}.mp4")
        context.contentResolver.openInputStream(videoUri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("无法读取源视频流")
        return tempFile
    }

    private fun combineJpegAndVideo(jpegFile: File, videoFile: File, outputFile: File) {
        FileOutputStream(outputFile).use { out ->
            FileInputStream(jpegFile).use { it.copyTo(out) }
            FileInputStream(videoFile).use { it.copyTo(out) }
            out.flush()
        }
    }

    /**
     * 将生成的 Motion Photo 写入系统 MediaStore (DCIM/Camera)，确保所有手机相册立即可见
     */
    private fun saveToMediaStoreGallery(sourceFile: File, displayName: String): Uri? {
        return try {
            val resolver = context.contentResolver
            val nowSeconds = System.currentTimeMillis() / 1000L
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, nowSeconds)
                put(MediaStore.Images.Media.DATE_MODIFIED, nowSeconds)
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                put(MediaStore.Images.Media.SIZE, sourceFile.length())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/Camera")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val insertedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return null

            resolver.openOutputStream(insertedUri)?.use { outputStream ->
                FileInputStream(sourceFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val completeValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(insertedUri, completeValues, null, null)
            } else {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(sourceFile.absolutePath),
                    arrayOf("image/jpeg"),
                    null
                )
            }

            insertedUri
        } catch (_: Exception) {
            null
        }
    }
}
