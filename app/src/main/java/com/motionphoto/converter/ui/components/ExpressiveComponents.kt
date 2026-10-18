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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.MotionPhotosOn
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
 * 1. Google Material 3 Expressive (MD3E) 灵动多叶徽章 (Cookie / Flower Shape Morphing)
 * 支持呼吸缩放与平滑花瓣旋转，同时缓存 Path 与 Brush 保证 120Hz 满帧渲染
 */
@Composable
fun ExpressiveMorphingBadge(
    modifier: Modifier = Modifier,
    lobes: Float = 8f,
    waveAmplitude: Float = 0.11f,
    isAnimating: Boolean = true,
    primaryColor: Color = MaterialTheme.colorScheme.primaryContainer,
    secondaryColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    outlineColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
    content: @Composable () -> Unit
) {
    val animatedLobes by animateFloatAsState(
        targetValue = lobes,
        animationSpec = ExpressiveMotion.defaultSpatial(),
        label = "lobes"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "m3e_badge_motion")
    val rotationDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isAnimating) 9000 else 24000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "badge_rot"
    )
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isAnimating) 1400 else 2600,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_breathe"
    )

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
                    rotationZ = rotationDeg
                    scaleX = breatheScale
                    scaleY = breatheScale
                }
                .drawWithCache {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val baseRadius = min(size.width, size.height) * 0.41f
                    val steps = 72
                    val lobesVal = animatedLobes

                    outerPath.rewind()
                    for (i in 0..steps) {
                        val theta = (i.toFloat() / steps) * (2f * PI.toFloat())
                        val r = baseRadius * 1.12f * (1f + (waveAmplitude * 0.78f) * cos(lobesVal * theta))
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

                    val outerFillColor = secondaryColor.copy(alpha = 0.45f)
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
 * 2. MD3 Expressive 波浪进度条
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
            animation = tween(durationMillis = 1000, easing = LinearEasing),
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
            val step = 5f
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
 * 3. MD3 Expressive 动态环形波浪光环
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
    val infiniteTransition = rememberInfiniteTransition(label = "halo_wave_phase")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_phase"
    )

    val bgPath = remember { Path() }
    val fgPath = remember { Path() }

    Spacer(
        modifier = modifier.drawWithCache {
            val strokePx = strokeWidth.toPx()
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseRadius = (min(size.width, size.height) / 2f) - strokePx * 1.8f
            val waveAmp = strokePx * 0.48f
            val frequency = 12f
            val steps = 90

            bgPath.rewind()
            for (i in 0..steps) {
                val ratio = i.toFloat() / steps
                val theta = -PI.toFloat() / 2f + ratio * 2f * PI.toFloat()
                val r = baseRadius + (waveAmp * 0.38f) * sin(frequency * theta)
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
                val currentPhase = phaseShift
                fgPath.rewind()
                for (i in 0..activeSteps) {
                    val ratio = i.toFloat() / steps
                    val theta = -PI.toFloat() / 2f + ratio * 2f * PI.toFloat()
                    val r = baseRadius + waveAmp * sin(frequency * theta - currentPhase)
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
 * 4. Google M3 Expressive 连通形变按钮组 (Connected Button Group)
 * 选中项动态拉伸宽度权重 + 圆角从方枕形弹跳过渡为胶囊圆角
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

            val itemWeight by animateFloatAsState(
                targetValue = if (isSelected) 1.28f else 1f,
                animationSpec = ExpressiveMotion.fastSpatial(),
                label = "btn_weight"
            )
            val startRadius by animateDpAsState(
                targetValue = if (isSelected || isFirst) 24.dp else 10.dp,
                animationSpec = ExpressiveMotion.fastSpatial(),
                label = "sr"
            )
            val endRadius by animateDpAsState(
                targetValue = if (isSelected || isLast) 24.dp else 10.dp,
                animationSpec = ExpressiveMotion.fastSpatial(),
                label = "er"
            )

            val containerColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                },
                animationSpec = ExpressiveMotion.fastEffects(),
                label = "btn_bg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = ExpressiveMotion.fastEffects(),
                label = "btn_fg"
            )

            Box(
                modifier = Modifier
                    .weight(itemWeight)
                    .height(46.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = startRadius,
                            bottomStart = startRadius,
                            topEnd = endRadius,
                            bottomEnd = endRadius
                        )
                    )
                    .drawBehind {
                        drawRect(color = containerColor)
                    }
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

/**
 * 5. 底部悬浮胶囊导航栏（包含「创作」「检验」「作品」「关于」四大页面）
 */
enum class AppTab(
    val title: String,
    val icon: ImageVector
) {
    STUDIO("创作", Icons.Rounded.AutoAwesomeMotion),
    INSPECTOR("检验", Icons.Rounded.Verified),
    GALLERY("作品", Icons.Rounded.Collections),
    ABOUT("关于", Icons.Rounded.Person);

    companion object {
        val Entries: List<AppTab> = values().toList()
    }
}

/**
 * 开源项目 Kyant0/AndroidLiquidGlass (Apache-2.0) 光学折射与色散着色器 (AGSL)
 * 仅作用于 Dock 栏底板背景层，绝不模糊或扭曲前景导航图标与文字
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

/**
 * 独立的 Dock 栏液态玻璃底板层修饰符
 * 仅对 Dock 栏背景胶囊本身应用光学透镜折射、模糊、菲涅尔高光与色散光圈，
 * 前景的导航文字、图标与选中胶囊位于上层独立图层，保持 100% 清晰锐利。
 */
@Composable
fun Modifier.kyantLiquidGlassDockBackdrop(
    config: MainViewModel.LiquidGlassConfig,
    surfaceColor: Color,
    primaryColor: Color,
    secondaryColor: Color,
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
        Color(0xFF1E1E2A).copy(alpha = (baseFrostAlpha + 0.18f).coerceAtMost(0.92f))
    } else {
        Color(0xFFFFFFFF).copy(alpha = (baseFrostAlpha + 0.22f).coerceAtMost(0.94f))
    }

    return this
        .shadow(
            elevation = (10f + config.specularStrength * 12f).dp,
            shape = shape,
            spotColor = primaryColor.copy(alpha = 0.30f),
            ambientColor = primaryColor.copy(alpha = 0.18f)
        )
        .clip(shape)
        // 1. 仅在背景层内部应用 AGSL 光学折射与毛玻璃模糊，然后再在前景绘制清晰锐利的高光边框
        .drawWithCache {
            val cr = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            val rimAlpha = (0.30f + config.specularStrength * 0.65f).coerceIn(0f, 1f)
            val dispAlpha = (config.dispersionStrength * 0.60f).coerceIn(0f, 0.85f)

            val topHighlightBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = rimAlpha),
                    primaryColor.copy(alpha = dispAlpha * 0.70f),
                    Color.White.copy(alpha = rimAlpha * 0.25f),
                    Color(0xFF70D6FF).copy(alpha = dispAlpha * 0.60f),
                    Color.White.copy(alpha = rimAlpha * 0.75f)
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )

            val innerLensBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = (0.22f + config.refractionStrength * 0.30f).coerceAtMost(0.62f)),
                    glassBaseColor,
                    primaryColor.copy(alpha = (0.08f + config.dispersionStrength * 0.18f).coerceAtMost(0.32f))
                )
            )

            val causticGlowBrush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.18f * config.refractionStrength),
                    secondaryColor.copy(alpha = 0.12f * config.dispersionStrength),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.28f, size.height * 0.25f),
                radius = size.width * 0.75f
            )

            val topSpecularBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.28f * config.specularStrength),
                    Color.Transparent
                ),
                startY = 0f,
                endY = size.height * 0.52f
            )
            val rimStroke = Stroke(width = (1.3f + config.specularStrength * 1.1f).dp.toPx())

            onDrawBehind {
                drawRoundRect(brush = innerLensBrush, cornerRadius = cr)
                drawRoundRect(brush = causticGlowBrush, cornerRadius = cr)
                drawRoundRect(brush = topSpecularBrush, cornerRadius = cr)
                drawRoundRect(brush = topHighlightBrush, cornerRadius = cr, style = rimStroke)
            }
        }
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
                            blurPx * 0.28f,
                            blurPx * 0.28f,
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
}

/**
 * 底部悬浮液态玻璃 Dock 栏：
 * - 已移除右下角多余的添加图片按钮
 * - 液态玻璃折射/模糊仅作用于底层 `kyantLiquidGlassDockBackdrop`，上层文字与图标 100% 清晰锐利
 * - 采用 Google M3 Expressive 弹簧形变（选中项弹性拉伸、圆角方枕→胶囊形变、图标微弹跳与标签丝滑展开）
 */
@Composable
fun ExpressiveFloatingToolbar(
    currentTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    liquidGlassConfig: MainViewModel.LiquidGlassConfig = MainViewModel.LiquidGlassConfig(),
    isDark: Boolean = false,
    onOpenLiquidGlassTuner: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .height(66.dp)
    ) {
        // 图层 1：独立的液态玻璃光学背景层（仅对 Dock 栏自身底板生效，不影响上层文字和图标）
        Box(
            modifier = Modifier
                .matchParentSize()
                .kyantLiquidGlassDockBackdrop(
                    config = liquidGlassConfig,
                    surfaceColor = MaterialTheme.colorScheme.surface,
                    primaryColor = MaterialTheme.colorScheme.primary,
                    secondaryColor = MaterialTheme.colorScheme.tertiary,
                    isDark = isDark
                )
        )

        // 图层 2：前景高清导航内容层（文字与图标不经过 RenderEffect 模糊或色散，保持锐利）
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppTab.Entries.forEach { tab ->
                val selected = tab == currentTab

                val tabWeight by animateFloatAsState(
                    targetValue = if (selected) 1.48f else 1f,
                    animationSpec = ExpressiveMotion.fastSpatial(),
                    label = "tab_weight"
                )
                val pillCorner by animateDpAsState(
                    targetValue = if (selected) 26.dp else 16.dp,
                    animationSpec = ExpressiveMotion.fastSpatial(),
                    label = "tab_corner"
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.12f else 1f,
                    animationSpec = ExpressiveMotion.fastSpatial(),
                    label = "tab_icon_scale"
                )

                val activeAlpha = if (liquidGlassConfig.enabled) {
                    (0.72f + liquidGlassConfig.tintAlpha * 0.22f).coerceIn(0.65f, 0.95f)
                } else 1f

                val pillColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = activeAlpha)
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
                    label = "tab_content_fg"
                )

                val itemShape = RoundedCornerShape(pillCorner)

                Row(
                    modifier = Modifier
                        .weight(tabWeight)
                        .height(52.dp)
                        .clip(itemShape)
                        .drawBehind {
                            drawRect(color = pillColor)
                        }
                        .then(
                            if (selected && liquidGlassConfig.enabled) {
                                Modifier.border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.70f * liquidGlassConfig.specularStrength),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                                        )
                                    ),
                                    shape = itemShape
                                )
                            } else Modifier
                        )
                        .expressiveBounceClick(pressedScale = 0.91f) {
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
                        modifier = Modifier
                            .size(21.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                    )
                    AnimatedVisibility(
                        visible = selected,
                        enter = fadeIn(animationSpec = ExpressiveMotion.fastEffects()) +
                            expandHorizontally(
                                animationSpec = ExpressiveMotion.fastSpatial(),
                                expandFrom = Alignment.Start
                            ),
                        exit = fadeOut(animationSpec = ExpressiveMotion.fastEffects()) +
                            shrinkHorizontally(
                                animationSpec = ExpressiveMotion.fastSpatial(),
                                shrinkTowards = Alignment.Start
                            )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(6.dp))
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

            // 液态玻璃实时自由调节入口按钮
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer.copy(
                            alpha = if (liquidGlassConfig.enabled) 0.78f else 0.92f
                        )
                    )
                    .expressiveBounceClick(pressedScale = 0.88f) {
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
}

/**
 * 6. 按需加载 (Lazy ExoPlayer) 的实况照片交互预览卡
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
        targetValue = if (isPlayingLive) 20.dp else 32.dp,
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
            .expressiveBounceClick(pressedScale = 0.97f) {
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
            enter = fadeIn(animationSpec = ExpressiveMotion.defaultEffects()),
            exit = fadeOut(animationSpec = ExpressiveMotion.fastEffects()),
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

        val badgeBg by animateColorAsState(
            targetValue = if (isPlayingLive) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Black.copy(alpha = 0.62f)
            },
            animationSpec = ExpressiveMotion.fastEffects(),
            label = "live_badge_bg"
        )
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
