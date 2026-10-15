package com.motionphoto.converter.core

import com.adobe.xmp.XMPException
import com.adobe.xmp.XMPMeta
import com.adobe.xmp.XMPMetaFactory
import com.adobe.xmp.options.SerializeOptions
import java.io.File
import java.io.FileOutputStream

/**
 * XMP 元数据写入工具 - 增强版
 * 支持多种格式：Google 标准、小米增强、华为兼容、通用格式
 * 确保在小米、OPPO、华为、vivo 等国产手机品牌上的最大兼容性
 */
object XmpMetadataWriter {
    
    // Google 标准命名空间
    private const val NS_GCAMERA = "http://ns.google.com/photos/1.0/camera/"
    private const val NS_CONTAINER = "http://ns.google.com/photos/1.0/container/"
    private const val NS_CONTAINER_ITEM = "http://ns.google.com/photos/1.0/container/item/"
    
    // 小米 MicroVideo 命名空间（旧版兼容）
    private const val NS_MICROVIDEO = "http://ns.google.com/photos/1.0/camera/"
    
    // 华为命名空间（基于逆向工程和社区研究）
    private const val NS_HUAWEI = "http://www.huawei.com/camera/1.0/"
    
    private const val PREFIX_GCAMERA = "GCamera"
    private const val PREFIX_CONTAINER = "Container"
    private const val PREFIX_ITEM = "Item"
    private const val PREFIX_HUAWEI = "HwCamera"
    
    /**
     * Google 标准格式 XMP（OPPO/部分小米适用）
     */
    fun createGoogleStandardXmp(
        videoLength: Long,
        videoDurationMs: Long,
        videoMimeType: String = "video/mp4"
    ): String {
        return try {
            val xmp = XMPMetaFactory.create()
            
            registerNamespaces()
            
            // Camera XMP - 标识 Motion Photo
            xmp.setProperty(NS_GCAMERA, "GCamera:MotionPhoto", "1")
            xmp.setProperty(NS_GCAMERA, "GCamera:MotionPhotoVersion", "1")
            xmp.setPropertyLong(NS_GCAMERA, "GCamera:MotionPhotoPresentationTimestampUs", videoDurationMs * 1000)
            
            // Container XMP - 描述文件结构
            addContainerDirectory(xmp, videoLength, videoMimeType)
            
            serializeXmp(xmp)
        } catch (e: XMPException) {
            throw RuntimeException("Failed to create Google standard XMP", e)
        }
    }
    
    /**
     * 小米增强格式 XMP（同时包含 MicroVideo 和 MotionPhoto 标签）
     */
    fun createXiaomiEnhancedXmp(
        videoLength: Long,
        videoDurationMs: Long,
        videoMimeType: String = "video/mp4"
    ): String {
        return try {
            val xmp = XMPMetaFactory.create()
            
            registerNamespaces()
            
            // GCamera namespace - MotionPhoto 标识
            xmp.setProperty(NS_GCAMERA, "GCamera:MotionPhoto", "1")
            xmp.setProperty(NS_GCAMERA, "GCamera:MotionPhotoVersion", "1")
            xmp.setPropertyLong(NS_GCAMERA, "GCamera:MotionPhotoPresentationTimestampUs", videoDurationMs * 1000)
            
            // MicroVideo 标识（小米旧版兼容）
            xmp.setProperty(NS_GCAMERA, "GCamera:MicroVideo", "1")
            xmp.setProperty(NS_GCAMERA, "GCamera:MicroVideoVersion", "1")
            xmp.setPropertyLong(NS_GCAMERA, "GCamera:MicroVideoOffset", videoLength)
            xmp.setPropertyLong(NS_GCAMERA, "GCamera:MicroVideoPresentationTimestampUs", videoDurationMs * 1000)
            
            // Container XMP
            addContainerDirectory(xmp, videoLength, videoMimeType)
            
            serializeXmp(xmp)
        } catch (e: XMPException) {
            throw RuntimeException("Failed to create Xiaomi enhanced XMP", e)
        }
    }
    
    /**
     * 华为兼容格式 XMP（基于华为相册识别规则）
     */
    fun createHuaweiCompatibleXmp(
        videoLength: Long,
        videoDurationMs: Long,
        videoMimeType: String = "video/mp4"
    ): String {
        return try {
            val xmp = XMPMetaFactory.create()
            
            registerNamespaces()
            XMPMetaFactory.getSchemaRegistry().registerNamespace(NS_HUAWEI, PREFIX_HUAWEI)
            
            // Google 标准元数据（基础兼容）
            xmp.setProperty(NS_GCAMERA, "GCamera:MotionPhoto", "1")
            xmp.setProperty(NS_GCAMERA, "GCamera:MotionPhotoVersion", "1")
            xmp.setPropertyLong(NS_GCAMERA, "GCamera:MotionPhotoPresentationTimestampUs", videoDurationMs * 1000)
            
            // 华为特定标识
            xmp.setProperty(NS_HUAWEI, "HwCamera:LivePhoto", "1")
            xmp.setPropertyLong(NS_HUAWEI, "HwCamera:VideoOffset", videoLength)
            xmp.setPropertyLong(NS_HUAWEI, "HwCamera:VideoDuration", videoDurationMs)
            
            // Container XMP
            addContainerDirectory(xmp, videoLength, videoMimeType)
            
            serializeXmp(xmp)
        } catch (e: XMPException) {
            throw RuntimeException("Failed to create Huawei compatible XMP", e)
        }
    }
    
    /**
     * 通用格式 XMP（包含所有厂商的元数据标识）
     * 推荐使用此格式，兼容性最好
     */
    fun createUniversalXmp(
        videoLength: Long,
        videoDurationMs: Long,
        videoMimeType: String = "video/mp4"
    ): String {
        return try {
            val xmp = XMPMetaFactory.create()
            
            registerNamespaces()
            XMPMetaFactory.getSchemaRegistry().registerNamespace(NS_HUAWEI, PREFIX_HUAWEI)
            
            // Google MotionPhoto 标准（OPPO/小米/Google Photos）
            xmp.setProperty(NS_GCAMERA, "GCamera:MotionPhoto", "1")
            xmp.setProperty(NS_GCAMERA, "GCamera:MotionPhotoVersion", "1")
            xmp.setPropertyLong(NS_GCAMERA, "GCamera:MotionPhotoPresentationTimestampUs", videoDurationMs * 1000)
            
            // 小米 MicroVideo 兼容
            xmp.setProperty(NS_GCAMERA, "GCamera:MicroVideo", "1")
            xmp.setProperty(NS_GCAMERA, "GCamera:MicroVideoVersion", "1")
            xmp.setPropertyLong(NS_GCAMERA, "GCamera:MicroVideoOffset", videoLength)
            xmp.setPropertyLong(NS_GCAMERA, "GCamera:MicroVideoPresentationTimestampUs", videoDurationMs * 1000)
            
            // 华为 LivePhoto 兼容
            xmp.setProperty(NS_HUAWEI, "HwCamera:LivePhoto", "1")
            xmp.setPropertyLong(NS_HUAWEI, "HwCamera:VideoOffset", videoLength)
            xmp.setPropertyLong(NS_HUAWEI, "HwCamera:VideoDuration", videoDurationMs)
            
            // Container XMP（标准容器结构）
            addContainerDirectory(xmp, videoLength, videoMimeType)
            
            serializeXmp(xmp)
        } catch (e: XMPException) {
            throw RuntimeException("Failed to create universal XMP", e)
        }
    }
    
    /**
     * 注册所有需要的命名空间
     */
    private fun registerNamespaces() {
        val registry = XMPMetaFactory.getSchemaRegistry()
        try {
            registry.registerNamespace(NS_GCAMERA, PREFIX_GCAMERA)
            registry.registerNamespace(NS_CONTAINER, PREFIX_CONTAINER)
            registry.registerNamespace(NS_CONTAINER_ITEM, PREFIX_ITEM)
        } catch (e: Exception) {
            // 命名空间可能已注册，忽略错误
        }
    }
    
    /**
     * 添加 Container Directory（Google 标准容器结构）
     */
    private fun addContainerDirectory(xmp: XMPMeta, videoLength: Long, videoMimeType: String) {
        // Item 1: 主 JPEG 图片
        xmp.setArrayItem(NS_CONTAINER, "Container:Directory", 1, createItemXmp(
            mime = "image/jpeg",
            semantic = "Primary",
            length = 0,
            padding = 0
        ))
        
        // Item 2: 嵌入的视频
        xmp.setArrayItem(NS_CONTAINER, "Container:Directory", 2, createItemXmp(
            mime = videoMimeType,
            semantic = "MotionPhoto",
            length = videoLength,
            padding = 0
        ))
    }
    
    /**
     * 创建 Container Item 描述
     */
    private fun createItemXmp(
        mime: String,
        semantic: String,
        length: Long,
        padding: Int
    ): String {
        return buildString {
            append("<rdf:Description")
            append(" Item:Mime=\"$mime\"")
            append(" Item:Semantic=\"$semantic\"")
            if (length > 0) {
                append(" Item:Length=\"$length\"")
            }
            if (padding > 0) {
                append(" Item:Padding=\"$padding\"")
            }
            append("/>")
        }
    }
    
    /**
     * 序列化 XMP 元数据
     */
    private fun serializeXmp(xmp: XMPMeta): String {
        val options = SerializeOptions().apply {
            omitPacketWrapper = true
            useCompactFormat = false  // 使用更宽松的格式，兼容性更好
            indent = "  "
        }
        return XMPMetaFactory.serializeToString(xmp, options)
    }
    
    /**
     * 将 XMP 元数据注入到 JPEG 文件中
     * JPEG 文件结构: SOI(FFD8) -> APP1(XMP) -> ... -> EOI(FFD9)
     */
    fun injectXmpIntoJpeg(inputFile: File, xmpString: String, outputFile: File) {
        val xmpBytes = buildXmpSegment(xmpString)
        
        inputFile.inputStream().use { input ->
            FileOutputStream(outputFile).use { output ->
                // 读取 JPEG SOI marker (0xFF 0xD8)
                val soi = ByteArray(2)
                input.read(soi)
                if (soi[0] != 0xFF.toByte() || soi[1] != 0xD8.toByte()) {
                    throw IllegalArgumentException("Not a valid JPEG file")
                }
                
                // 写入 SOI
                output.write(soi)
                
                // 写入 XMP APP1 segment
                output.write(xmpBytes)
                
                // 复制剩余的 JPEG 数据
                input.copyTo(output)
            }
        }
    }
    
    /**
     * 构建 JPEG APP1 XMP segment
     * 格式: FF E1 [length:2] [XMP header] [XMP data]
     */
    private fun buildXmpSegment(xmpString: String): ByteArray {
        val xmpHeader = "http://ns.adobe.com/xap/1.0/\u0000"
        val xmpData = xmpHeader.toByteArray(Charsets.UTF_8) + xmpString.toByteArray(Charsets.UTF_8)
        
        // APP1 marker (0xFF 0xE1)
        val marker = byteArrayOf(0xFF.toByte(), 0xE1.toByte())
        
        // Length (2 bytes, big-endian, includes length field itself)
        val length = xmpData.size + 2
        if (length > 65535) {
            throw IllegalArgumentException("XMP data too large: $length bytes")
        }
        
        val lengthBytes = byteArrayOf(
            (length shr 8).toByte(),
            (length and 0xFF).toByte()
        )
        
        return marker + lengthBytes + xmpData
    }
}
