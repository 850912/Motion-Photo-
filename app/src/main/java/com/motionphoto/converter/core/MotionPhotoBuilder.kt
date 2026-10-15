package com.motionphoto.converter.core

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * 增强版 Motion Photo 构建器
 * 支持多种格式：Google 标准、华为格式、小米格式
 * 兼容国产手机品牌和主流社交应用
 */
class MotionPhotoBuilder(private val context: Context) {
    
    data class ConversionProgress(
        val stage: Stage,
        val progress: Float
    ) {
        enum class Stage {
            EXTRACTING_FRAME,
            EXTRACTING_VIDEO,
            WRITING_METADATA,
            COMBINING,
            COMPLETE
        }
    }
    
    enum class OutputFormat {
        GOOGLE_STANDARD,    // Google/OPPO/小米 标准格式
        XIAOMI_ENHANCED,    // 小米增强格式（额外的 MicroVideo 标签）
        HUAWEI_COMPATIBLE,  // 华为/荣耀兼容格式
        UNIVERSAL           // 通用格式（多种元数据组合）
    }
    
    /**
     * 从视频转换为 Motion Photo
     * @param videoUri 源视频 URI
     * @param outputFile 输出的 Motion Photo 文件
     * @param frameTimeUs 提取帧的时间点（微秒），默认为视频开始
     * @param format 输出格式，默认为通用格式
     * @param onProgress 进度回调
     */
    suspend fun convertVideoToMotionPhoto(
        videoUri: Uri,
        outputFile: File,
        frameTimeUs: Long = 0L,
        format: OutputFormat = OutputFormat.UNIVERSAL,
        onProgress: ((ConversionProgress) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // 1. 提取关键帧作为主图片
            onProgress?.invoke(ConversionProgress(ConversionProgress.Stage.EXTRACTING_FRAME, 0.1f))
            val frameFile = extractFrameAsJpeg(videoUri, frameTimeUs)
            
            // 2. 提取完整视频（无损，使用 MediaMuxer 确保兼容性）
            onProgress?.invoke(ConversionProgress(ConversionProgress.Stage.EXTRACTING_VIDEO, 0.3f))
            val videoFile = extractVideoWithMuxer(videoUri)
            
            // 3. 获取视频元数据
            val videoMetadata = getVideoMetadata(videoUri)
            
            // 4. 根据格式写入相应的 XMP 元数据
            onProgress?.invoke(ConversionProgress(ConversionProgress.Stage.WRITING_METADATA, 0.6f))
            val jpegWithXmp = addXmpMetadata(frameFile, videoFile, videoMetadata, format)
            
            // 5. 合并 JPEG 和视频
            onProgress?.invoke(ConversionProgress(ConversionProgress.Stage.COMBINING, 0.8f))
            combineJpegAndVideo(jpegWithXmp, videoFile, outputFile)
            
            // 清理临时文件
            frameFile.delete()
            videoFile.delete()
            jpegWithXmp.delete()
            
            onProgress?.invoke(ConversionProgress(ConversionProgress.Stage.COMPLETE, 1.0f))
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 提取视频帧并保存为高质量 JPEG
     */
    private fun extractFrameAsJpeg(videoUri: Uri, timeUs: Long): File {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        
        // 获取最接近的关键帧，使用高质量选项
        val bitmap = retriever.getFrameAtTime(
            timeUs,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        ) ?: throw IllegalStateException("Failed to extract frame")
        
        val tempFile = File(context.cacheDir, "temp_frame_${System.currentTimeMillis()}.jpg")
        FileOutputStream(tempFile).use { out ->
            // 使用最高质量保存 JPEG（95% 质量）
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        
        bitmap.recycle()
        retriever.release()
        
        return tempFile
    }
    
    /**
     * 使用 MediaMuxer 提取视频（确保兼容性）
     * MediaMuxer 输出的 MP4 容器格式兼容性更好
     */
    private fun extractVideoWithMuxer(videoUri: Uri): File {
        val extractor = MediaExtractor()
        context.contentResolver.openFileDescriptor(videoUri, "r")?.use { pfd ->
            extractor.setDataSource(pfd.fileDescriptor)
        }
        
        // 找到视频轨道
        val videoTrackIndex = (0 until extractor.trackCount).firstOrNull { index ->
            val format = extractor.getTrackFormat(index)
            format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
        } ?: throw IllegalStateException("No video track found")
        
        // 找到音频轨道（如果存在）
        val audioTrackIndex = (0 until extractor.trackCount).firstOrNull { index ->
            val format = extractor.getTrackFormat(index)
            format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        }
        
        val tempFile = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
        
        // 使用 MediaMuxer 重新打包（确保标准 MP4 容器）
        val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        
        try {
            val videoFormat = extractor.getTrackFormat(videoTrackIndex)
            val muxerVideoTrack = muxer.addTrack(videoFormat)
            
            var muxerAudioTrack = -1
            if (audioTrackIndex != null) {
                val audioFormat = extractor.getTrackFormat(audioTrackIndex)
                muxerAudioTrack = muxer.addTrack(audioFormat)
            }
            
            muxer.start()
            
            // 复制视频数据
            extractor.selectTrack(videoTrackIndex)
            extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            
            val bufferInfo = android.media.MediaCodec.BufferInfo()
            val buffer = ByteBuffer.allocate(1024 * 1024) // 1MB buffer
            
            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                
                if (bufferInfo.size < 0) break
                
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                
                muxer.writeSampleData(muxerVideoTrack, buffer, bufferInfo)
                extractor.advance()
            }
            
            // 复制音频数据（如果存在）
            if (audioTrackIndex != null && muxerAudioTrack >= 0) {
                extractor.unselectTrack(videoTrackIndex)
                extractor.selectTrack(audioTrackIndex)
                extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                
                while (true) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    
                    if (bufferInfo.size < 0) break
                    
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    bufferInfo.flags = extractor.sampleFlags
                    
                    muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                    extractor.advance()
                }
            }
            
            muxer.stop()
        } finally {
            muxer.release()
            extractor.release()
        }
        
        return tempFile
    }
    
    /**
     * 获取视频元数据
     */
    private fun getVideoMetadata(videoUri: Uri): VideoMetadata {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/mp4"
        
        retriever.release()
        
        return VideoMetadata(
            width = width,
            height = height,
            durationMs = duration,
            mimeType = mimeType
        )
    }
    
    /**
     * 根据指定格式添加 XMP 元数据到 JPEG
     */
    private fun addXmpMetadata(
        jpegFile: File,
        videoFile: File,
        videoMetadata: VideoMetadata,
        format: OutputFormat
    ): File {
        val xmpMetadata = when (format) {
            OutputFormat.GOOGLE_STANDARD -> {
                XmpMetadataWriter.createGoogleStandardXmp(
                    videoLength = videoFile.length(),
                    videoDurationMs = videoMetadata.durationMs,
                    videoMimeType = videoMetadata.mimeType
                )
            }
            OutputFormat.XIAOMI_ENHANCED -> {
                XmpMetadataWriter.createXiaomiEnhancedXmp(
                    videoLength = videoFile.length(),
                    videoDurationMs = videoMetadata.durationMs,
                    videoMimeType = videoMetadata.mimeType
                )
            }
            OutputFormat.HUAWEI_COMPATIBLE -> {
                XmpMetadataWriter.createHuaweiCompatibleXmp(
                    videoLength = videoFile.length(),
                    videoDurationMs = videoMetadata.durationMs,
                    videoMimeType = videoMetadata.mimeType
                )
            }
            OutputFormat.UNIVERSAL -> {
                XmpMetadataWriter.createUniversalXmp(
                    videoLength = videoFile.length(),
                    videoDurationMs = videoMetadata.durationMs,
                    videoMimeType = videoMetadata.mimeType
                )
            }
        }
        
        val outputFile = File(context.cacheDir, "temp_with_xmp_${System.currentTimeMillis()}.jpg")
        XmpMetadataWriter.injectXmpIntoJpeg(jpegFile, xmpMetadata, outputFile)
        
        return outputFile
    }
    
    /**
     * 合并 JPEG 和视频文件
     */
    private fun combineJpegAndVideo(jpegFile: File, videoFile: File, outputFile: File) {
        FileOutputStream(outputFile).use { out ->
            // 写入 JPEG 数据
            jpegFile.inputStream().use { it.copyTo(out) }
            // 直接追加视频数据
            videoFile.inputStream().use { it.copyTo(out) }
        }
    }
    
    data class VideoMetadata(
        val width: Int,
        val height: Int,
        val durationMs: Long,
        val mimeType: String
    )
}
