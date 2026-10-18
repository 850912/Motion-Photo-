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
 * Material 3 Expressive (MD3E) 高响应物理弹簧动效引擎
 * 针对 120Hz 触控响应进行了高刚度与零分配优化（复用静态 SpringSpec 实例，避免重组时重复分配对象）
 */
object ExpressiveMotion {

    private val FastSpatialSpec: SpringSpec<Any> = spring(
        dampingRatio = 0.72f,
        stiffness = 2200f
    )

    private val DefaultSpatialSpec: SpringSpec<Any> = spring(
        dampingRatio = 0.78f,
        stiffness = 1100f
    )

    private val SlowSpatialSpec: SpringSpec<Any> = spring(
        dampingRatio = 0.82f,
        stiffness = 550f
    )

    private val FastEffectsSpec: SpringSpec<Any> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 3800f
    )

    private val DefaultEffectsSpec: SpringSpec<Any> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 2000f
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
 * 零重组开销的 GPU 硬件层 (`graphicsLayer`) 按压回弹修饰符
 * 仅在 graphicsLayer 阶段读取 scaleState.value，完全跳过 Composition 与 Layout 阶段
 */
fun Modifier.expressiveBounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
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
