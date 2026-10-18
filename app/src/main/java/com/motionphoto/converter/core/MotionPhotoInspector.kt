package com.motionphoto.converter.core

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.adobe.internal.xmp.XMPMeta
import com.adobe.internal.xmp.XMPMetaFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 动态照片（Motion Photo）二进制容器检验、无损拆解 (Demuxer) 与一键修复器 (Repair)
 *
 * 优化点：
 * - 全链路 64KB 缓冲流加速文件读取与分离
 * - 自动清理旧的 inspected_motion_* 临时缓存视频，防止缓存膨胀
 */
object MotionPhotoInspector {

    private const val IO_BUFFER_SIZE = 64 * 1024

    data class InspectionReport(
        val fileName: String,
        val totalFileSize: Long,
        val isValidMotionPhoto: Boolean,
        val canBeRepaired: Boolean,
        val detectedStandards: List<String>,
        val hasXmpPacket: Boolean,
        val rawXmpSnippet: String,
        val motionPhotoFlag: Int,
        val microVideoFlag: Int,
        val presentationTimestampUs: Long,
        val declaredVideoLength: Long,
        val actualMp4Offset: Long,
        val actualMp4Length: Long,
        val jpegPortionSize: Long,
        val extractedVideoFile: File?,
        val primaryMime: String,
        val secondaryMime: String,
        val MakeModelSummary: String,
        val diagnosticNotes: List<String>
    )

    private val XMP_HEADER_BYTES = "http://ns.adobe.com/xap/1.0/\u0000".toByteArray(Charsets.UTF_8)

    suspend fun inspectUri(context: Context, uri: Uri): Result<InspectionReport> = withContext(Dispatchers.IO) {
        try {
            val tempCopy = File(context.cacheDir, "inspect_source_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedOutputStream(FileOutputStream(tempCopy), IO_BUFFER_SIZE).use { out ->
                    input.copyTo(out, IO_BUFFER_SIZE)
                }
            } ?: return@withContext Result.failure(IllegalArgumentException("无法读取所选文件"))

            val displayName = queryDisplayName(context, uri) ?: tempCopy.name
            val result = inspectFile(context, tempCopy, displayNameOverride = displayName)
            tempCopy.delete()
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun inspectFile(
        context: Context,
        file: File,
        displayNameOverride: String? = null
    ): Result<InspectionReport> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() < 16) {
                return@withContext Result.failure(IllegalArgumentException("文件不存在或体积过小"))
            }

            // 清理之前检验遗留的临时视频文件
            cleanupOldInspectedVideos(context.cacheDir)

            val totalSize = file.length()
            val fileName = displayNameOverride ?: file.name
            val notes = mutableListOf<String>()
            val standards = mutableListOf<String>()

            val rawXmp = extractRawXmpFromJpeg(file)
            val xmpMeta: XMPMeta? = rawXmp?.let {
                try {
                    XMPMetaFactory.parseFromString(it)
                } catch (_: Exception) {
                    null
                }
            }

            var motionPhotoFlag = 0
            var microVideoFlag = 0
            var timestampUs = -1L
            var declaredLength = 0L

            if (rawXmp != null) {
                notes.add("成功定位 JPEG APP1 XMP 元数据段 (${rawXmp.toByteArray().size} bytes)")
                motionPhotoFlag = parseXmpInt(xmpMeta, XmpMetadataWriter.NS_GCAMERA, "MotionPhoto")
                    ?: Regex("""(?:GCamera|Camera):MotionPhoto[=">]+(\d+)""").find(rawXmp)?.groupValues?.get(1)?.toIntOrNull()
                    ?: 0

                microVideoFlag = parseXmpInt(xmpMeta, XmpMetadataWriter.NS_GCAMERA, "MicroVideo")
                    ?: Regex("""GCamera:MicroVideo[=">]+(\d+)""").find(rawXmp)?.groupValues?.get(1)?.toIntOrNull()
                    ?: 0

                timestampUs = parseXmpLong(xmpMeta, XmpMetadataWriter.NS_GCAMERA, "MotionPhotoPresentationTimestampUs")
                    ?: parseXmpLong(xmpMeta, XmpMetadataWriter.NS_GCAMERA, "MicroVideoPresentationTimestampUs")
                    ?: Regex("""MotionPhotoPresentationTimestampUs[=">]+(-?\d+)""").find(rawXmp)?.groupValues?.get(1)?.toLongOrNull()
                    ?: Regex("""MicroVideoPresentationTimestampUs[=">]+(-?\d+)""").find(rawXmp)?.groupValues?.get(1)?.toLongOrNull()
                    ?: -1L

                val itemLengths = Regex("""Item:Length="(\d+)"""").findAll(rawXmp)
                    .mapNotNull { it.groupValues[1].toLongOrNull() }
                    .filter { it > 0L }
                    .toList()
                val microOffset = parseXmpLong(xmpMeta, XmpMetadataWriter.NS_GCAMERA, "MicroVideoOffset")
                    ?: Regex("""MicroVideoOffset[=">]+(\d+)""").find(rawXmp)?.groupValues?.get(1)?.toLongOrNull()
                    ?: 0L
                val oppoLength = Regex("""OpCamera:VideoLength[=">]+(\d+)""").find(rawXmp)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                val hwOffset = Regex("""HwCamera:VideoOffset[=">]+(\d+)""").find(rawXmp)?.groupValues?.get(1)?.toLongOrNull() ?: 0L

                declaredLength = itemLengths.lastOrNull()
                    ?: microOffset.takeIf { it > 0 }
                    ?: oppoLength.takeIf { it > 0 }
                    ?: hwOffset.takeIf { it > 0 }
                    ?: 0L

                if (motionPhotoFlag == 1 && itemLengths.isNotEmpty()) {
                    standards.add("Google Motion Photo 1.0")
                }
                if (microVideoFlag == 1 || microOffset > 0) {
                    standards.add("小米 MicroVideo")
                }
                if (rawXmp.contains("OpCamera:") || rawXmp.contains("oplus")) {
                    standards.add("OPPO O-Live Photo")
                }
                if (rawXmp.contains("HwCamera:LivePhoto")) {
                    standards.add("华为实况照片")
                }
            } else {
                notes.add("未在 JPEG 头部检测到 XMP APP1 数据段")
            }

            val mp4OffsetFromFtyp = findMp4HeaderOffset(file)
            val mp4OffsetFromDeclared = if (declaredLength in 1 until totalSize) {
                totalSize - declaredLength
            } else {
                -1L
            }

            val resolvedMp4Offset = when {
                mp4OffsetFromFtyp != null && mp4OffsetFromFtyp > 0 -> mp4OffsetFromFtyp
                mp4OffsetFromDeclared > 0 -> mp4OffsetFromDeclared
                else -> -1L
            }

            val actualMp4Length = if (resolvedMp4Offset in 1 until totalSize) {
                totalSize - resolvedMp4Offset
            } else {
                0L
            }

            var extractedVideo: File? = null
            if (resolvedMp4Offset in 1 until totalSize && actualMp4Length > 64) {
                val outVideo = File(context.cacheDir, "inspected_motion_${System.currentTimeMillis()}.mp4")
                extractTailBytes(file, resolvedMp4Offset, outVideo)
                if (isValidMp4Header(outVideo)) {
                    extractedVideo = outVideo
                    notes.add("成功验证内嵌 MP4 视频容器 (偏移量 $resolvedMp4Offset, 大小 $actualMp4Length bytes)")
                    if (declaredLength > 0 && declaredLength == actualMp4Length) {
                        notes.add("XMP 声明长度与实际尾部 MP4 字节长度完全一致")
                    } else if (declaredLength > 0) {
                        notes.add("警告：XMP 声明长度 ($declaredLength) 与实际 MP4 长度 ($actualMp4Length) 不一致，可一键修复")
                    } else {
                        notes.add("警告：存在内嵌 MP4 但缺少 Container:Directory 长度声明，可一键修复")
                    }
                } else {
                    outVideo.delete()
                }
            } else {
                notes.add("未在文件尾部检出内嵌 MP4 视频流")
            }

            if (fileName.endsWith("MP.jpg", ignoreCase = true) || fileName.endsWith("MP.jpeg", ignoreCase = true)) {
                notes.add("文件名符合 Android Motion Photo 1.0 官方 *MP.jpg 规范")
            } else if (extractedVideo != null) {
                notes.add("提示：文件名未以 MP.jpg 结尾，点击「一键修复并重导出」可自动补全规范命名")
            }

            val isValid = (motionPhotoFlag == 1 || microVideoFlag == 1 || standards.isNotEmpty()) &&
                extractedVideo != null &&
                (declaredLength == 0L || declaredLength == actualMp4Length)

            val canRepair = extractedVideo != null

            Result.success(
                InspectionReport(
                    fileName = fileName,
                    totalFileSize = totalSize,
                    isValidMotionPhoto = isValid,
                    canBeRepaired = canRepair,
                    detectedStandards = if (standards.isEmpty() && extractedVideo != null) listOf("内嵌 MP4 (待修复 XMP)") else standards,
                    hasXmpPacket = rawXmp != null,
                    rawXmpSnippet = rawXmp?.trim() ?: "",
                    motionPhotoFlag = motionPhotoFlag,
                    microVideoFlag = microVideoFlag,
                    presentationTimestampUs = timestampUs,
                    declaredVideoLength = declaredLength,
                    actualMp4Offset = resolvedMp4Offset,
                    actualMp4Length = actualMp4Length,
                    jpegPortionSize = if (resolvedMp4Offset > 0) resolvedMp4Offset else totalSize,
                    extractedVideoFile = extractedVideo,
                    primaryMime = "image/jpeg",
                    secondaryMime = "video/mp4",
                    MakeModelSummary = readExifSummary(file),
                    diagnosticNotes = notes
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanupOldInspectedVideos(cacheDir: File) {
        try {
            cacheDir.listFiles { f ->
                f.isFile && f.name.startsWith("inspected_motion_") && f.name.endsWith(".mp4")
            }?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    /**
     * 将实况照片中的内嵌 MP4 独立无损导出至系统相册 Movies/MotionStudio
     */
    suspend fun exportEmbeddedVideoToGallery(
        context: Context,
        report: InspectionReport
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val videoFile = report.extractedVideoFile
                ?: return@withContext Result.failure(IllegalStateException("该照片不含可导出的内嵌视频流"))
            if (!videoFile.exists()) {
                return@withContext Result.failure(IllegalStateException("临时视频缓存已失效，请重新检验"))
            }

            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val outName = "VID_${sdf.format(Date())}_extracted.mp4"
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, outName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/MotionStudio")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext Result.failure(IllegalStateException("无法创建相册视频条目"))

            resolver.openOutputStream(uri)?.use { out ->
                BufferedInputStream(FileInputStream(videoFile), IO_BUFFER_SIZE).use { it.copyTo(out, IO_BUFFER_SIZE) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val done = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
            }
            Result.success("Movies/MotionStudio/$outName")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 剥离尾部视频，将纯净高清静态 JPG 导出至 Pictures/MotionStudio
     */
    suspend fun exportStillImageToGallery(
        context: Context,
        sourceUriOrFile: Any,
        report: InspectionReport
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val tempSource = copySourceToTemp(context, sourceUriOrFile)
                ?: return@withContext Result.failure(IllegalArgumentException("无法读取源文件"))

            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val outName = "IMG_${sdf.format(Date())}_still.jpg"
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
                ?: return@withContext Result.failure(IllegalStateException("无法创建相册图片条目"))

            val bytesToCopy = report.jpegPortionSize.coerceAtMost(tempSource.length())
            resolver.openOutputStream(uri)?.use { out ->
                BufferedInputStream(FileInputStream(tempSource), IO_BUFFER_SIZE).use { input ->
                    val buf = ByteArray(IO_BUFFER_SIZE)
                    var remaining = bytesToCopy
                    while (remaining > 0) {
                        val toRead = minOf(buf.size.toLong(), remaining).toInt()
                        val read = input.read(buf, 0, toRead)
                        if (read <= 0) break
                        out.write(buf, 0, read)
                        remaining -= read
                    }
                }
            }
            tempSource.delete()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
            }
            Result.success("Pictures/MotionStudio/$outName")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 一键修复损坏或无法被系统相册识别的实况照片
     */
    suspend fun repairMotionPhotoToGallery(
        context: Context,
        sourceUriOrFile: Any,
        report: InspectionReport
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val videoFile = report.extractedVideoFile
                ?: return@withContext Result.failure(IllegalStateException("未检测到内嵌 MP4 视频流，无法修复"))
            val tempSource = copySourceToTemp(context, sourceUriOrFile)
                ?: return@withContext Result.failure(IllegalArgumentException("无法读取源文件"))

            // 1. 截取纯净 JPEG 头部
            val pureJpeg = File(context.cacheDir, "repair_head_${System.currentTimeMillis()}.jpg")
            BufferedInputStream(FileInputStream(tempSource), IO_BUFFER_SIZE).use { input ->
                BufferedOutputStream(FileOutputStream(pureJpeg), IO_BUFFER_SIZE).use { out ->
                    val buf = ByteArray(IO_BUFFER_SIZE)
                    var remaining = report.jpegPortionSize.coerceAtMost(tempSource.length())
                    while (remaining > 0) {
                        val toRead = minOf(buf.size.toLong(), remaining).toInt()
                        val read = input.read(buf, 0, toRead)
                        if (read <= 0) break
                        out.write(buf, 0, read)
                        remaining -= read
                    }
                }
            }
            tempSource.delete()

            // 2. 重新生成全品牌兼容 XMP 并注入
            val xmpPacket = XmpMetadataWriter.createXmpPacket(
                format = MotionPhotoBuilder.OutputFormat.UNIVERSAL,
                videoLength = videoFile.length(),
                presentationTimestampUs = if (report.presentationTimestampUs >= 0) report.presentationTimestampUs else 500_000L,
                videoDurationMs = 2500L,
                videoMimeType = "video/mp4"
            )
            val jpegWithXmp = File(context.cacheDir, "repair_xmp_${System.currentTimeMillis()}.jpg")
            XmpMetadataWriter.injectXmpIntoJpeg(pureJpeg, xmpPacket, jpegWithXmp)
            pureJpeg.delete()

            // 3. 拼接并保存至系统相册 DCIM/Camera
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val outName = "MVIMG_${sdf.format(Date())}_REPAIRED.MP.jpg"
            val appOutDir = File(context.getExternalFilesDir(null), "MotionPhotos").apply {
                if (!exists()) mkdirs()
            }
            val repairedFile = File(appOutDir, outName)
            BufferedOutputStream(FileOutputStream(repairedFile), IO_BUFFER_SIZE).use { out ->
                BufferedInputStream(FileInputStream(jpegWithXmp), IO_BUFFER_SIZE).use { it.copyTo(out, IO_BUFFER_SIZE) }
                BufferedInputStream(FileInputStream(videoFile), IO_BUFFER_SIZE).use { it.copyTo(out, IO_BUFFER_SIZE) }
            }
            jpegWithXmp.delete()

            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, outName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/Camera")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    BufferedInputStream(FileInputStream(repairedFile), IO_BUFFER_SIZE).use { it.copyTo(out, IO_BUFFER_SIZE) }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                    resolver.update(uri, done, null, null)
                }
            }

            Result.success("DCIM/Camera/$outName")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun copySourceToTemp(context: Context, sourceUriOrFile: Any): File? {
        return try {
            val temp = File(context.cacheDir, "demux_src_${System.currentTimeMillis()}.jpg")
            when (sourceUriOrFile) {
                is File -> {
                    if (!sourceUriOrFile.exists()) return null
                    sourceUriOrFile.copyTo(temp, overwrite = true, bufferSize = IO_BUFFER_SIZE)
                    temp
                }
                is Uri -> {
                    context.contentResolver.openInputStream(sourceUriOrFile)?.use { input ->
                        BufferedOutputStream(FileOutputStream(temp), IO_BUFFER_SIZE).use { out ->
                            input.copyTo(out, IO_BUFFER_SIZE)
                        }
                    } ?: return null
                    temp
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractRawXmpFromJpeg(file: File): String? {
        return try {
            RandomAccessFile(file, "r").use { raf ->
                if (raf.readUnsignedShort() != 0xFFD8) return null
                while (raf.filePointer + 4 < raf.length()) {
                    val marker = raf.readUnsignedShort()
                    if (marker == 0xFFDA || marker == 0xFFD9) break
                    if (marker < 0xFF00) break
                    val segLen = raf.readUnsignedShort()
                    if (segLen < 2) break
                    if (marker == 0xFFE1) {
                        val data = ByteArray(segLen - 2)
                        raf.readFully(data)
                        if (data.size > XMP_HEADER_BYTES.size) {
                            var match = true
                            for (i in XMP_HEADER_BYTES.indices) {
                                if (data[i] != XMP_HEADER_BYTES[i]) {
                                    match = false
                                    break
                                }
                            }
                            if (match) {
                                return String(
                                    data,
                                    XMP_HEADER_BYTES.size,
                                    data.size - XMP_HEADER_BYTES.size,
                                    Charsets.UTF_8
                                )
                            }
                        }
                    } else {
                        raf.skipBytes(segLen - 2)
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun findMp4HeaderOffset(file: File): Long? {
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val len = raf.length()
                val buf = ByteArray(IO_BUFFER_SIZE)
                var offset = 0L
                while (offset < len - 12) {
                    raf.seek(offset)
                    val read = raf.read(buf)
                    if (read < 12) break
                    for (i in 4 until read - 8) {
                        if (buf[i] == 'f'.code.toByte() &&
                            buf[i + 1] == 't'.code.toByte() &&
                            buf[i + 2] == 'y'.code.toByte() &&
                            buf[i + 3] == 'p'.code.toByte()
                        ) {
                            val boxSize = ((buf[i - 4].toInt() and 0xFF) shl 24) or
                                ((buf[i - 3].toInt() and 0xFF) shl 16) or
                                ((buf[i - 2].toInt() and 0xFF) shl 8) or
                                (buf[i - 1].toInt() and 0xFF)
                            if (boxSize in 8..256) {
                                return offset + i - 4
                            }
                        }
                    }
                    offset += (read - 16).coerceAtLeast(1)
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractTailBytes(source: File, startOffset: Long, dest: File) {
        BufferedInputStream(FileInputStream(source), IO_BUFFER_SIZE).use { input ->
            BufferedOutputStream(FileOutputStream(dest), IO_BUFFER_SIZE).use { output ->
                var remainingSkip = startOffset
                while (remainingSkip > 0) {
                    val skipped = input.skip(remainingSkip)
                    if (skipped <= 0) break
                    remainingSkip -= skipped
                }
                input.copyTo(output, IO_BUFFER_SIZE)
            }
        }
    }

    private fun isValidMp4Header(file: File): Boolean {
        if (!file.exists() || file.length() < 12) return false
        return try {
            FileInputStream(file).use { input ->
                val header = ByteArray(12)
                if (input.read(header) < 12) return false
                header[4] == 'f'.code.toByte() &&
                    header[5] == 't'.code.toByte() &&
                    header[6] == 'y'.code.toByte() &&
                    header[7] == 'p'.code.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun readExifSummary(file: File): String {
        return try {
            val exif = androidx.exifinterface.media.ExifInterface(file.absolutePath)
            val make = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKE) ?: "Generic"
            val model = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL) ?: "Android Camera"
            val width = exif.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_WIDTH, 0)
            val height = exif.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_LENGTH, 0)
            "$make $model (${width}×${height})"
        } catch (_: Exception) {
            "标准 JPEG 图像"
        }
    }

    private fun parseXmpInt(xmp: XMPMeta?, ns: String, prop: String): Int? {
        return try {
            xmp?.getPropertyInteger(ns, prop)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseXmpLong(xmp: XMPMeta?, ns: String, prop: String): Long? {
        return try {
            xmp?.getPropertyLong(ns, prop)
        } catch (_: Exception) {
            null
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
