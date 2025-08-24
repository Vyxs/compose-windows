package fr.vyxs.compose.windows.runtime

import androidx.compose.ui.graphics.Color

internal fun Color.darker(factor: Float): Color {
    fun ch(v: Float) = (v * (1f - factor)).coerceIn(0f, 1f)
    return Color(ch(red), ch(green), ch(blue), alpha)
}

internal fun Color.lighter(factor: Float): Color {
    fun ch(v: Float) = (v + (1f - v) * factor).coerceIn(0f, 1f)
    return Color(ch(red), ch(green), ch(blue), alpha)
}

internal fun Color.grayscale(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}

internal fun Color.tintRed(): Color {
    val g = grayscale()
    return Color(
        red = (g + 0.6f).coerceIn(0f, 1f),
        green = (g * 0.4f).coerceIn(0f, 1f),
        blue = (g * 0.4f).coerceIn(0f, 1f),
        alpha = 1f
    )
}