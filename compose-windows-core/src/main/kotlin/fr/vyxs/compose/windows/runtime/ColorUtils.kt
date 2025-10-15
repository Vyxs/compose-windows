package fr.vyxs.compose.windows.runtime

import androidx.compose.ui.graphics.Color

/**
 * Returns a darker variant of this [Color] by blending toward black.
 * @param factor Range [0, 1]. Higher makes the color darker.
 */
fun Color.darker(factor: Float): Color {
    fun ch(v: Float) = (v * (1f - factor)).coerceIn(0f, 1f)
    return Color(ch(red), ch(green), ch(blue), alpha)
}

/**
 * Returns a lighter variant of this [Color] by blending toward white.
 * @param factor Range [0, 1]. Higher makes the color lighter.
 */
fun Color.lighter(factor: Float): Color {
    fun ch(v: Float) = (v + (1f - v) * factor).coerceIn(0f, 1f)
    return Color(ch(red), ch(green), ch(blue), alpha)
}

/**
 * Computes a simple luminance-like grayscale value of this [Color].
 * @return Value in [0, 1], where 0 is black and 1 is white.
 */
fun Color.grayscale(): Float =
    (0.299f * red + 0.587f * green + 0.114f * blue)

/**
 * Tints this [Color] toward a soft red tone while preserving alpha.
 * @param intensity Range [0, 1]. Higher increases the red influence.
 */
fun Color.tintRed(intensity: Float = 0.25f): Color {
    val a = intensity.coerceIn(0f, 1f)
    val r = 1f
    val g = 0.2f
    val b = 0.2f
    return Color(
        red * (1 - a) + r * a,
        green * (1 - a) + g * a,
        blue * (1 - a) + b * a,
        alpha
    )
}
