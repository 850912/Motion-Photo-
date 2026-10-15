package com.motionphoto.converter.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.motionphoto.converter.core.MotionPhotoBuilder
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            // 权限未授予的处理
        }
    }
    
    // 视频选择器
    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.selectVideo(it)
        }
    }
    
    LaunchedEffect(Unit) {
        // 请求必要权限
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Motion Photo Converter",
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                },
                label = "state_transition"
            ) { state ->
                when (state) {
                    is MainViewModel.UiState.Idle -> {
                        IdleScreen(
                            onSelectVideo = {
                                videoPickerLauncher.launch("video/*")
                            }
                        )
                    }
                    
                    is MainViewModel.UiState.VideoSelected -> {
                        VideoPreviewScreen(
                            videoUri = state.uri,
                            onConvert = { frameTimeUs, format ->
                                viewModel.startConversion(state.uri, frameTimeUs, format)
                            },
                            onCancel = {
                                viewModel.reset()
                            }
                        )
                    }
                    
                    is MainViewModel.UiState.Converting -> {
                        ConvertingScreen(
                            progress = state.progress,
                            stage = state.stage
                        )
                    }
                    
                    is MainViewModel.UiState.Success -> {
                        SuccessScreen(
                            outputFile = state.outputFile,
                            onShare = { file ->
                                shareFile(context, file)
                            },
                            onConvertAnother = {
                                viewModel.reset()
                                videoPickerLauncher.launch("video/*")
                            }
                        )
                    }
                    
                    is MainViewModel.UiState.Error -> {
                        ErrorScreen(
                            message = state.message,
                            onRetry = {
                                viewModel.reset()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleScreen(onSelectVideo: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 动画图标
        val infiniteTransition = rememberInfiniteTransition(label = "icon_pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "视频转 Motion Photo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "保持原始画质和帧率\n无损转换为 Android Motion Photo",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        FilledTonalButton(
            onClick = onSelectVideo,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "选择视频",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun VideoPreviewScreen(
    videoUri: Uri,
    onConvert: (Long, MotionPhotoBuilder.OutputFormat) -> Unit,
    onCancel: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf(MotionPhotoBuilder.OutputFormat.UNIVERSAL) }
    var showFormatSheet by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "视频预览",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 视频缩略图
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = RoundedCornerShape(16.dp)
        ) {
            AsyncImage(
                model = videoUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 格式选择
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            onClick = { showFormatSheet = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "输出格式",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (selectedFormat) {
                            MotionPhotoBuilder.OutputFormat.UNIVERSAL -> "通用格式（推荐）"
                            MotionPhotoBuilder.OutputFormat.GOOGLE_STANDARD -> "Google 标准"
                            MotionPhotoBuilder.OutputFormat.XIAOMI_ENHANCED -> "小米增强"
                            MotionPhotoBuilder.OutputFormat.HUAWEI_COMPATIBLE -> "华为兼容"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "将从视频开始提取关键帧作为封面，保持原始画质",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("取消")
            }
            
            Button(
                onClick = { onConvert(0L, selectedFormat) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("开始转换")
            }
        }
    }
    
    // 格式选择底部表单
    if (showFormatSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFormatSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "选择输出格式",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                FormatOption(
                    title = "通用格式",
                    subtitle = "兼容小米、OPPO、华为、Google 相册",
                    description = "包含多种元数据标识，兼容性最好（推荐）",
                    isSelected = selectedFormat == MotionPhotoBuilder.OutputFormat.UNIVERSAL,
                    onClick = {
                        selectedFormat = MotionPhotoBuilder.OutputFormat.UNIVERSAL
                        showFormatSheet = false
                    }
                )
                
                FormatOption(
                    title = "Google 标准",
                    subtitle = "兼容 OPPO、部分小米、Google 相册",
                    description = "标准 Motion Photo 格式",
                    isSelected = selectedFormat == MotionPhotoBuilder.OutputFormat.GOOGLE_STANDARD,
                    onClick = {
                        selectedFormat = MotionPhotoBuilder.OutputFormat.GOOGLE_STANDARD
                        showFormatSheet = false
                    }
                )
                
                FormatOption(
                    title = "小米增强",
                    subtitle = "优化小米设备兼容性",
                    description = "包含 MicroVideo 和 MotionPhoto 标识",
                    isSelected = selectedFormat == MotionPhotoBuilder.OutputFormat.XIAOMI_ENHANCED,
                    onClick = {
                        selectedFormat = MotionPhotoBuilder.OutputFormat.XIAOMI_ENHANCED
                        showFormatSheet = false
                    }
                )
                
                FormatOption(
                    title = "华为兼容",
                    subtitle = "优化华为/荣耀设备兼容性",
                    description = "包含华为 LivePhoto 元数据",
                    isSelected = selectedFormat == MotionPhotoBuilder.OutputFormat.HUAWEI_COMPATIBLE,
                    onClick = {
                        selectedFormat = MotionPhotoBuilder.OutputFormat.HUAWEI_COMPATIBLE
                        showFormatSheet = false
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun FormatOption(
    title: String,
    subtitle: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ConvertingScreen(
    progress: Float,
    stage: MotionPhotoBuilder.ConversionProgress.Stage
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 旋转动画
        val infiniteTransition = rememberInfiniteTransition(label = "rotation")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
        
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.size(80.dp),
            strokeWidth = 6.dp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = when (stage) {
                MotionPhotoBuilder.ConversionProgress.Stage.EXTRACTING_FRAME -> "提取关键帧..."
                MotionPhotoBuilder.ConversionProgress.Stage.EXTRACTING_VIDEO -> "提取视频流..."
                MotionPhotoBuilder.ConversionProgress.Stage.WRITING_METADATA -> "写入元数据..."
                MotionPhotoBuilder.ConversionProgress.Stage.COMBINING -> "合并文件..."
                MotionPhotoBuilder.ConversionProgress.Stage.COMPLETE -> "完成！"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SuccessScreen(
    outputFile: File,
    onShare: (File) -> Unit,
    onConvertAnother: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 成功动画
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "转换成功！",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Motion Photo 已保存",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = outputFile.absolutePath,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        FilledTonalButton(
            onClick = { onShare(outputFile) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("分享")
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedButton(
            onClick = onConvertAnother,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("继续转换")
        }
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "转换失败",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("重试")
        }
    }
}

private fun shareFile(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    
    context.startActivity(Intent.createChooser(intent, "分享 Motion Photo"))
}
