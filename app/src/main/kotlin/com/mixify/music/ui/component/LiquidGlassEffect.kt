/**
 * Mixify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Liquid Glass effect — ported from SimpMusic's
 * `ui/component/LiquidGlassContainer.kt` (and its `expect/ui/LiquidGlass.kt`).
 *
 * SimpMusic is a Kotlin Multiplatform project, so the original code splits into
 * an `expect`/`actual` pair (`PlatformBackdrop`) to support Android + Desktop + iOS.
 * Mixify is a single-module Android app, so that split is unnecessary here: this
 * file talks to `com.kyant.backdrop` directly. Behaviour (blur curve, scrim,
 * press/hold "liquid" glow) matches the original 1:1.
 *
 * Requires the `io.github.kyant0:backdrop` library (added in step 1 below).
 */
package com.mixify.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop as nativeLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.sign

/**
 * Global on/off switch for the effect, read from the "Enable Liquid Glass" setting.
 * Every glass surface checks this before drawing, so turning the setting off falls
 * back to a flat translucent pill everywhere with no other code changes needed.
 * Provide the real value from settings inside MixifyTheme() — see step 3.
 */
val LocalLiquidGlassEnabled = compositionLocalOf { false }

/** Whether the app is currently in dark theme — used to pick a black or white scrim. */
val LocalIsDarkTheme = compositionLocalOf { false }

/** Alias kept only for readability at call sites (bottom nav bar, mini player). */
typealias AppBackdrop = LayerBackdrop

/**
 * Marks a composable as the backdrop source: the content that sibling glass
 * surfaces (bottom nav, mini player) will sample and refract. Wrap the screen
 * content Box that sits BEHIND the nav bar / mini player with this — the
 * NavHost container in MainActivity.
 */
fun Modifier.layerBackdrop(backdrop: AppBackdrop): Modifier = this.nativeLayerBackdrop(backdrop)

/** Creates the shared backdrop instance. Remember this once per screen and pass it down. */
@Composable
fun rememberAppBackdrop(fallbackColor: Color): AppBackdrop =
    rememberLayerBackdrop {
        drawRect(fallbackColor)
        drawContent()
    }

/**
 * Applies the liquid-glass effect to any element (bottom nav pill, mini player pill).
 *
 * With the setting off, falls back to the flat translucent pill Mixify already
 * used (surfaceContainerHighest @ 80%) — same shape and hit target, only the draw
 * changes, so callers don't need to branch on the setting themselves.
 *
 * @param interactive set false for a static glass surface (no press glow).
 */
@Composable
fun Modifier.liquidGlass(
    backdrop: AppBackdrop,
    shape: Shape = CircleShape,
    interactive: Boolean = true,
    highlight: Highlight = Highlight.Default,
): Modifier {
    if (!LocalLiquidGlassEnabled.current) {
        return this
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f))
    }
    val isDark = LocalIsDarkTheme.current
    val layer = rememberGraphicsLayer()
    val interaction = rememberGlassInteraction()
    return this.drawInteractiveGlass(
        isDark = isDark,
        backdrop = backdrop,
        layer = layer,
        luminanceAnimation = 0.5f,
        shape = shape,
        interaction = if (interactive) interaction else null,
        highlight = highlight,
    )
}

/**
 * Overload for surfaces that sample their own background luminance so the glass
 * keeps adapting to whatever is scrolling behind it. Without per-frame luminance
 * sampling wired up, just pass a fixed value like 0.5f for [luminanceAnimation].
 */
@Composable
fun Modifier.liquidGlass(
    backdrop: AppBackdrop,
    layer: GraphicsLayer,
    luminanceAnimation: Float,
    shape: Shape = CircleShape,
    interactive: Boolean = true,
    blurScale: Float = 1f,
    minScrim: Float = 0.12f,
    maxScrim: Float = 0.5f,
): Modifier {
    if (!LocalLiquidGlassEnabled.current) {
        return this
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f))
    }
    val isDark = LocalIsDarkTheme.current
    val interaction = rememberGlassInteraction()
    return this.drawInteractiveGlass(
        isDark = isDark,
        backdrop = backdrop,
        layer = layer,
        luminanceAnimation = luminanceAnimation,
        shape = shape,
        interaction = if (interactive) interaction else null,
        pressedScale = 1.04f,
        blurScale = blurScale,
        minScrim = minScrim,
        maxScrim = maxScrim,
    )
}

/** Press/hold state holder for a single liquid-glass surface. */
class GlassInteraction(private val scope: CoroutineScope) {
    private val pressSpec = spring<Float>(dampingRatio = 0.6f, stiffness = 300f)
    private val pressAnimation = Animatable(0f)
    val pressProgress: Float get() = pressAnimation.value
    var touchPosition by mutableStateOf(Offset.Zero)

    suspend fun detectPress(scope: PointerInputScope) {
        with(scope) {
            inspectDragGestures(
                onDragStart = { down ->
                    touchPosition = down.position
                    this@GlassInteraction.scope.launch { pressAnimation.animateTo(1f, pressSpec) }
                },
                onDragEnd = { _ ->
                    this@GlassInteraction.scope.launch { pressAnimation.animateTo(0f, pressSpec) }
                },
                onDragCancel = {
                    this@GlassInteraction.scope.launch { pressAnimation.animateTo(0f, pressSpec) }
                },
            ) { change, _ ->
                touchPosition = change.position
            }
        }
    }
}

@Composable
internal fun rememberGlassInteraction(): GlassInteraction {
    val scope = rememberCoroutineScope()
    return remember(scope) { GlassInteraction(scope) }
}

/**
 * Draws the liquid-glass effect: real-time blur + refraction (via Kyant's
 * `drawBackdrop` RuntimeShader pipeline) plus an optional press response — the
 * surface scales up slightly, refraction/blur deepen, and a radial glow follows
 * the pointer, springing back on release. Pass `interaction = null` for a static
 * surface (e.g. a decorative panel that isn't tappable).
 */
fun Modifier.drawInteractiveGlass(
    isDark: Boolean,
    backdrop: AppBackdrop,
    layer: GraphicsLayer,
    luminanceAnimation: Float,
    shape: Shape,
    interaction: GlassInteraction?,
    pressedScale: Float = 1.12f,
    highlight: Highlight = Highlight.Default,
    blurScale: Float = 1f,
    minScrim: Float = 0.12f,
    maxScrim: Float = 0.5f,
): Modifier =
    this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            highlight = { highlight },
            effects = {
                val l = (luminanceAnimation * 2f - 1f).let { sign(it) * it * it }
                val press = interaction?.pressProgress ?: 0f
                vibrancy()
                colorControls(brightness = 0.05f, contrast = 1f, saturation = 1.5f)
                blur(
                    (
                        if (l > 0f) lerp(8f.dp.toPx(), 16f.dp.toPx(), l)
                        else lerp(8f.dp.toPx(), 2f.dp.toPx(), -l)
                    ) * blurScale + 2f.dp.toPx() * press,
                )
                lens(size.minDimension / 4f + 2f.dp.toPx() * press, size.minDimension / 2f, false)
            },
            onDrawBackdrop = { drawBackdrop ->
                drawBackdrop()
                layer.record { drawBackdrop() }
            },
            onDrawSurface = {
                val darken = lerp(minScrim, maxScrim, ((luminanceAnimation - 0.3f) / 0.5f).coerceIn(0f, 1f))
                drawRect((if (isDark) Color.Black else Color.White).copy(alpha = darken))
                val press = interaction?.pressProgress ?: 0f
                if (press > 0f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.18f * press), Color.Transparent),
                            center = interaction?.touchPosition ?: Offset(size.width / 2f, size.height / 2f),
                            radius = size.minDimension * 1.2f,
                        ),
                        blendMode = BlendMode.Plus,
                    )
                }
            },
            layerBlock = if (interaction != null) {
                { val scale = lerp(1f, pressedScale, interaction.pressProgress); scaleX = scale; scaleY = scale }
            } else null,
        ).then(
            if (interaction != null) Modifier.pointerInput(interaction) { interaction.detectPress(this) }
            else Modifier,
        )

/** Observe-only drag/press recogniser — never consumes events, so wrapped buttons still work. */
internal suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val down = awaitFirstDown(requireUnconsumed = false)
        onDragStart(down)
        onDrag(down, Offset.Zero)
        val upEvent = drag(pointerId = down.id, onDrag = { onDrag(it, it.positionChange()) })
        if (upEvent == null) onDragCancel() else onDragEnd(upEvent)
    }
}

private suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
): PointerInputChange? {
    val isPointerUp = currentEvent.changes.fastFirstOrNull { it.id == pointerId }?.pressed != true
    if (isPointerUp) return null
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer) ?: return null
        if (change.isConsumed) return null
        if (change.changedToUpIgnoreConsumed()) return change
        onDrag(change)
        pointer = change.id
    }
}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(pointerId: PointerId): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) return dragEvent else pointer = otherDown.id
        } else {
            if (dragEvent.previousPosition != dragEvent.position) return dragEvent
        }
    }
}
