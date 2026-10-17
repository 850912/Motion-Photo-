package com.motionphoto.converter.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Material 3 Expressive (MD3E) 高响应物理弹簧动效引擎
 * 针对 120Hz 触控响应进行了高刚度优化 (stiffness 提升，消除拖泥带水感)
 */
object ExpressiveMotion {

    fun <T> fastSpatial(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.68f,
        stiffness = 1800f
    )

    fun <T> defaultSpatial(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.74f,
        stiffness = 950f
    )

    fun <T> slowSpatial(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.78f,
        stiffness = 500f
    )

    fun <T> fastEffects(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 3400f
    )

    fun <T> defaultEffects(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 1800f
    )
}

/**
 * 零重组开销的 GPU 硬件层 (`graphicsLayer`) 按压回弹修饰符
 */
fun Modifier.expressiveBounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = ExpressiveMotion.fastSpatial(),
        label = "expressive_press_scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}
