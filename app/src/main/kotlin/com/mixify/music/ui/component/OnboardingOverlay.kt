package com.mixify.music.ui.component

import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

data class OnboardingStep(
    val targetBounds: Rect,
    val title: String,
    val description: String,
)

@Composable
fun OnboardingOverlay(
    steps: List<OnboardingStep>,
    onFinish: () -> Unit,
) {
    if (steps.isEmpty()) return

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val currentStep = steps[currentStepIndex]

    val animatedBounds by animateRectAsState(
        targetValue = currentStep.targetBounds,
        animationSpec = tween(durationMillis = 400),
        label = "spotlight_bounds"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Consume all taps so they don't fall through to the UI below
                detectTapGestures { }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.99f } // Needed for BlendMode.Clear to work on a transparent activity
        ) {
            // Draw dark overlay
            drawRect(
                color = Color.Black.copy(alpha = 0.75f),
                size = size
            )

            // Punch out the spotlight hole
            drawRoundRect(
                color = Color.Transparent,
                topLeft = animatedBounds.topLeft,
                size = animatedBounds.size,
                cornerRadius = CornerRadius(48f, 48f),
                blendMode = BlendMode.Clear
            )
        }

        val configuration = LocalConfiguration.current
        val screenHeightPx = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }
        val isTargetInTopHalf = animatedBounds.center.y < (screenHeightPx / 2)

        var cardSize by remember { mutableStateOf(IntSize.Zero) }

        val cardOffsetY = with(LocalDensity.current) {
            if (isTargetInTopHalf) {
                (animatedBounds.bottom + 16.dp.toPx()).toDp()
            } else {
                (animatedBounds.top - cardSize.height - 16.dp.toPx()).toDp()
            }
        }

        Card(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .offset(y = cardOffsetY)
                .onSizeChanged { cardSize = it },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = currentStep.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentStep.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.width(280.dp) // Set a reasonable fixed/max width
                ) {
                    Text(
                        text = "${currentStepIndex + 1} / ${steps.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    
                    TextButton(onClick = onFinish) {
                        Text("Skip")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(onClick = {
                        if (currentStepIndex < steps.lastIndex) {
                            currentStepIndex++
                        } else {
                            onFinish()
                        }
                    }) {
                        Text(if (currentStepIndex < steps.lastIndex) "Next" else "Done")
                    }
                }
            }
        }
    }
}
