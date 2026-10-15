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
import androidx.compose.foundation.layout.Column
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

@Composable
fun ExpressiveMorphingBadge(
    modifier: Modifier = Modifier,
    lobes: Float = 8f,
    waveAmplitude: Float = 0.11f,
    primaryColor: Color = MaterialTheme.colorScheme.primaryContainer,
    secondaryColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    outlineColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "md3e_morph")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val animatedLobes by animateFloatAsState(
        targetValue = lobes,
        animationSpec = ExpressiveMotion.slowSpatial(),
        label = "lobes"
    )
    val animatedAmp by animateFloatAsState(
        targetValue = waveAmplitude,
        animationSpec = ExpressiveMotion.defaultSpatial(),
        label = "amp"
    )

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = breathe
            scaleY = breathe
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseRadius = min(size.width, size.height) * 0.41f

            val outerPath = Path()
            val steps = 160
            for (i in 0..steps) {
                val theta = (i.toFloat() / steps) * (2f * PI.toFloat())
                val r = baseRadius * 1.12f * (1f + (animatedAmp * 0.8f) * cos(animatedLobes * theta - phase * 0.7f))
                val x = cx + r * cos(theta)
                val y = cy + r * sin(theta)
                if (i == 0) outerPath.moveTo(x, y) else outerPath.lineTo(x, y)
            }
            outerPath.close()
            drawPath(
                path = outerPath,
                color = secondaryColor.copy(alpha = 0.45f)
            )

            val mainPath = Path()
            for (i in 0..steps) {
                val theta = (i.toFloat() / steps) * (2f * PI.toFloat())
                val r = baseRadius * (1f + animatedAmp * cos(animatedLobes * theta + phase))
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

@Composable
fun ExpressiveWavyLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeWidth: Dp = 6.dp,
    waveAmplitude: Dp = 4.dp,
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
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

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
            val wavePath = Path()
            var x = startX
            val step = 3f
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
    val infiniteTransition = rememberInfiniteTransition(label = "circular_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "circular_phase"
    )

    Canvas(modifier = modifier) {
        val strokePx = strokeWidth.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadius = (min(size.width, size.height) / 2f) - strokePx * 1.8f
        val waveAmp = strokePx * 0.52f
        val frequency = 12f
        val steps = 180

        val bgPath = Path()
        for (i in 0..steps) {
            val ratio = i.toFloat() / steps
            val theta = -PI.toFloat() / 2f + ratio * 2f * PI.toFloat()
            val r = baseRadius + (waveAmp * 0.4f) * sin(frequency * theta + phase)
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
        val fgPath = Path()
        for (i in 0..activeSteps) {
            val ratio = i.toFloat() / steps
            val theta = -PI.toFloat() / 2f + ratio * 2f * PI.toFloat()
            val r = baseRadius + waveAmp * sin(frequency * theta - phase * 1.5f)
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

            val topStart by animateDpAsState(
                targetValue = if (isSelected || isFirst) 24.dp else 8.dp,
                animationSpec = ExpressiveMotion.fastSpatial(),
                label = "ts"
            )
            val bottomStart by animateDpAsState(
                targetValue = if (isSelected || isFirst) 24.dp else 8.dp,
                animationSpec = ExpressiveMotion.fastSpatial(),
                label = "bs"
            )
            val topEnd by animateDpAsState(
                targetValue = if (isSelected || isLast) 24.dp else 8.dp,
                animationSpec = ExpressiveMotion.fastSpatial(),
                label = "te"
            )
            val bottomEnd by animateDpAsState(
                targetValue = if (isSelected || isLast) 24.dp else 8.dp,
                animationSpec = ExpressiveMotion.fastSpatial(),
                label = "be"
            )

            val containerColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                },
                animationSpec = ExpressiveMotion.fastEffects(),
                label = "bg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = ExpressiveMotion.fastEffects(),
                label = "fg"
            )

            val weight by animateFloatAsState(
                targetValue = if (isSelected) 1.22f else 1f,
                animationSpec = ExpressiveMotion.fastSpatial(),
                label = "weight"
            )

            Box(
                modifier = Modifier
                    .weight(weight)
                    .height(46.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = topStart,
                            bottomStart = bottomStart,
                            topEnd = topEnd,
                            bottomEnd = bottomEnd
                        )
                    )
                    .background(containerColor)
                    .expressiveBounceClick(pressedScale = 0.92f) {
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

enum class AppTab(
    val title: String,
    val icon: ImageVector
) {
    STUDIO("创作工坊", Icons.Default.AutoAwesomeMotion),
    INSPECTOR("实况检验", Icons.Default.Verified),
    GALLERY("作品与兼容", Icons.Default.Collections)
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(68.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = ExpressiveMotion.fastEffects(),
                        label = "tab_pill_fg"
                    )
                    val itemWeight by animateFloatAsState(
                        targetValue = if (selected) 1.35f else 1f,
                        animationSpec = ExpressiveMotion.defaultSpatial(),
                        label = "tab_weight"
                    )

                    Row(
                        modifier = Modifier
                            .weight(itemWeight)
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(pillColor)
                            .expressiveBounceClick(pressedScale = 0.92f) {
                                onSelectTab(tab)
                            }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        AnimatedVisibility(visible = selected) {
                            Row {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = contentColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .size(68.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
                .expressiveBounceClick(pressedScale = 0.90f) {
                    onQuickPickVideo()
                },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "选择视频",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

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

    val exoPlayer = remember(videoFileOrUri) {
        if (videoFileOrUri == null) {
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
                    playWhenReady = autoPlayInitially
                }
            }
        }
    }

    LaunchedEffect(isPlayingLive, exoPlayer) {
        if (exoPlayer != null) {
            if (isPlayingLive) {
                exoPlayer.seekTo(0)
                exoPlayer.play()
            } else {
                exoPlayer.pause()
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }

    val cardScale by animateFloatAsState(
        targetValue = if (isPlayingLive) 1.018f else 1f,
        animationSpec = ExpressiveMotion.defaultSpatial(),
        label = "live_photo_scale"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (isPlayingLive) 22.dp else 32.dp,
        animationSpec = ExpressiveMotion.defaultSpatial(),
        label = "live_photo_corner"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isPlayingLive) 2.5.dp else 1.dp,
                color = if (isPlayingLive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(cornerRadius)
            )
            .expressiveBounceClick(pressedScale = 0.97f) {
                if (exoPlayer != null) {
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
            enter = fadeIn(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(220)),
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

        val badgeBg by animateColorAsState(
            targetValue = if (isPlayingLive) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Black.copy(alpha = 0.62f)
            },
            animationSpec = ExpressiveMotion.fastEffects(),
            label = "live_badge_bg"
        )
        val badgeFg by animateColorAsState(
            targetValue = if (isPlayingLive) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                Color.White
            },
            animationSpec = ExpressiveMotion.fastEffects(),
            label = "live_badge_fg"
        )

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
                text = if (isPlayingLive) "实况播放中 · LIVE" else "实况 · 点击播放",
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

            if (exoPlayer != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlayingLive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
