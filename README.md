# Motion Studio (v3.5 · Material 3 Expressive)

一个采用 Google 最新 **Material 3 Expressive (MD3E)** 设计语言与物理弹簧动效体系构建的 **Android 实况照片（Motion Photo）全能合成、图影混流、长曝光、无损拆解与修复工具**。

## 🚀 v3.5 升级亮点

### 1. Release 构建配置（R8 压缩）
- 仓库不包含签名密钥。Release 构建启用 R8 代码混淆与资源压缩 (`isMinifyEnabled = true`、`isShrinkResources = true`)，生成未签名 APK。
- 推送至 GitHub Actions 后会生成 Release 和 Debug APK；正式发布前请使用你自己的签名密钥签名 Release APK。

### 2. 融合三大开源项目的 5 项旗舰新功能
- **图影混流合成 (`Photo + Video Dual-Source Muxer`，参考 `PetrVys/MotionPhoto2` & `googleinterns/libmphoto`)**：支持任选一张高像素静态照片（JPG/PNG/HEIC，如修图后照片或 iOS 导出的实况静态图）与任意视频流无损合成为单文件 Android 实况照片。
- **本机相机样张 EXIF 克隆 (`copy-img-meta`，参考 `wszqkzqk/live-photo-conv`)**：支持挑选任意一张由本机原生相机拍摄的普通照片，一键将其 25+ 项相机与光学镜头 EXIF 参数（Make、Model、LensModel、FNumber、FocalLength、ISO 等）完整克隆至实况照片中，彻底解决各品牌系统相册对“私货元数据”的严苛校验。
- **动态长曝光流光封面 (`--long-exposure`，参考 `wszqkzqk/live-photo-conv`)**：支持对裁剪区间内的视频帧进行多帧堆栈合成，提供 **「单帧清晰定格」**、**「丝滑慢门长曝光（均值融合，适合流水/车流虚化）」** 与 **「流光星轨叠加（峰值高光，适合夜景光轨）」** 三种模式。
- **封面风格滤镜与旗舰光学铭牌水印**：内置「原色直出」「鲜明流光」「胶片暖调」「经典黑白」实时滤镜，以及可选的底部光学参数白色铭牌横幅（Leica / Hasselblad / XMAGE / Pixel 风格）。
- **无损拆解器 (`Demuxer`) & 损坏实况一键修复 (`live-photo-repair`) & 批量队列**：
  - 在「实况检验」页支持 **一键无损提取内嵌 MP4 视频**（保存至 `Movies/MotionStudio`）与 **一键提取纯净静态 JPG**（保存至 `Pictures/MotionStudio`）。
  - 支持 **一键修复损坏实况照片**：自动扫描二进制 `ftyp` 偏移量，重新计算尾部视频字节长度并注入全品牌通用 XMP 目录。
  - 支持 **多视频批量队列转实况 (`PickMultipleVisualMedia`)**。
