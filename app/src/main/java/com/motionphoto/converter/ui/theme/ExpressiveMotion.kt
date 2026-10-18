package com.motionphoto.converter.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Google Material 3 Expressive (MD3E) 物理弹簧动效系统
 * 遵循 M3 Expressive 官方 Spatial & Effects 双轨弹簧规范：
 * - Spatial 弹簧具备鲜明的弹性回弹 (dampingRatio 0.54 ~ 0.64)，赋予胶囊形变、卡片展开与按压回弹灵动张力
 * - Effects 弹簧采用无过冲平滑阻尼，用于色彩与透明度过渡
 * - 静态复用 SpringSpec 实例，兼顾满帧流畅度与表现力
 */
object ExpressiveMotion {

    private val FastSpatialSpec: SpringSpec<Any> = spring(
        dampingRatio = 0.55f,
        stiffness = 620f
    )

    private val DefaultSpatialSpec: SpringSpec<Any> = spring(
        dampingRatio = 0.60f,
        stiffness = 380f
    )

    private val SlowSpatialSpec: SpringSpec<Any> = spring(
        dampingRatio = 0.64f,
        stiffness = 220f
    )

    private val FastEffectsSpec: SpringSpec<Any> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 1400f
    )

    private val DefaultEffectsSpec: SpringSpec<Any> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 700f
    )

    @Stable
    @Suppress("UNCHECKED_CAST")
    fun <T> fastSpatial(): FiniteAnimationSpec<T> = FastSpatialSpec as FiniteAnimationSpec<T>

    @Stable
    @Suppress("UNCHECKED_CAST")
    fun <T> defaultSpatial(): FiniteAnimationSpec<T> = DefaultSpatialSpec as FiniteAnimationSpec<T>

    @Stable
    @Suppress("UNCHECKED_CAST")
    fun <T> slowSpatial(): FiniteAnimationSpec<T> = SlowSpatialSpec as FiniteAnimationSpec<T>

    @Stable
    @Suppress("UNCHECKED_CAST")
    fun <T> fastEffects(): FiniteAnimationSpec<T> = FastEffectsSpec as FiniteAnimationSpec<T>

    @Stable
    @Suppress("UNCHECKED_CAST")
    fun <T> defaultEffects(): FiniteAnimationSpec<T> = DefaultEffectsSpec as FiniteAnimationSpec<T>
}

/**
 * MD3 Expressive 硬件加速按压回弹修饰符
 * 按下时快速微缩，松手时触发富有弹性的 M3E 物理回弹，仅在 graphicsLayer 阶段更新缩放
 */
fun Modifier.expressiveBounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.93f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleState = animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = ExpressiveMotion.fastSpatial(),
        label = "expressive_press_scale"
    )
    this
        .graphicsLayer {
            val s = scaleState.value
            scaleX = s
            scaleY = s
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}
