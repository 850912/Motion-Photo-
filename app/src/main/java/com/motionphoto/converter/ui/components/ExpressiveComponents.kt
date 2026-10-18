package com.motionphoto.converter.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.net.Uri
import android.os.Build
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
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.MotionPhotosOn
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import coil.request.ImageRequest
import com.motionphoto.converter.ui.MainViewModel
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
 * - 使用 drawWithCache 缓存几何 Path 与渐变 Brush，避免每帧重复计算 140+ 次三角函数与分配 Brush
 * - 旋转动画仅在 graphicsLayer 阶段更新 RenderNode 属性，CPU 零重绘开销
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

    val rotationState = if (isAnimating) {
        val infiniteTransition = rememberInfiniteTransition(label = "badge_spin")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rot"
        )
    } else {
        null
    }

    val outerPath = remember { Path() }
    val mainPath = remember { Path() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotationState?.value ?: 0f
                }
                .drawWithCache {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val baseRadius = min(size.width, size.height) * 0.41f
                    val steps = 64
                    val lobesVal = animatedLobes

                    outerPath.rewind()
                    for (i in 0..steps) {
                        val theta = (i.toFloat() / steps) * (2f * PI.toFloat())
                        val r = baseRadius * 1.11f * (1f + (waveAmplitude * 0.75f) * cos(lobesVal * theta))
                        val x = cx + r * cos(theta)
                        val y = cy + r * sin(theta)
                        if (i == 0) outerPath.moveTo(x, y) else outerPath.lineTo(x, y)
                    }
                    outerPath.close()

                    mainPath.rewind()
                    for (i in 0..steps) {
                        val theta = (i.toFloat() / steps) * (2f * PI.toFloat())
                        val r = baseRadius * (1f + waveAmplitude * cos(lobesVal * theta))
                        val x = cx + r * cos(theta)
                        val y = cy + r * sin(theta)
                        if (i == 0) mainPath.moveTo(x, y) else mainPath.lineTo(x, y)
                    }
                    mainPath.close()

                    val outerFillColor = secondaryColor.copy(alpha = 0.42f)
                    val mainBrush = Brush.linearGradient(
                        colors = listOf(primaryColor, secondaryColor),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                    val strokeStyle = Stroke(width = 2.dp.toPx())

                    onDrawBehind {
                        drawPath(path = outerPath, color = outerFillColor)
                        drawPath(path = mainPath, brush = mainBrush)
                        drawPath(path = mainPath, color = outlineColor, style = strokeStyle)
                    }
                }
        )
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
    val animatedProgressState = animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = ExpressiveMotion.defaultSpatial(),
        label = "wavy_linear_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "wave_shift")
    val wavePhaseState = infiniteTransition.animateFloat(
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
        val animatedProgress = animatedProgressState.value
        val wavePhase = wavePhaseState.value
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
            val step = 6f
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
 * 3. 零分配环形波浪光环 (缓存背景轨道 Path)
 */
@Composable
fun ExpressiveWavyCircularHalo(
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
    strokeWidth: Dp = 7.dp
) {
    val animatedProgressState = animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = ExpressiveMotion.defaultSpatial(),
        label = "circular_wavy_progress"
    )
    val bgPath = remember { Path() }
    val fgPath = remember { Path() }

    Spacer(
        modifier = modifier.drawWithCache {
            val strokePx = strokeWidth.toPx()
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseRadius = (min(size.width, size.height) / 2f) - strokePx * 1.8f
            val waveAmp = strokePx * 0.45f
            val frequency = 12f
            val steps = 84

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

            val bgStroke = Stroke(width = strokePx * 0.75f, cap = StrokeCap.Round)
            val fgStroke = Stroke(width = strokePx, cap = StrokeCap.Round)

            onDrawBehind {
                drawPath(path = bgPath, color = trackColor, style = bgStroke)

                val activeSteps = (steps * animatedProgressState.value).toInt().coerceAtLeast(2)
                fgPath.rewind()
                for (i in 0..activeSteps) {
                    val ratio = i.toFloat() / steps
                    val theta = -PI.toFloat() / 2f + ratio * 2f * PI.toFloat()
                    val r = baseRadius + waveAmp * sin(frequency * theta)
                    val x = cx + r * cos(theta)
                    val y = cy + r * sin(theta)
                    if (i == 0) fgPath.moveTo(x, y) else fgPath.lineTo(x, y)
                }
                drawPath(path = fgPath, color = activeColor, style = fgStroke)
            }
        }
    )
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
 * 5. 底部悬浮胶囊导航栏（已精简为「创作」「检验」「作品」三大核心页面）
 */
enum class AppTab(
    val title: String,
    val icon: ImageVector
) {
    STUDIO("创作", Icons.Rounded.AutoAwesomeMotion),
    INSPECTOR("检验", Icons.Rounded.Verified),
    GALLERY("作品", Icons.Rounded.Collections);

    companion object {
        val Entries: List<AppTab> = values().toList()
    }
}

/**
 * 开源项目 Kyant0/AndroidLiquidGlass (Apache-2.0) 光学折射与色散着色器 (AGSL)
 */
private const val KYANT_LIQUID_GLASS_AGSL = """
    uniform shader content;
    uniform float2 resolution;
    uniform float cornerRadius;
    uniform float refractionStrength;
    uniform float dispersionStrength;
    uniform float specularStrength;
    uniform float4 tintColor;

    float sdRoundedBox(float2 p, float2 b, float r) {
        float2 q = abs(p) - b + r;
        return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
    }

    half4 main(float2 coord) {
        float2 halfSize = resolution * 0.5;
        float2 p = coord - halfSize;
        float r = clamp(cornerRadius, 4.0, min(halfSize.x, halfSize.y));
        float d = sdRoundedBox(p, halfSize, r);

        // 边缘透镜折射带宽
        float rimWidth = max(r * 0.85, 18.0);
        float edgeFactor = clamp(1.0 + d / rimWidth, 0.0, 1.0);
        float lensCurve = edgeFactor * edgeFactor * (3.0 - 2.0 * edgeFactor);

        // 计算指向中心的折射法线
        float2 normal = length(p) > 0.001 ? normalize(p) : float2(0.0, 0.0);
        float2 offset = -normal * lensCurve * refractionStrength * 18.0;

        // 棱镜色散 (Chromatic Aberration R/G/B 通道分离采样)
        float2 disp = normal * lensCurve * dispersionStrength * 4.5;
        half4 colR = content.eval(clamp(coord + offset - disp, float2(0.0), resolution));
        half4 colG = content.eval(clamp(coord + offset, float2(0.0), resolution));
        half4 colB = content.eval(clamp(coord + offset + disp, float2(0.0), resolution));
        half4 baseCol = half4(colR.r, colG.g, colB.b, colG.a);

        // 叠加玻璃色调与漫射浓度
        half3 tinted = mix(baseCol.rgb, half3(tintColor.rgb), half(tintColor.a));

        // 菲涅尔左上光源边缘高光
        float lightDir = clamp((-normal.x * 0.65 - normal.y * 0.75) * 0.5 + 0.5, 0.0, 1.0);
        float rimGlow = pow(edgeFactor, 2.4) * specularStrength * (0.35 + 0.65 * lightDir);
        tinted += half3(rimGlow * 0.55);

        return half4(clamp(tinted, half3(0.0), half3(1.0)), baseCol.a);
    }
"""

@Composable
fun Modifier.kyantLiquidGlass(
    config: MainViewModel.LiquidGlassConfig,
    surfaceColor: Color,
    primaryColor: Color,
    isDark: Boolean
): Modifier {
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { config.cornerRadiusDp.dp.toPx() }
    val blurPx = with(density) { config.blurRadiusDp.dp.toPx() }
    val shape = remember(config.cornerRadiusDp) { RoundedCornerShape(config.cornerRadiusDp.dp) }

    if (!config.enabled) {
        return this
            .shadow(elevation = 10.dp, shape = shape)
            .clip(shape)
            .background(surfaceColor.copy(alpha = 0.96f))
    }

    // 复用 RuntimeShader 实例，避免在 graphicsLayer 中重复编译 AGSL 着色器导致掉帧
    val cachedRuntimeShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                RuntimeShader(KYANT_LIQUID_GLASS_AGSL)
            } catch (_: Throwable) {
                null
            }
        } else {
            null
        }
    }

    val baseFrostAlpha = config.tintAlpha.coerceIn(0.05f, 0.88f)
    val glassBaseColor = if (isDark) {
        Color(0xFF1E1E28).copy(alpha = baseFrostAlpha)
    } else {
        Color(0xFFFFFFFF).copy(alpha = (baseFrostAlpha + 0.12f).coerceAtMost(0.90f))
    }

    return this
        .shadow(
            elevation = (10f + config.specularStrength * 10f).dp,
            shape = shape,
            spotColor = primaryColor.copy(alpha = 0.28f),
            ambientColor = primaryColor.copy(alpha = 0.16f)
        )
        .clip(shape)
        .graphicsLayer {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && cachedRuntimeShader != null) {
                try {
                    cachedRuntimeShader.setFloatUniform(
                        "resolution",
                        size.width.coerceAtLeast(1f),
                        size.height.coerceAtLeast(1f)
                    )
                    cachedRuntimeShader.setFloatUniform("cornerRadius", cornerRadiusPx)
                    cachedRuntimeShader.setFloatUniform("refractionStrength", config.refractionStrength)
                    cachedRuntimeShader.setFloatUniform("dispersionStrength", config.dispersionStrength)
                    cachedRuntimeShader.setFloatUniform("specularStrength", config.specularStrength)
                    cachedRuntimeShader.setFloatUniform(
                        "tintColor",
                        surfaceColor.red,
                        surfaceColor.green,
                        surfaceColor.blue,
                        config.tintAlpha * 0.45f
                    )
                    val shaderEffect = RenderEffect.createRuntimeShaderEffect(cachedRuntimeShader, "content")
                    renderEffect = if (blurPx > 0.5f) {
                        val blurEffect = RenderEffect.createBlurEffect(
                            blurPx * 0.35f,
                            blurPx * 0.35f,
                            Shader.TileMode.CLAMP
                        )
                        RenderEffect.createChainEffect(shaderEffect, blurEffect).asComposeRenderEffect()
                    } else {
                        shaderEffect.asComposeRenderEffect()
                    }
                } catch (_: Throwable) {
                }
            }
        }
        .drawWithCache {
            val cr = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            val rimAlpha = (0.25f + config.specularStrength * 0.65f).coerceIn(0f, 1f)
            val dispAlpha = (config.dispersionStrength * 0.55f).coerceIn(0f, 0.8f)

            val topHighlightBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = rimAlpha),
                    primaryColor.copy(alpha = dispAlpha * 0.65f),
                    Color.White.copy(alpha = rimAlpha * 0.20f),
                    Color(0xFF70D6FF).copy(alpha = dispAlpha * 0.55f),
                    Color.White.copy(alpha = rimAlpha * 0.70f)
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )

            val innerLensBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = (0.18f + config.refractionStrength * 0.28f).coerceAtMost(0.55f)),
                    glassBaseColor,
                    primaryColor.copy(alpha = (0.06f + config.dispersionStrength * 0.14f).coerceAtMost(0.28f))
                )
            )

            // 在 drawWithCache 阶段缓存顶部高光渐变与边框 Stroke，避免 onDrawWithContent 每帧分配
            val topSpecularBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f * config.specularStrength),
                    Color.Transparent
                ),
                startY = 0f,
                endY = size.height * 0.48f
            )
            val rimStroke = Stroke(width = (1.2f + config.specularStrength * 1.1f).dp.toPx())

            onDrawWithContent {
                drawRoundRect(brush = innerLensBrush, cornerRadius = cr)
                drawRoundRect(brush = topSpecularBrush, cornerRadius = cr)
                drawContent()
                drawRoundRect(brush = topHighlightBrush, cornerRadius = cr, style = rimStroke)
            }
        }
}

@Composable
fun ExpressiveFloatingToolbar(
    currentTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    onQuickPickVideo: () -> Unit,
    liquidGlassConfig: MainViewModel.LiquidGlassConfig = MainViewModel.LiquidGlassConfig(),
    isDark: Boolean = false,
    onOpenLiquidGlassTuner: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pillCornerShape = remember(liquidGlassConfig.cornerRadiusDp) {
        RoundedCornerShape(liquidGlassConfig.cornerRadiusDp.dp)
    }

    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .kyantLiquidGlass(
                    config = liquidGlassConfig,
                    surfaceColor = MaterialTheme.colorScheme.surface,
                    primaryColor = MaterialTheme.colorScheme.primary,
                    isDark = isDark
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppTab.Entries.forEach { tab ->
                    val selected = tab == currentTab
                    val activeAlpha = if (liquidGlassConfig.enabled) {
                        (0.55f + liquidGlassConfig.tintAlpha * 0.35f).coerceIn(0.45f, 0.92f)
                    } else 1f
                    val pillColorState = animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = activeAlpha)
                        } else {
                            Color.Transparent
                        },
                        animationSpec = ExpressiveMotion.fastEffects(),
                        label = "tab_pill_bg"
                    )
                    val contentColor = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }

                    Row(
                        modifier = Modifier
                            .weight(if (selected) 1.28f else 1f)
                            .height(50.dp)
                            .clip(pillCornerShape)
                            .drawBehind {
                                drawRect(color = pillColorState.value)
                            }
                            .then(
                                if (selected && liquidGlassConfig.enabled) {
                                    Modifier.border(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.65f * liquidGlassConfig.specularStrength),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                            )
                                        ),
                                        shape = pillCornerShape
                                    )
                                } else Modifier
                            )
                            .expressiveBounceClick(pressedScale = 0.94f) {
                                onSelectTab(tab)
                            }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = contentColor,
                            maxLines = 1
                        )
                    }
                }

                // 液态玻璃实时自由调节入口按钮
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(
                                alpha = if (liquidGlassConfig.enabled) 0.65f else 0.9f
                            )
                        )
                        .expressiveBounceClick(pressedScale = 0.90f) {
                            onOpenLiquidGlassTuner()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "调节液态玻璃",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(19.dp)
                    )
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
                    imageVector = Icons.Rounded.AddPhotoAlternate,
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

    val imageRequest = remember(context, coverImageModel) {
        ImageRequest.Builder(context)
            .data(coverImageModel)
            .crossfade(false)
            .build()
    }

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
            model = imageRequest,
            contentDescription = "Motion Photo Cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = isPlayingLive && exoPlayer != null,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150)),
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
                        if (view.player !== exoPlayer) {
                            view.player = exoPlayer
                        }
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
                imageVector = Icons.Rounded.MotionPhotosOn,
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
                    imageVector = if (isPlayingLive) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
