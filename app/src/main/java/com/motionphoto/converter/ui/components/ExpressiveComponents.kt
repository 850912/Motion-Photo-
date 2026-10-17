package com.motionphoto.converter.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.motionphoto.converter.ui.theme.ExpressiveMotion
import com.motionphoto.converter.ui.theme.expressiveBounceClick
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 1. 120Hz 高性能 MD3 Expressive 多叶徽章
 * 优化点：
 * - 复用 remember { Path() }，避免每帧分配 Path 对象引起 GC 卡顿
 * - 静态展示时零无限重绘开销，仅在 isAnimating = true（如合成中）启用硬件层旋转
 */
@Composable
fun ExpressiveMorphingBadge(
    modifier: Modifier = Modifier,
    lobes: Float = 8f,
    waveAmplitude: Float = 0.10f,
    isAnimating: Boolean = false,
    primaryColor: Color = MaterialTheme.colorScheme.primaryContainer,
    secondaryColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    outlineColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
    content: @Composable () -> Unit
) {
    val animatedLobes by animateFloatAsState(
        targetValue = lobes,
        animationSpec = ExpressiveMotion.defaultSpatial(),
        label = "lobes"
    )

    val rotationDeg = if (isAnimating) {
        val infiniteTransition = rememberInfiniteTransition(label = "badge_spin")
        val rot by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rot"
        )
        rot
    } else {
        0f
    }

    val outerPath = remember { Path() }
    val mainPath = remember { Path() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotationDeg
                }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseRadius = min(size.width, size.height) * 0.41f
            val steps = 72

            outerPath.rewind()
            for (i in 0..steps) {
                val theta = (i.toFloat() / steps) * (2f * PI.toFloat())
                val r = baseRadius * 1.11f * (1f + (waveAmplitude * 0.75f) * cos(animatedLobes * theta))
                val x = cx + r * cos(theta)
                val y = cy + r * sin(theta)
                if (i == 0) outerPath.moveTo(x, y) else outerPath.lineTo(x, y)
            }
            outerPath.close()
            drawPath(
                path = outerPath,
                color = secondaryColor.copy(alpha = 0.42f)
            )

            mainPath.rewind()
            for (i in 0..steps) {
                val theta = (i.toFloat() / steps) * (2f * PI.toFloat())
                val r = baseRadius * (1f + waveAmplitude * cos(animatedLobes * theta))
                val x = cx + r * cos(theta)
                val y = cy + r * sin(theta)
                if (i == 0) mainPath.moveTo(x, y) else mainPath.lineTo(x, y)
            }
            mainPath.close()

            drawPath(
                path = mainPath,
                brush = Brush.linearGradient(
                    colors = listOf(primaryColor, secondaryColor),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
            )
            drawPath(
                path = mainPath,
                color = outlineColor,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        content()
    }
}

/**
 * 2. 零对象分配的 MD3E 波浪进度条 (复用 Path)
 */
@Composable
fun ExpressiveWavyLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeWidth: Dp = 6.dp,
    waveAmplitude: Dp = 3.5.dp,
    waveLength: Dp = 28.dp
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = ExpressiveMotion.defaultSpatial(),
        label = "wavy_linear_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "wave_shift")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val wavePath = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(waveAmplitude * 2 + strokeWidth * 2)
    ) {
        val strokePx = strokeWidth.toPx()
        val ampPx = waveAmplitude.toPx()
        val waveLenPx = waveLength.toPx().coerceAtLeast(12f)
        val centerY = size.height / 2f
        val startX = strokePx
        val endX = (size.width - strokePx).coerceAtLeast(startX)
        val totalWidth = endX - startX
        val activeEndX = startX + totalWidth * animatedProgress

        if (animatedProgress > 0.01f) {
            wavePath.rewind()
            var x = startX
            val step = 4f
            var first = true
            while (x <= activeEndX) {
                val angle = ((x - startX) / waveLenPx) * (2f * PI.toFloat()) - wavePhase
                val y = centerY + ampPx * sin(angle)
                if (first) {
                    wavePath.moveTo(x, y)
                    first = false
                } else {
                    wavePath.lineTo(x, y)
                }
                x += step
            }
            drawPath(
                path = wavePath,
                color = activeColor,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        val gapPx = strokePx * 2.2f
        val trackStartX = (activeEndX + gapPx).coerceAtMost(endX)
        if (trackStartX < endX - 2f) {
            drawLine(
                color = trackColor,
                start = Offset(trackStartX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * 3. 零分配环形波浪光环
 */
@Composable
fun ExpressiveWavyCircularHalo(
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
    strokeWidth: Dp = 7.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = ExpressiveMotion.defaultSpatial(),
        label = "circular_wavy_progress"
    )
    val bgPath = remember { Path() }
    val fgPath = remember { Path() }

    Canvas(modifier = modifier) {
        val strokePx = strokeWidth.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadius = (min(size.width, size.height) / 2f) - strokePx * 1.8f
        val waveAmp = strokePx * 0.45f
        val frequency = 12f
        val steps = 96

        bgPath.rewind()
        for (i in 0..steps) {
            val ratio = i.toFloat() / steps
            val theta = -PI.toFloat() / 2f + ratio * 2f * PI.toFloat()
            val r = baseRadius + (waveAmp * 0.4f) * sin(frequency * theta)
            val x = cx + r * cos(theta)
            val y = cy + r * sin(theta)
            if (i == 0) bgPath.moveTo(x, y) else bgPath.lineTo(x, y)
        }
        bgPath.close()
        drawPath(
            path = bgPath,
            color = trackColor,
            style = Stroke(width = strokePx * 0.75f, cap = StrokeCap.Round)
        )

        val activeSteps = (steps * animatedProgress).toInt().coerceAtLeast(2)
        fgPath.rewind()
        for (i in 0..activeSteps) {
            val ratio = i.toFloat() / steps
            val theta = -PI.toFloat() / 2f + ratio * 2f * PI.toFloat()
            val r = baseRadius + waveAmp * sin(frequency * theta)
            val x = cx + r * cos(theta)
            val y = cy + r * sin(theta)
            if (i == 0) fgPath.moveTo(x, y) else fgPath.lineTo(x, y)
        }
        drawPath(
            path = fgPath,
            color = activeColor,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
    }
}

/**
 * 4. 高响应连通形变按钮组 (Connected Button Group)
 */
@Composable
fun <T> ExpressiveConnectedButtonGroup(
    items: List<T>,
    selectedItem: T,
    onSelect: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = item == selectedItem
            val isFirst = index == 0
            val isLast = index == items.lastIndex

            val startRadius by animateDpAsState(
                targetValue = if (isSelected || isFirst) 22.dp else 8.dp,
                animationSpec = ExpressiveMotion.fastSpatial(),
                label = "sr"
            )
            val endRadius by animateDpAsState(
                targetValue = if (isSelected || isLast) 22.dp else 8.dp,
                animationSpec = ExpressiveMotion.fastSpatial(),
                label = "er"
            )

            val containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            }
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .weight(if (isSelected) 1.18f else 1f)
                    .height(44.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = startRadius,
                            bottomStart = startRadius,
                            topEnd = endRadius,
                            bottomEnd = endRadius
                        )
                    )
                    .background(containerColor)
                    .expressiveBounceClick(pressedScale = 0.95f) {
                        onSelect(item)
                    }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelProvider(item),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 5. 底部悬浮胶囊导航栏 (新增 ABOUT "关于作者" 页面)
 */
enum class AppTab(
    val title: String,
    val icon: ImageVector
) {
    STUDIO("创作", Icons.Default.AutoAwesomeMotion),
    INSPECTOR("检验", Icons.Default.Verified),
    GALLERY("作品", Icons.Default.Collections),
    ABOUT("关于", Icons.Default.Person)
}

@Composable
fun ExpressiveFloatingToolbar(
    currentTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    onQuickPickVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppTab.values().forEach { tab ->
                    val selected = tab == currentTab
                    val pillColor by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        },
                        animationSpec = ExpressiveMotion.fastEffects(),
                        label = "tab_pill_bg"
                    )
                    val contentColor = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Row(
                        modifier = Modifier
                            .weight(if (selected) 1.35f else 1f)
                            .height(50.dp)
                            .clip(CircleShape)
                            .background(pillColor)
                            .expressiveBounceClick(pressedScale = 0.94f) {
                                onSelectTab(tab)
                            }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        if (selected) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = contentColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .size(64.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(22.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                )
                .expressiveBounceClick(pressedScale = 0.92f) {
                    onQuickPickVideo()
                },
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "选择视频",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

/**
 * 6. 按需加载 (Lazy ExoPlayer) 的实况照片交互预览卡
 * 默认仅显示静态图片，仅当点击播放实况时才创建 ExoPlayer，避免进入页面时主线程解码卡顿
 */
@Composable
fun LiveMotionPhotoPlayerCard(
    coverImageModel: Any?,
    videoFileOrUri: Any?,
    resolutionLabel: String,
    durationMs: Long,
    modifier: Modifier = Modifier,
    autoPlayInitially: Boolean = false
) {
    val context = LocalContext.current
    var isPlayingLive by remember(videoFileOrUri) { mutableStateOf(autoPlayInitially) }

    val exoPlayer = remember(videoFileOrUri, isPlayingLive) {
        if (!isPlayingLive || videoFileOrUri == null) {
            null
        } else {
            ExoPlayer.Builder(context).build().apply {
                val uri = when (videoFileOrUri) {
                    is Uri -> videoFileOrUri
                    is File -> Uri.fromFile(videoFileOrUri)
                    is String -> Uri.parse(videoFileOrUri)
                    else -> null
                }
                if (uri != null) {
                    setMediaItem(MediaItem.fromUri(uri))
                    repeatMode = Player.REPEAT_MODE_ONE
                    prepare()
                    playWhenReady = true
                }
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }

    val cornerRadius by animateDpAsState(
        targetValue = if (isPlayingLive) 22.dp else 30.dp,
        animationSpec = ExpressiveMotion.defaultSpatial(),
        label = "live_photo_corner"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isPlayingLive) 2.dp else 1.dp,
                color = if (isPlayingLive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(cornerRadius)
            )
            .expressiveBounceClick(pressedScale = 0.98f) {
                if (videoFileOrUri != null) {
                    isPlayingLive = !isPlayingLive
                }
            }
    ) {
        AsyncImage(
            model = coverImageModel,
            contentDescription = "Motion Photo Cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = isPlayingLive && exoPlayer != null,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180)),
            modifier = Modifier.fillMaxSize()
        ) {
            if (exoPlayer != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { view ->
                        view.player = exoPlayer
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        val badgeBg = if (isPlayingLive) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Black.copy(alpha = 0.62f)
        }
        val badgeFg = if (isPlayingLive) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            Color.White
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(CircleShape)
                .background(badgeBg)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MotionPhotosOn,
                contentDescription = null,
                tint = badgeFg,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isPlayingLive) "实况播放中 · LIVE" else "实况 · 点击画面播放",
                style = MaterialTheme.typography.labelMedium,
                color = badgeFg
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "$resolutionLabel · ${String.format("%.1fs", durationMs / 1000f)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlayingLive) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
