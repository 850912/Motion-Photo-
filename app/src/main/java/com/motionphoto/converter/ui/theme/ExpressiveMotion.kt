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
 * Material 3 Expressive (MD3E) 物理弹簧动效引擎 (MotionScheme)
 */
object ExpressiveMotion {

    fun <T> fastSpatial(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.56f,
        stiffness = 1100f
    )

    fun <T> defaultSpatial(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.62f,
        stiffness = 620f
    )

    fun <T> slowSpatial(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.68f,
        stiffness = 320f
    )

    fun <T> fastEffects(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 2800f
    )

    fun <T> defaultEffects(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 1400f
    )
}

fun Modifier.expressiveBounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.94f,
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
