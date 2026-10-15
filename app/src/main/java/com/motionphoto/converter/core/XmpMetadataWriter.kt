package com.motionphoto.converter.core

import com.adobe.internal.xmp.XMPMetaFactory
import com.adobe.internal.xmp.options.SerializeOptions
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Android Motion Photo 1.0 & 多品牌实况照片 XMP 元数据引擎
 *
 * 修复了旧版本的核心问题：
 * 1. 使用标准 RDF Seq + parseType="Resource" 构建 Container:Directory，避免 setArrayItem 抛出 XMPException 或转义 XML 标签
 * 2. 保留标准 <?xpacket begin="..." id="W5M0MpCehiHzreSzNTczkc9d"?> 包装头尾，确保系统相册与 ExifTool 正常识别
 * 3. 智能解析 JPEG 标记段（SOI / APP0 / APP1 Exif），在保留相机 EXIF 信息的前提下注入 APP1 XMP 数据段
 * 4. 精确写入与静态封面帧对齐的微秒级时间戳 MotionPhotoPresentationTimestampUs
 */
object XmpMetadataWriter {

    const val NS_GCAMERA = "http://ns.google.com/photos/1.0/camera/"
    const val NS_CONTAINER = "http://ns.google.com/photos/1.0/container/"
    const val NS_CONTAINER_ITEM = "http://ns.google.com/photos/1.0/container/item/"
    const val NS_OPCAMERA = "http://ns.oplus.com/photos/1.0/camera/"
    const val NS_HWCAMERA = "http://www.huawei.com/camera/1.0/"

    private const val XMP_HEADER_SIGNATURE = "http://ns.adobe.com/xap/1.0/\u0000"

    /**
     * 生成符合指定厂商规范的完整 XMP 数据包（含 xpacket 头尾）
     */
    fun createXmpPacket(
        format: MotionPhotoBuilder.OutputFormat,
        videoLength: Long,
        presentationTimestampUs: Long,
        videoDurationMs: Long,
        videoMimeType: String = "video/mp4"
    ): String {
        val safeTimestampUs = presentationTimestampUs.coerceAtLeast(0L)
        val safeDurationMs = videoDurationMs.coerceAtLeast(100L)

        val includeMicroVideo = format == MotionPhotoBuilder.OutputFormat.UNIVERSAL ||
            format == MotionPhotoBuilder.OutputFormat.XIAOMI_ENHANCED
        val includeOppo = format == MotionPhotoBuilder.OutputFormat.UNIVERSAL ||
            format == MotionPhotoBuilder.OutputFormat.OPPO_OLIVE
        val includeHuawei = format == MotionPhotoBuilder.OutputFormat.UNIVERSAL ||
            format == MotionPhotoBuilder.OutputFormat.HUAWEI_COMPATIBLE

        val rawRdfXml = buildRdfXml(
            videoLength = videoLength,
            presentationTimestampUs = safeTimestampUs,
            videoDurationMs = safeDurationMs,
            videoMimeType = videoMimeType,
            includeMicroVideo = includeMicroVideo,
            includeOppo = includeOppo,
            includeHuawei = includeHuawei
        )

        return try {
            registerNamespaces()
            val xmpMeta = XMPMetaFactory.parseFromString(rawRdfXml)
            val options = SerializeOptions().apply {
                omitPacketWrapper = false
                useCompactFormat = true
                indent = "  "
                padding = 64
            }
            XMPMetaFactory.serializeToString(xmpMeta, options)
        } catch (_: Exception) {
            wrapWithXPacket(rawRdfXml)
        }
    }

    /**
     * 兼容旧接口调用
     */
    fun createUniversalXmp(
        videoLength: Long,
        videoDurationMs: Long,
        videoMimeType: String = "video/mp4",
        presentationTimestampUs: Long = 0L
    ): String = createXmpPacket(
        format = MotionPhotoBuilder.OutputFormat.UNIVERSAL,
        videoLength = videoLength,
        presentationTimestampUs = presentationTimestampUs,
        videoDurationMs = videoDurationMs,
        videoMimeType = videoMimeType
    )

    private fun registerNamespaces() {
        try {
            val registry = XMPMetaFactory.getSchemaRegistry()
            registry.registerNamespace(NS_GCAMERA, "GCamera")
            registry.registerNamespace(NS_CONTAINER, "Container")
            registry.registerNamespace(NS_CONTAINER_ITEM, "Item")
            registry.registerNamespace(NS_OPCAMERA, "OpCamera")
            registry.registerNamespace(NS_HWCAMERA, "HwCamera")
        } catch (_: Exception) {
            // 忽略重复注册异常
        }
    }

    private fun buildRdfXml(
        videoLength: Long,
        presentationTimestampUs: Long,
        videoDurationMs: Long,
        videoMimeType: String,
        includeMicroVideo: Boolean,
        includeOppo: Boolean,
        includeHuawei: Boolean
    ): String = buildString {
        append("<x:xmpmeta xmlns:x=\"adobe:ns:meta/\" x:xmptk=\"Adobe XMP Core 6.1.11\">\n")
        append("  <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n")
        append("    <rdf:Description rdf:about=\"\"\n")
        append("        xmlns:GCamera=\"$NS_GCAMERA\"\n")
        append("        xmlns:Camera=\"$NS_GCAMERA\"\n")
        append("        xmlns:Container=\"$NS_CONTAINER\"\n")
        append("        xmlns:Item=\"$NS_CONTAINER_ITEM\"")
        if (includeOppo) {
            append("\n        xmlns:OpCamera=\"$NS_OPCAMERA\"")
        }
        if (includeHuawei) {
            append("\n        xmlns:HwCamera=\"$NS_HWCAMERA\"")
        }
        // Google Motion Photo 1.0 标准属性
        append("\n        GCamera:MotionPhoto=\"1\"")
        append("\n        GCamera:MotionPhotoVersion=\"1\"")
        append("\n        GCamera:MotionPhotoPresentationTimestampUs=\"$presentationTimestampUs\"")

        // 小米 / 旧版 Google 相册 MicroVideo 兼容属性
        if (includeMicroVideo) {
            append("\n        GCamera:MicroVideo=\"1\"")
            append("\n        GCamera:MicroVideoVersion=\"1\"")
            append("\n        GCamera:MicroVideoOffset=\"$videoLength\"")
            append("\n        GCamera:MicroVideoPresentationTimestampUs=\"$presentationTimestampUs\"")
        }

        // OPPO / OnePlus / Realme O-Live Photo 扩展属性
        if (includeOppo) {
            append("\n        OpCamera:MotionPhotoOwner=\"oplus\"")
            append("\n        OpCamera:OLivePhotoVersion=\"2\"")
            append("\n        OpCamera:MotionPhotoPrimaryPresentationTimestampUs=\"$presentationTimestampUs\"")
            append("\n        OpCamera:VideoLength=\"$videoLength\"")
        }

        // 华为 / 荣耀实况照片兼容属性
        if (includeHuawei) {
            append("\n        HwCamera:LivePhoto=\"1\"")
            append("\n        HwCamera:VideoOffset=\"$videoLength\"")
            append("\n        HwCamera:VideoDuration=\"$videoDurationMs\"")
        }
        append(">\n")

        // Google 标准 Container:Directory 结构
        append("      <Container:Directory>\n")
        append("        <rdf:Seq>\n")
        append("          <rdf:li rdf:parseType=\"Resource\">\n")
        append("            <Container:Item\n")
        append("                Item:Mime=\"image/jpeg\"\n")
        append("                Item:Semantic=\"Primary\"\n")
        append("                Item:Length=\"0\"\n")
        append("                Item:Padding=\"0\"/>\n")
        append("          </rdf:li>\n")
        append("          <rdf:li rdf:parseType=\"Resource\">\n")
        append("            <Container:Item\n")
        append("                Item:Mime=\"$videoMimeType\"\n")
        append("                Item:Semantic=\"MotionPhoto\"\n")
        append("                Item:Length=\"$videoLength\"\n")
        append("                Item:Padding=\"0\"/>\n")
        append("          </rdf:li>\n")
        append("        </rdf:Seq>\n")
        append("      </Container:Directory>\n")
        append("    </rdf:Description>\n")
        append("  </rdf:RDF>\n")
        append("</x:xmpmeta>")
    }

    private fun wrapWithXPacket(rdfXml: String): String {
        return "<?xpacket begin=\"\uFEFF\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>\n" +
            rdfXml +
            "\n<?xpacket end=\"w\"?>"
    }

    /**
     * 将 XMP 元数据无损注入到 JPEG 文件中，保留原有的 APP0 (JFIF) 与 APP1 (Exif) 段，
     * 并自动移除已存在的旧 XMP APP1 段，避免重复冲突。
     */
    fun injectXmpIntoJpeg(inputFile: File, xmpString: String, outputFile: File) {
        val xmpApp1Segment = buildXmpApp1Segment(xmpString)
        val inputBytes = FileInputStream(inputFile).use { it.readBytes() }

        if (inputBytes.size < 4 ||
            inputBytes[0] != 0xFF.toByte() ||
            inputBytes[1] != 0xD8.toByte()
        ) {
            throw IllegalArgumentException("输入文件不是有效的 JPEG 图像")
        }

        val outBuffer = ByteArrayOutputStream(inputBytes.size + xmpApp1Segment.size)
        // 1. 写入 JPEG SOI (0xFFD8)
        outBuffer.write(inputBytes, 0, 2)

        var pos = 2
        var xmpWritten = false
        val xmpHeaderBytes = XMP_HEADER_SIGNATURE.toByteArray(Charsets.UTF_8)

        // 2. 遍历 JPEG 头部标记段，保留 JFIF (FFE0) 和 Exif (FFE1)，并在它们之后插入 XMP (FFE1)
        while (pos + 4 <= inputBytes.size) {
            if (inputBytes[pos] != 0xFF.toByte()) {
                break
            }
            val marker = inputBytes[pos + 1].toInt() and 0xFF

            // 遇到 SOS (0xDA) 或 EOI (0xD9)，说明头部元数据段结束
            if (marker == 0xDA || marker == 0xD9) {
                if (!xmpWritten) {
                    outBuffer.write(xmpApp1Segment)
                    xmpWritten = true
                }
                break
            }

            // 独立无长度标记 (RSTn, SOI, TEM)
            if (marker == 0xD8 || marker == 0x01 || (marker in 0xD0..0xD7)) {
                outBuffer.write(inputBytes, pos, 2)
                pos += 2
                continue
            }

            val segLength = ((inputBytes[pos + 2].toInt() and 0xFF) shl 8) or
                (inputBytes[pos + 3].toInt() and 0xFF)
            if (segLength < 2 || pos + 2 + segLength > inputBytes.size) {
                break
            }

            val totalSegBytes = 2 + segLength

            // 如果是 APP0 (0xE0) 或非 XMP 的 APP1 (0xE1 Exif)，先保留写入
            if (marker == 0xE0 || marker == 0xE1) {
                val isOldXmp = marker == 0xE1 && isXmpPayload(inputBytes, pos + 4, segLength - 2, xmpHeaderBytes)
                if (!isOldXmp) {
                    outBuffer.write(inputBytes, pos, totalSegBytes)
                }
                pos += totalSegBytes
            } else {
                // 在进入 DQT/SOF 等图像核心段之前写入 XMP APP1 段
                if (!xmpWritten) {
                    outBuffer.write(xmpApp1Segment)
                    xmpWritten = true
                }
                outBuffer.write(inputBytes, pos, totalSegBytes)
                pos += totalSegBytes
            }
        }

        if (!xmpWritten) {
            outBuffer.write(xmpApp1Segment)
        }

        // 3. 写入剩余的 JPEG 扫描流数据
        if (pos < inputBytes.size) {
            outBuffer.write(inputBytes, pos, inputBytes.size - pos)
        }

        FileOutputStream(outputFile).use { out ->
            outBuffer.writeTo(out)
        }
    }

    private fun isXmpPayload(
        bytes: ByteArray,
        payloadStart: Int,
        payloadLength: Int,
        xmpHeader: ByteArray
    ): Boolean {
        if (payloadLength < xmpHeader.size) return false
        for (i in xmpHeader.indices) {
            if (bytes[payloadStart + i] != xmpHeader[i]) return false
        }
        return true
    }

    /**
     * 构建标准 JPEG APP1 XMP 段：0xFF 0xE1 + [2字节大端长度] + "http://ns.adobe.com/xap/1.0/\0" + XMP UTF-8 内容
     */
    private fun buildXmpApp1Segment(xmpString: String): ByteArray {
        val headerBytes = XMP_HEADER_SIGNATURE.toByteArray(Charsets.UTF_8)
        val payloadBytes = xmpString.toByteArray(Charsets.UTF_8)
        val totalPayloadSize = headerBytes.size + payloadBytes.size
        val segmentLength = totalPayloadSize + 2

        if (segmentLength > 65535) {
            throw IllegalArgumentException("XMP 数据段超出 JPEG APP1 64KB 限制 ($segmentLength bytes)")
        }

        val result = ByteArray(2 + segmentLength)
        result[0] = 0xFF.toByte()
        result[1] = 0xE1.toByte()
        result[2] = ((segmentLength shr 8) and 0xFF).toByte()
        result[3] = (segmentLength and 0xFF).toByte()
        System.arraycopy(headerBytes, 0, result, 4, headerBytes.size)
        System.arraycopy(payloadBytes, 0, result, 4 + headerBytes.size, payloadBytes.size)
        return result
    }
}
