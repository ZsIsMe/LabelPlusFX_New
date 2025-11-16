package ink.meodinger.lpfx.util.color

import javafx.scene.paint.Color
import kotlin.math.pow


/**
 * Author: Meodinger
 * Date: 2021/7/29
 * Have fun with my code!
 */


/**
 * Apply an opacity to a Color
 */
fun Color.opacity(opacity: Double): Color = Color(red, green, blue, opacity)

/**
 * Get color RGBA in hex
 * @return String format in RRGGBBAA
 */
fun Color.toHexRGBA(): String = toString().uppercase().substring(2, 10)

/**
 * Get color RGB in hex
 * @return String format in RRGGBB
 */
fun Color.toHexRGB(): String = toString().uppercase().substring(2, 8)

/**
 * Whether a string is valid ColorHex, or in the other words,
 * format in `RRGGBB` or `RRGGBBAA`.
 */
fun String?.isColorHex(): Boolean {
    if (this == null) return false
    if (length != 6 && length != 8) return false
    for (c in uppercase().toCharArray()) if (c !in '0'..'9' && c !in 'A'..'F') return false

    return true
}

/**
 * Calculate the luminance of a color
 * @return luminance value between 0.0 and 1.0
 */
fun Color.luminance(): Double {
    val r = if (red <= 0.03928) red / 12.92 else ((red + 0.055) / 1.055).pow(2.4)
    val g = if (green <= 0.03928) green / 12.92 else ((green + 0.055) / 1.055).pow(2.4)
    val b = if (blue <= 0.03928) blue / 12.92 else ((blue + 0.055) / 1.055).pow(2.4)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

/**
 * Calculate contrast ratio between two colors
 * @param other The other color to compare with
 * @return contrast ratio between 1.0 and 21.0
 */
fun Color.contrastRatio(other: Color): Double {
    val lum1 = this.luminance()
    val lum2 = other.luminance()
    val lighter = kotlin.math.max(lum1, lum2)
    val darker = kotlin.math.min(lum1, lum2)
    return (lighter + 0.05) / (darker + 0.05)
}

/**
 * Get a contrasting text color (black or white) that has good readability against this background color
 * @return Color.BLACK or Color.WHITE
 */
fun Color.getContrastingTextColor(): Color {
    val whiteContrast = this.contrastRatio(Color.WHITE)
    val blackContrast = this.contrastRatio(Color.BLACK)
    
    // Choose the color with higher contrast ratio
    // WCAG AA standard requires at least 4.5:1 contrast ratio
    return if (whiteContrast > blackContrast) Color.WHITE else Color.BLACK
}