package com.motionphoto.converter.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Picture
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.graphics.drawable.AnimatedImageDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
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
import androidx.compose.material.icons.rounded.GifBox
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
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
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 实时背景捕获状态 (Real-Time GPU Backdrop Capture State)
 * 参考 Kyant0/AndroidLiquidGlass、Kashif-E/KMPLiquidGlass 与 QWEA0/Liquid-Glass-Android：
 * - 在 Android 10+ (API 29+) 上使用硬件加速 `RenderNode` (GPU DisplayList) 零拷贝录制底层页面内容，
 *   完美支持 `Bitmap.Config.HARDWARE` 硬件位图、嵌套图层与实时滚动内容，绝不触发软件画布崩溃；
 * - 在 Android 8.0~9.0 (API 26~28) 上平稳回退至 `Picture` 录制。
 */
@Stable
class LiquidGlassBackdropState {
    val renderNode: RenderNode? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        RenderNode("LiquidGlassBackdrop")
    } else {
        null
    }
    val fallbackPicture: Picture = Picture()
    var sourceWindowPosition: Offset by mutableStateOf(Offset.Zero)
    var recordedWidth: Int by mutableIntStateOf(0)
    var recordedHeight: Int by mutableIntStateOf(0)
    var frameTick: Int by mutableIntStateOf(0)
}

@Composable
fun rememberLiquidGlassBackdropState(): LiquidGlassBackdropState {
    return remember { LiquidGlassBackdropState() }
}

/**
 * 标记主页面内容为液态玻璃的 GPU 实时采样背景源
 */
fun Modifier.liquidGlassBackdropSource(
    state: LiquidGlassBackdropState,
    enabled: Boolean = true
): Modifier = this
    .onGloballyPositioned { coords ->
        state.sourceWindowPosition = coords.positionInWindow()
    }
    .drawWithContent {
        drawContent()
        if (enabled && size.width > 1f && size.height > 1f) {
            val w = size.width.toInt()
            val h = size.height.toInt()
            val node = state.renderNode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && node != null) {
                try {
                    node.setPosition(0, 0, w, h)
                    val recCanvas = node.beginRecording(w, h)
                    try {
                        draw(
                            density = this,
                            layoutDirection = this.layoutDirection,
                            canvas = androidx.compose.ui.graphics.Canvas(recCanvas),
                            size = this.size
                        ) {
                            this@drawWithContent.drawContent()
                        }
                    } finally {
                        node.endRecording()
                    }
                    state.recordedWidth = w
                    state.recordedHeight = h
                    state.frameTick = (state.frameTick + 1) and 0x7FFFFFFF
                } catch (_: Throwable) {
                }
            } else {
                try {
                    val picCanvas = state.fallbackPicture.beginRecording(w, h)
                    try {
                        draw(
                            density = this,
                            layoutDirection = this.layoutDirection,
                            canvas = androidx.compose.ui.graphics.Canvas(picCanvas),
                            size = this.size
                        ) {
                            this@drawWithContent.drawContent()
                        }
                    } finally {
                        state.fallbackPicture.endRecording()
                    }
                    state.recordedWidth = w
                    state.recordedHeight = h
                    state.frameTick = (state.frameTick + 1) and 0x7FFFFFFF
                } catch (_: Throwable) {
                }
            }
        }
    }

/**
 * 重力感应动态光源方向追踪 (参考 QWEA0/Liquid-Glass-Android `sensorHighlight`)
 * 随手机握持倾角实时平滑偏移液态玻璃边缘高光角度
 */
@Composable
private fun rememberDeviceTiltLightVector(enabled: Boolean): Offset {
    val context = LocalContext.current
    var lightVec by remember { mutableStateOf(Offset(-0.58f, -0.81f)) }

    DisposableEffect(context, enabled) {
        if (!enabled) {
            return@DisposableEffect onDispose {}
        }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensorManager == null || sensor == null) {
            return@DisposableEffect onDispose {}
        }

        var smoothX = -0.58f
        var smoothY = -0.81f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val values = event?.values ?: return
                if (values.size < 2) return
                val rawX = (-values[0] / 9.81f).coerceIn(-1f, 1f)
                val rawY = (-values[1] / 9.81f).coerceIn(-1f, 1f)
                val targetX = (rawX * 0.65f - 0.35f).coerceIn(-1f, 1f)
                val targetY = (rawY * 0.55f - 0.55f).coerceIn(-1f, -0.15f)
                smoothX += (targetX - smoothX) * 0.14f
                smoothY += (targetY - smoothY) * 0.14f
                val len = sqrt(smoothX * smoothX + smoothY * smoothY).coerceAtLeast(0.001f)
                lightVec = Offset(smoothX / len, smoothY / len)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        try {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        } catch (_: Throwable) {}

        onDispose {
            try {
                sensorManager.unregisterListener(listener)
            } catch (_: Throwable) {}
        }
    }
    return lightVec
}

/**
 * 1. Google Material 3 Expressive (MD3E) 灵动多叶徽章 (Cookie / Flower Shape Morphing)
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
 * 深度还原 Kyant0/AndroidLiquidGlass + QWEA0/Liquid-Glass-Android 2.0 光学透镜着色器 (AGSL)：
 * 1. 解析主胶囊与选中 Tab 水滴胶囊的双重圆角矩形 SDF 梯度法线 (`gradSdRoundedBox`)，实现水滴透镜融合折射
 * 2. 采用 `circleBevel` 四分之一圆弧 + 逆幂律 (`inversePower`) 复合剖面，在胶囊边缘形成细腻的 iOS 26 / Kyant 光学折叠环
 * 3. 七通道光谱棱镜色散采样 (`R / O / Y / G / C / B / V`) + 背景色彩 Vibrancy 增艳矩阵
 * 4. 重力感应动态光源 (`lightDir`) + 跟随选中 Tab 的焦散水滴透镜 (`dropletCenter` / `dropletHalfSize`)
 */
private const val KYANT_LIQUID_GLASS_AGSL = """
    uniform shader content;
    uniform float2 resolution;
    uniform float cornerRadius;
    uniform float refractionStrength;
    uniform float dispersionStrength;
    uniform float specularStrength;
    uniform float4 tintColor;
    uniform float2 dropletCenter;
    uniform float2 dropletHalfSize;
    uniform float2 lightDir;

    float sdRoundedBox(float2 p, float2 b, float r) {
        float2 q = abs(p) - b + r;
        return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
    }

    float2 gradSdRoundedBox(float2 p, float2 b, float r) {
        float2 q = abs(p) - b + r;
        float2 s = sign(p);
        if (q.x > 0.0 && q.y > 0.0) {
            return s * normalize(q);
        } else if (q.x > q.y) {
            return float2(s.x, 0.0);
        } else {
            return float2(0.0, s.y);
        }
    }

    float circleBevel(float x) {
        float cx = clamp(x, 0.0, 1.0);
        return 1.0 - sqrt(max(0.0, 1.0 - cx * cx));
    }

    half4 main(float2 coord) {
        float2 halfSize = resolution * 0.5;
        float2 p = coord - halfSize;
        float r = clamp(cornerRadius, 6.0, min(halfSize.x, halfSize.y));
        float dOuter = sdRoundedBox(p, halfSize, r);

        // 1. 外层 Dock 胶囊边缘四分之一圆弧 + 逆幂律透镜折射带
        float bevelWidth = max(r * 0.95, 24.0);
        float rimProgress = clamp(1.0 + dOuter / bevelWidth, 0.0, 1.0);
        float lensOuter = circleBevel(rimProgress) * (0.65 + 0.35 * rimProgress);
        float2 sdfNormalOuter = gradSdRoundedBox(p, halfSize, r);
        float2 radialNormal = length(p) > 0.001 ? normalize(p) : float2(0.0, 0.0);
        float2 normalOuter = normalize(mix(sdfNormalOuter, radialNormal, 0.20));

        // 2. 内部选中 Tab 液态水滴胶囊 (Droplet Lens) 局部凸透镜微折射
        float2 pDrop = coord - dropletCenter;
        float rDrop = min(dropletHalfSize.x, dropletHalfSize.y);
        float dDrop = sdRoundedBox(pDrop, dropletHalfSize, rDrop);
        float dropRim = clamp(1.0 - abs(dDrop) / 18.0, 0.0, 1.0);
        float2 normalDrop = gradSdRoundedBox(pDrop, dropletHalfSize, rDrop);

        // 综合折射位移向量
        float maxRefractPx = (20.0 + r * 0.55) * refractionStrength;
        float2 refractOffset = (-normalOuter * lensOuter * maxRefractPx)
            - (normalDrop * circleBevel(dropRim) * 8.5 * refractionStrength);

        // 3. 七通道光谱棱镜色散采样 (ROYGCBV)
        float2 dispDir = normalize(normalOuter + normalDrop * 0.35 + float2(0.0001, 0.0001));
        float dispMag = (lensOuter * (0.40 + 0.60 * rimProgress) + dropRim * 0.28) * dispersionStrength * 4.2;
        float2 dispStep = dispDir * dispMag;
        float2 minBound = float2(1.0, 1.0);
        float2 maxBound = max(resolution - 1.0, minBound);

        float2 baseCoord = coord + refractOffset;
        half4 cR = content.eval(clamp(baseCoord - dispStep * 2.4, minBound, maxBound));
        half4 cO = content.eval(clamp(baseCoord - dispStep * 1.6, minBound, maxBound));
        half4 cY = content.eval(clamp(baseCoord - dispStep * 0.8, minBound, maxBound));
        half4 cG = content.eval(clamp(baseCoord,                  minBound, maxBound));
        half4 cC = content.eval(clamp(baseCoord + dispStep * 0.8, minBound, maxBound));
        half4 cB = content.eval(clamp(baseCoord + dispStep * 1.6, minBound, maxBound));
        half4 cV = content.eval(clamp(baseCoord + dispStep * 2.4, minBound, maxBound));

        half3 refractedRgb = half3(
            (cR.r * 0.50 + cO.r * 0.32 + cY.r * 0.18),
            (cY.g * 0.22 + cG.g * 0.56 + cC.g * 0.22),
            (cC.b * 0.18 + cB.b * 0.48 + cV.b * 0.34)
        );

        // 4. Kyant Vibrancy 色彩增艳与通透对比度提升
        half luma = dot(refractedRgb, half3(0.299, 0.587, 0.114));
        half3 vibrantRgb = clamp(mix(half3(luma), refractedRgb, half(1.34)), half3(0.0), half3(1.0));

        // 5. 混合清透玻璃色调（保留底层实时滚动画面细节）
        half3 tinted = mix(vibrantRgb, half3(tintColor.rgb), half(tintColor.a));

        // 6. 重力感应双瓣对称高光 (pow 4.5 角度瓣) + 选中水滴焦散光晕
        float2 n = normalOuter;
        float primaryLobe = pow(clamp(dot(n, lightDir) * 0.5 + 0.5, 0.0, 1.0), 3.5);
        float oppositeLobe = pow(clamp(dot(n, -lightDir) * 0.5 + 0.5, 0.0, 1.0), 4.2) * 0.65;
        float rimSpecular = pow(rimProgress, 2.8) * specularStrength * (0.24 + 0.76 * (primaryLobe + oppositeLobe));

        float dropSpecular = pow(dropRim, 3.2) * specularStrength * 0.28 *
            clamp(dot(normalDrop, lightDir) * 0.5 + 0.5, 0.0, 1.0);

        tinted += half3(rimSpecular * 0.72 + dropSpecular);

        return half4(clamp(tinted, half3(0.0), half3(1.0)), 1.0);
    }
"""

/**
 * 独立的 Dock 栏实时 GPU 背景液态玻璃底板层修饰符：
 * - 通过硬件加速 `RenderNode` 零拷贝重绘 Dock 栏正下方的滚动页面内容，并执行「先高斯模糊 -> 后 AGSL 双圆角 SDF 透镜折射与七通道光谱色散」
 * - 前景的导航文字、图标与选中胶囊位于上层独立兄弟图层，100% 清晰锐利，绝不被模糊或扭曲
 */
@Composable
fun Modifier.kyantLiquidGlassDockBackdrop(
    config: MainViewModel.LiquidGlassConfig,
    backdropState: LiquidGlassBackdropState?,
    dockWindowPosition: Offset,
    activeFocusFractionX: Float,
    lightDir: Offset,
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

    // 降低底板遮罩不透明度，让底层 RenderNode 实时捕获的页面色彩与边缘充分透射并产生明显的透镜折射感
    val baseFrostAlpha = (config.tintAlpha * 0.52f).coerceIn(0.06f, 0.58f)
    val glassBaseColor = if (isDark) {
        Color(0xFF161622).copy(alpha = baseFrostAlpha)
    } else {
        Color(0xFFFFFFFF).copy(alpha = (baseFrostAlpha + 0.06f).coerceAtMost(0.64f))
    }

    return this
        .shadow(
            elevation = (10f + config.specularStrength * 12f).dp,
            shape = shape,
            spotColor = primaryColor.copy(alpha = 0.34f),
            ambientColor = primaryColor.copy(alpha = 0.18f)
        )
        .clip(shape)
        .drawWithCache {
            val cr = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            val rimAlpha = (0.38f + config.specularStrength * 0.62f).coerceIn(0f, 1f)
            val dispAlpha = (config.dispersionStrength * 0.72f).coerceIn(0f, 0.92f)

            val innerLensBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = (0.14f + config.refractionStrength * 0.18f).coerceAtMost(0.38f)),
                    glassBaseColor,
                    primaryColor.copy(alpha = (0.06f + config.dispersionStrength * 0.12f).coerceAtMost(0.22f))
                )
            )

            val focusX = (size.width * activeFocusFractionX).coerceIn(0f, size.width)
            val causticGlowBrush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.24f * config.refractionStrength),
                    secondaryColor.copy(alpha = 0.14f * config.dispersionStrength),
                    Color.Transparent
                ),
                center = Offset(focusX, size.height * 0.45f),
                radius = (size.width * 0.48f).coerceAtLeast(1f)
            )

            val lightStart = Offset(
                x = size.width * (0.5f + lightDir.x * 0.5f),
                y = size.height * (0.5f + lightDir.y * 0.5f)
            )
            val lightEnd = Offset(
                x = size.width * (0.5f - lightDir.x * 0.5f),
                y = size.height * (0.5f - lightDir.y * 0.5f)
            )

            val topHighlightBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = rimAlpha),
                    primaryColor.copy(alpha = dispAlpha * 0.76f),
                    Color.White.copy(alpha = rimAlpha * 0.24f),
                    Color(0xFF70D6FF).copy(alpha = dispAlpha * 0.68f),
                    Color.White.copy(alpha = rimAlpha * 0.84f)
                ),
                start = lightStart,
                end = lightEnd
            )
            val rimStroke = Stroke(width = (1.4f + config.specularStrength * 1.1f).dp.toPx())

            onDrawBehind {
                // 1. 绘制底层实时捕获的 GPU DisplayList (RenderNode / Picture)，使 Dock 栏实时透视并折射下方滚动的页面元素
                drawRect(color = surfaceColor)
                if (backdropState != null && backdropState.frameTick > 0 && backdropState.recordedWidth > 0) {
                    val dx = backdropState.sourceWindowPosition.x - dockWindowPosition.x
                    val dy = backdropState.sourceWindowPosition.y - dockWindowPosition.y
                    val nativeCanvas = drawContext.canvas.nativeCanvas
                    val saveCount = nativeCanvas.save()
                    try {
                        nativeCanvas.translate(dx, dy)
                        val node = backdropState.renderNode
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && node != null && node.hasDisplayList()) {
                            nativeCanvas.drawRenderNode(node)
                        } else if (backdropState.fallbackPicture.width > 0) {
                            nativeCanvas.drawPicture(backdropState.fallbackPicture)
                        }
                    } catch (_: Throwable) {
                    } finally {
                        nativeCanvas.restoreToCount(saveCount)
                    }
                }

                // 2. 叠加清透玻璃漫射层、动态水滴焦散与随重力感应旋转的菲涅尔棱镜边框
                drawRoundRect(brush = innerLensBrush, cornerRadius = cr)
                drawRoundRect(brush = causticGlowBrush, cornerRadius = cr)
                drawRoundRect(brush = topHighlightBrush, cornerRadius = cr, style = rimStroke)
            }
        }
        .graphicsLayer {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && cachedRuntimeShader != null) {
                try {
                    val w = size.width.coerceAtLeast(1f)
                    val h = size.height.coerceAtLeast(1f)
                    cachedRuntimeShader.setFloatUniform("resolution", w, h)
                    cachedRuntimeShader.setFloatUniform("cornerRadius", cornerRadiusPx)
                    cachedRuntimeShader.setFloatUniform("refractionStrength", config.refractionStrength)
                    cachedRuntimeShader.setFloatUniform("dispersionStrength", config.dispersionStrength)
                    cachedRuntimeShader.setFloatUniform("specularStrength", config.specularStrength)
                    cachedRuntimeShader.setFloatUniform(
                        "tintColor",
                        surfaceColor.red,
                        surfaceColor.green,
                        surfaceColor.blue,
                        (config.tintAlpha * 0.34f).coerceIn(0.04f, 0.45f)
                    )
                    cachedRuntimeShader.setFloatUniform(
                        "dropletCenter",
                        w * activeFocusFractionX,
                        h * 0.50f
                    )
                    cachedRuntimeShader.setFloatUniform(
                        "dropletHalfSize",
                        w * 0.14f,
                        h * 0.39f
                    )
                    cachedRuntimeShader.setFloatUniform(
                        "lightDir",
                        lightDir.x,
                        lightDir.y
                    )
                    val shaderEffect = RenderEffect.createRuntimeShaderEffect(cachedRuntimeShader, "content")
                    // 遵循 Kyant0/AndroidLiquidGlass 光学顺序：先对背景执行高斯模糊 (inner)，再进入 AGSL 双 SDF 透镜折射与七波段色散 (outer)
                    renderEffect = if (blurPx > 0.5f) {
                        val blurEffect = RenderEffect.createBlurEffect(
                            blurPx * 0.62f,
                            blurPx * 0.62f,
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
 * - 实时折射并模糊 Dock 栏下方的页面滚动内容 (`backdropState`)
 * - 液态玻璃折射/模糊仅作用于底层 `kyantLiquidGlassDockBackdrop`，上层文字与图标 100% 清晰锐利
 * - 支持重力感应动态高光追踪与选中 Tab 水滴透镜聚焦
 */
@Composable
fun ExpressiveFloatingToolbar(
    currentTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    backdropState: LiquidGlassBackdropState? = null,
    liquidGlassConfig: MainViewModel.LiquidGlassConfig = MainViewModel.LiquidGlassConfig(),
    isDark: Boolean = false,
    onOpenLiquidGlassTuner: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var dockWindowPosition by remember { mutableStateOf(Offset.Zero) }
    val lightDir = rememberDeviceTiltLightVector(enabled = liquidGlassConfig.enabled)

    val targetFocusFraction = remember(currentTab) {
        (currentTab.ordinal + 0.5f) / (AppTab.Entries.size + 0.55f)
    }
    val animatedFocusFraction by animateFloatAsState(
        targetValue = targetFocusFraction,
        animationSpec = ExpressiveMotion.defaultSpatial(),
        label = "dock_light_focus"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .height(66.dp)
            .onGloballyPositioned { coords ->
                dockWindowPosition = coords.positionInWindow()
            }
    ) {
        // 图层 1：独立的实时 GPU 背景采样液态玻璃底板层（仅对 Dock 栏自身底板生效，不模糊前景文字和图标）
        Box(
            modifier = Modifier
                .matchParentSize()
                .kyantLiquidGlassDockBackdrop(
                    config = liquidGlassConfig,
                    backdropState = backdropState,
                    dockWindowPosition = dockWindowPosition,
                    activeFocusFractionX = animatedFocusFraction,
                    lightDir = lightDir,
                    surfaceColor = MaterialTheme.colorScheme.surface,
                    primaryColor = MaterialTheme.colorScheme.primary,
                    secondaryColor = MaterialTheme.colorScheme.tertiary,
                    isDark = isDark
                )
        )

        // 图层 2：前景高清导航内容层（文字与图标不经过 RenderEffect 模糊或色散，保持 100% 锐利）
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
                    (0.58f + liquidGlassConfig.tintAlpha * 0.24f).coerceIn(0.52f, 0.88f)
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
                                            Color.White.copy(alpha = 0.82f * liquidGlassConfig.specularStrength),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
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
                            alpha = if (liquidGlassConfig.enabled) 0.72f else 0.92f
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
 * 6. 原生硬件加速 GIF 动图循环预览卡 (AnimatedImageDrawable / ImageDecoder)
 * 确保在 App 内制作完成或浏览历史作品时，微信 GIF 动图直接动态循环播放
 */
@Composable
fun AnimatedGifPreviewCard(
    gifFile: File,
    resolutionLabel: String,
    fileSizeBytes: Long,
    modifier: Modifier = Modifier
) {
    val sizeKb = (fileSizeBytes / 1024L).coerceAtLeast(1L)
    val isWeChatStickerReady = fileSizeBytes <= 1024L * 1024L

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { imageView ->
                if (gifFile.exists()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        try {
                            val source = ImageDecoder.createSource(gifFile)
                            val drawable = ImageDecoder.decodeDrawable(source)
                            imageView.setImageDrawable(drawable)
                            if (drawable is AnimatedImageDrawable) {
                                drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                                drawable.start()
                            }
                        } catch (_: Throwable) {
                            imageView.setImageBitmap(BitmapFactory.decodeFile(gifFile.absolutePath))
                        }
                    } else {
                        imageView.setImageBitmap(BitmapFactory.decodeFile(gifFile.absolutePath))
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.GifBox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isWeChatStickerReady) "GIF 循环播放中 · 可直接添加为微信表情" else "高清 GIF 循环播放中",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.62f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = "$resolutionLabel · ${sizeKb} KB",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

/**
 * 7. 按需加载 (Lazy ExoPlayer) 的实况照片交互预览卡
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
            .allowHardware(false)
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
                    text = "$resolutionLabel · ${String.format(Locale.US, "%.1fs", durationMs / 1000f)}",
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
