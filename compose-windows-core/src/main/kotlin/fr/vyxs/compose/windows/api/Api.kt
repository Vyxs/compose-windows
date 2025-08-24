package fr.vyxs.compose.windows.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import fr.vyxs.compose.windows.runtime.WindowsWindow
import java.awt.EventQueue
import kotlin.math.roundToInt

/**
 * Launches a Windows-style Compose application with a customizable title bar while preserving
 * native Windows window behaviors (e.g., Snap, maximize, minimize, close).
 *
 * Use [WindowsAppScope.window], [WindowsAppScope.titleBar], and [WindowsAppScope.content]
 * inside the provided [block] to configure the window and its UI.
 *
 * Example:
 * ```kotlin
 * fun main() = WindowsApp {
 *   window { title("Demo"); size(900, 600) }
 *   titleBar { end { /* custom buttons */ } }
 *   content { /* app content */ }
 * }
 * ```
 */
fun WindowsApp(block: WindowsAppScope.() -> Unit) {
    val scope = WindowsAppScope().apply(block)
    EventQueue.invokeLater { scope.build().show() }
}

/**
 * Scope for configuring a Windows-style application window.
 *
 * Use this scope to:
 * - Configure the native window via [window]
 * - Configure the custom title bar via [titleBar]
 * - Provide the main Compose [content]
 */
class WindowsAppScope {
    internal val window = WindowConfig()
    internal val bar = TitleBarConfig()
    internal var contentComposable: (@Composable () -> Unit)? = null

    /** Configures the native window (title, size, resizable, etc.). */
    fun window(block: WindowConfig.() -> Unit) = window.block()

    /** Configures the custom title bar slots (start, center, end). */
    fun titleBar(block: TitleBarConfig.() -> Unit) = bar.block()

    /** Sets the application content Composable. */
    fun content(block: @Composable () -> Unit) { contentComposable = block }

    internal fun build() =
        WindowsWindow(window, bar, contentComposable)
}

/**
 * Window configuration for title, size, resizability, rounded corners and title bar appearance.
 *
 * Only negative values are prevented; very large values are allowed.
 * Title may be empty.
 */
class WindowConfig {
    /** Window title shown to the OS (can be empty if you want). */
    var title: String = "App"
        set(value) {
            field = value
        }

    /** Initial width in pixels (negative values coerced to 0). */
    var width: Int = 900
        set(value) {
            val nonNegative = value.coerceAtLeast(0)
            field = if (minWidth != null) nonNegative.coerceAtLeast(minWidth!!) else nonNegative
        }

    /** Initial height in pixels (negative values coerced to 0). */
    var height: Int = 600
        set(value) {
            val nonNegative = value.coerceAtLeast(0)
            field = if (minHeight != null) nonNegative.coerceAtLeast(minHeight!!) else nonNegative
        }

    /** Optional minimum width in pixels (negative values coerced to 0). */
    var minWidth: Int? = null
        set(value) {
            val v = value?.coerceAtLeast(0)
            field = v
            if (v != null && width < v) width = v
        }

    /** Optional minimum height in pixels (negative values coerced to 0). */
    var minHeight: Int? = null
        set(value) {
            val v = value?.coerceAtLeast(0)
            field = v
            if (v != null && height < v) height = v
        }

    /** Whether the window can be resized by the user. */
    var resizable: Boolean = true

    /** Corner radius in pixels (negative values coerced to 0). */
    var cornerRadius: Int = 2
        set(value) {
            field = value.coerceAtLeast(0)
        }

    /** Title bar background color (ARGB int). */
    var titleBarColor: Int = 0x202020

    /** Title bar height in pixels (negative values coerced to 0). */
    var titleBarHeight: Int = 40
        set(value) {
            field = value.coerceAtLeast(0)
        }

    /** Sets the window title (can be empty). */
    fun title(value: String) { this.title = value }

    /** Sets the initial window size in pixels (negatives coerced to 0). */
    fun size(w: Int, h: Int) { this.width = w; this.height = h }

    /** Sets the initial window size using [Dp] (negatives coerced to 0). */
    fun size(w: Dp, h: Dp) { size(w.value.roundToInt(), h.value.roundToInt()) }

    /** Sets the minimum window size in pixels (negatives coerced to 0). */
    fun minSize(w: Int, h: Int) { this.minWidth = w; this.minHeight = h }

    /** Sets the minimum window size using [Dp] (negatives coerced to 0). */
    fun minSize(w: Dp, h: Dp) { minSize(w.value.roundToInt(), h.value.roundToInt()) }

    /** Sets only the minimum width (negatives coerced to 0). */
    fun minWidth(px: Int) { this.minWidth = px }

    /** Sets only the minimum height (negatives coerced to 0). */
    fun minHeight(px: Int) { this.minHeight = px }

    /** Enables or disables window resizing. */
    fun resizable(isResizable: Boolean) { this.resizable = isResizable }

    /** Sets the corner radius from a [Dp] (negatives coerced to 0). */
    fun cornerRadius(radius: Dp) { this.cornerRadius = radius.value.roundToInt() }

    /** Sets the title bar color from a Compose [Color]. */
    fun titleBarColor(color: Color) { this.titleBarColor = color.toArgb() }

    /** Sets the title bar height from a [Dp] (negatives coerced to 0). */
    fun titleBarHeight(height: Dp) { this.titleBarHeight = height.value.roundToInt() }
}

/**
 * Title bar configuration.
 *
 * The title bar is split into three slots: [start], [center], and [end]. Each slot receives a
 * [TitleBarScope] granting access to the current title bar color and window [actions].
 */
class TitleBarConfig {
    internal var startContent: (@Composable TitleBarScope.() -> Unit)? = null
    internal var centerContent: (@Composable TitleBarScope.() -> Unit)? = null
    internal var endContent: (@Composable TitleBarScope.() -> Unit)? = null

    /** Defines the start (left) slot content of the title bar. */
    fun start(block: @Composable TitleBarScope.() -> Unit) { startContent = block }

    /** Defines the center slot content of the title bar. */
    fun center(block: @Composable TitleBarScope.() -> Unit) { centerContent = block }

    /** Defines the end (right) slot content of the title bar. */
    fun end(block: @Composable TitleBarScope.() -> Unit) { endContent = block }
}

/**
 * Window actions available to title bar elements.
 *
 * Use these callbacks to minimize, toggle maximize/restore, or close the window.
 */
class WindowActions(
    val minimize: () -> Unit,
    val toggleMaximize: () -> Unit,
    val close: () -> Unit
)

/**
 * Scope provided to title bar slot content.
 *
 * Exposes the resolved [titleBarColor] for styling and [actions] for integrating native window
 * behavior into custom controls.
 */
class TitleBarScope internal constructor(
    val titleBarColor: Color,
    val actions: WindowActions
)
