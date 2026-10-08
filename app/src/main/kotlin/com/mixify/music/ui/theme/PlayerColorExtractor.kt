/**
 * Mixify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.mixify.music.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.palette.graphics.Palette

object PlayerColorExtractor {
    fun extractGradientColors(
        palette: Palette,
        fallbackColor: Int,
    ): List<Color> {
        // Use vibrant swatch if available for brighter, more vivid colors, otherwise fallback to ranked color
        val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.swatches.maxByOrNull { it.population }
        val rawColor = swatch?.rgb?.let { Color(it) } ?: Color(palette.rankedColors(1, fallbackColor).first())
        
        // Blend with less black (0.3f instead of 0.65f) so the colors are brighter, more vivid, and vibrant
        val primaryColor = lerp(rawColor, Color.Black, 0.3f)
        return listOf(
            primaryColor,
            lerp(primaryColor, Color.Black, 0.25f),
            lerp(primaryColor, Color.Black, 0.5f),
        )
    }
}
