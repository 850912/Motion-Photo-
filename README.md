# Motion Studio (v2.0 · Material 3 Expressive)

一个采用 Google 最新 **Material 3 Expressive (MD3E)** 设计语言与物理弹簧动效体系重构的 **Android 实况照片（Motion Photo）无损合成与检验工具**。

## 核心重构亮点

### 1. 彻底修复实况照片无法正常生成与相册无法识别的问题
- **标准 RDF `Container:Directory` 序列化**：修复了旧版本直接将 XML 字符串传入 `setArrayItem` 导致抛出 `XMPException` 或标签被转义的致命问题，使用标准 `rdf:Seq` + `rdf:parseType="Resource"` 结构并通过 `XMPMetaFactory` 校验序列化，完整保留 `<?xpacket ...?>` 包装头尾。
- **保留相机 EXIF + 无损注入 APP1 XMP**：智能解析 JPEG 头部标记段（`SOI 0xFFD8`、`APP0 JFIF`、`APP1 Exif`），自动写入相机 `Make`、`Model`、`DateTimeOriginal`、`Orientation` 与图像尺寸，使小米澎湃 OS、OPPO ColorOS、华为 HarmonyOS 及 Google 相册均将其识别为原生相机拍摄的实况照片。
- **精确封面微秒时间戳与竖屏旋转矩阵保护**：支持在时间轴任意挑选静态封面帧，自动计算裁剪区间内的相对微秒时间戳写入 `GCamera:MotionPhotoPresentationTimestampUs`；同时在 `MediaMuxer` 中写入视频旋转角度 `setOrientationHint` 与单调递增 PTS 保护，彻底解决竖屏视频拉伸变形与封装崩溃。
- **直存系统相册 (`DCIM/Camera/*MP.jpg`)**：遵循 Android Motion Photo 1.0 官方文件命名正则规范（如 `MVIMG_yyyyMMdd_HHmmss_SSS.MP.jpg`、`PXL_yyyyMMdd_HHmmss_SSS.MP.jpg`），通过 `MediaStore` 自动写入系统相册，无需手动去私有目录寻找文件。

### 2. Google Material 3 Expressive (MD3E) 视觉与物理动效重塑
- **MD3E 物理弹簧动效系统 (`ExpressiveMotion`)**：全面采用空间物理弹簧 (`Spatial Springs`, 低阻尼回弹超调) 与视觉效果弹簧 (`Effects Springs`, 临界阻尼平滑过渡)，所有卡片、胶囊与按钮均具备 `.expressiveBounceClick` 弹性按压形变反馈。
- **标志性几何形变与波浪进度组件 (`ExpressiveComponents`)**：
  - **多叶曲奇/花瓣形变徽章 (`ExpressiveMorphingBadge`)**：基于极坐标谐波方程绘制的呼吸旋转多边形徽章，随工作台状态平滑改变花瓣阶数。
  - **Android 16 正弦波浪进度条 (`ExpressiveWavyLinearProgress` & `ExpressiveWavyCircularHalo`)**：动态相位推进的波浪进度条与环形波浪光环。
  - **连通圆角形变按钮组 (`ExpressiveConnectedButtonGroup`)**：选中项自动膨胀为饱满胶囊圆角 (`24.dp`)，相邻项保持紧凑内侧圆角 (`8.dp`)。
  - **悬浮胶囊导航栏 (`ExpressiveFloatingToolbar`)**：底部高阴影胶囊工具栏搭配超椭圆快速选片 FAB。
- **交互式实况预览与 XMP 二进制检验器 (`MotionPhotoInspector`)**：
  - 内置 **「实况 · LIVE」** 交互式预览卡片，点击即可从高清静态封面平滑缩放过渡至 Media3 `ExoPlayer` 视频回放。
  - 内置 **「实况检验」** 页面，支持任选相册照片解析 APP1 XMP 报文、扫描尾部 MP4 `ftyp` 偏移量并现场回放内嵌视频流。