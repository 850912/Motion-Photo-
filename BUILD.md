# Motion Photo Converter - 构建说明

## 📦 项目已完成

这个视频转 Motion Photo 应用已经完全开发完成，包含：

✅ **核心功能**
- 无损视频转换（MediaExtractor + MediaMuxer）
- 多格式 XMP 元数据支持（Google、小米、华为）
- Material 3 现代界面设计
- 完整的权限管理和文件分享

✅ **国产手机兼容性优化**
- 通用格式（包含所有厂商元数据）
- Google 标准格式（OPPO、小米、Google Photos）
- 小米增强格式（MicroVideo + MotionPhoto）
- 华为兼容格式（LivePhoto 元数据）

## 🔨 构建方法

### 方法 1：Android Studio（推荐）

1. **导出项目**
   - 将整个 workspace 文件夹复制到电脑
   - 或使用 git 推送到 GitHub 后在电脑上克隆

2. **打开项目**
   ```bash
   # 在电脑上
   git clone <你的仓库地址>
   # 或直接用 Android Studio 打开文件夹
   ```

3. **同步和构建**
   - Android Studio 会自动下载 Gradle、Android SDK 和依赖
   - Build → Build Bundle(s) / APK(s) → Build APK(s)
   - 或使用终端：`./gradlew assembleDebug`

4. **安装测试**
   - 连接 Android 设备或启动模拟器
   - Run → Run 'app'
   - 或使用 adb：`adb install app/build/outputs/apk/debug/app-debug.apk`

### 方法 2：GitHub Actions 自动构建

项目包含 `.github/workflows/android-build.yml`，可以自动构建：

1. **推送到 GitHub**
   ```bash
   git init
   git add .
   git commit -m "Initial commit: Motion Photo Converter"
   git remote add origin <你的仓库地址>
   git push -u origin main
   ```

2. **触发构建**
   - 推送后 GitHub Actions 会自动构建
   - 在 Actions 标签页查看进度
   - 构建完成后下载 APK artifact

3. **下载 APK**
   - 进入 Actions → 选择构建任务
   - 下载 `app-debug` artifact
   - 解压后得到 APK 文件

### 方法 3：命令行构建（需要 Android SDK）

如果你的电脑已安装 Android SDK：

```bash
# 确保 ANDROID_HOME 已设置
export ANDROID_HOME=/path/to/android-sdk

# 构建
./gradlew assembleDebug

# 输出位置
# app/build/outputs/apk/debug/app-debug.apk
```

## 📱 安装和使用

### 安装

```bash
# 通过 adb 安装
adb install app-debug.apk

# 或直接在手机上打开 APK 文件安装
```

### 使用说明

1. **授予权限**：首次打开会请求访问照片和视频的权限
2. **选择视频**：点击"选择视频"按钮，从相册选择要转换的视频
3. **选择格式**：
   - **通用格式**（推荐）：兼容所有品牌
   - **Google 标准**：适合 OPPO、Google Pixel
   - **小米增强**：优化小米设备
   - **华为兼容**：优化华为/荣耀设备
4. **开始转换**：点击"开始转换"，等待处理完成
5. **保存分享**：转换完成后可以分享或在相册中查看

### 输出位置

```
/Android/data/com.motionphoto.converter/files/MotionPhotos/
motion_photo_<时间戳>.jpg
```

## 🧪 测试兼容性

### 测试设备建议

| 品牌 | 推荐格式 | 测试要点 |
|------|---------|---------|
| **小米** | 通用格式 / 小米增强 | 在相册中长按查看动态效果 |
| **OPPO** | 通用格式 / Google 标准 | 相册应自动识别为动态照片 |
| **华为/荣耀** | 通用格式 / 华为兼容 | 发送到华为设备查看兼容性 |
| **vivo** | 通用格式 | 可能需要第三方相册应用 |
| **Google Pixel** | 所有格式 | Google Photos 完全支持 |

### 验证方法

1. **静态显示**：所有设备都应能正常显示封面图片
2. **动态播放**：在支持的相册中长按或点击应能播放视频
3. **元数据检查**：使用 ExifTool 查看 XMP 元数据是否正确写入

```bash
# 使用 ExifTool 检查
exiftool motion_photo_*.jpg | grep -i "motion\|micro\|container"
```

## 🐛 问题排查

### 相册不识别动态效果

1. 确认使用了正确的格式（推荐通用格式）
2. 检查相册应用是否支持 Motion Photo
3. 尝试在 Google Photos 中打开

### 转换失败

1. 检查视频格式是否为常见格式（MP4、MOV）
2. 确认已授予存储权限
3. 查看 logcat 日志获取详细错误信息

### 文件过大

- Motion Photo 会保留原始视频质量，文件大小等于图片 + 视频
- 如需压缩，建议先压缩源视频再转换

## 📝 技术细节

### 依赖版本

- Android SDK: 26-34
- Kotlin: 1.9.20
- Compose BOM: 2024.02.00
- Material 3: Latest
- Media3: 1.2.1
- XMP Core: 6.1.11
- Coil: 2.5.0

### 最低要求

- Android 8.0 (API 26) 及以上
- 存储空间：根据视频大小而定
- 权限：读取视频、读取/写入图片

## 🔗 相关资源

- [Android Motion Photo Format 1.0](https://developer.android.com/media/platform/motion-photo-format)
- [Material Design 3](https://m3.material.io/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)

---

**注意**：由于 AndroidHarness 环境限制，无法直接构建 APK。请使用上述方法之一进行构建和测试。
