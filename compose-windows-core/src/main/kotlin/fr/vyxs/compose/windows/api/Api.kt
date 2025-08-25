package fr.vyxs.compose.windows.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import java.awt.EventQueue
import kotlin.math.roundToInt

/**
 * Launches a Compose Desktop application with a Windows-style custom title bar
 * while preserving native OS behaviors such as Snap, maximize, minimize, and close.
 *
 * This overload separates native window configuration from UI composition:
 * - [configure] sets the window properties (title, size, etc.)
 * - [block] is a fully composable DSL with access to all standard Compose APIs
 *   (e.g., remember, derivedStateOf, CompositionLocal, collectAsState)
 */
fun WindowsApp(
    configure: WindowConfig.() -> Unit,
    block: @Composable WindowsAppScope.() -> Unit
) {
    val scope = WindowsAppScope().apply { window.configure() }
    EventQueue.invokeLater { scope.build(block).show() }
}

/**
 * Launches the application using a single composable DSL block that defines both
 * window configuration and UI composition through [WindowsAppScope].
 */
fun WindowsApp(block: @Composable WindowsAppScope.() -> Unit) {
    val scope = WindowsAppScope()
    EventQueue.invokeLater { scope.build(block).show() }
}

/**
 * DSL scope for configuring the native window and declaring UI.
 *
 * Responsibilities:
 * - [window]: configure native properties (title, size, resizable, etc.)
 * - [titleBar]: declare custom title bar UI via start/center/end slots
 * - [content]: declare the main application content
 *
 * The [WindowsApp] block is composable, so any standard Compose API can be used
 * at the top level (e.g., `val state = remember { ... }`) and will be captured
 * by the slot lambdas declared through [titleBar] and [content].
 */
class WindowsAppScope {
    internal val window = WindowConfig()
    internal val bar = TitleBarConfig()
    internal var contentComposable: (@Composable () -> Unit)? = null

    /**
     * Configures the native window (title, size, resizability, rounded corners, colors).
     * This is typically called from the non-composable [WindowsApp] `configure` parameter.
     */
    fun window(block: WindowConfig.() -> Unit) = window.block()

    /**
     * Declares the custom title bar UI by providing [start], [center], and [end] slots.
     * Each slot receives a [TitleBarScope] with the resolved color and native actions.
     */
    fun titleBar(block: TitleBarConfig.() -> Unit) = bar.block()

    /**
     * Declares the main content Composable of the application window.
     */
    fun content(block: @Composable () -> Unit) { contentComposable = block }

    internal fun build(setup: @Composable WindowsAppScope.() -> Unit) =
        fr.vyxs.compose.windows.runtime.WindowsWindow(window, this, setup)
}

/**
 * Mutable native window configuration. Invalid inputs (e.g., negative sizes)
 * are coerced to safe values, and minimum constraints are enforced.
 */
class WindowConfig {
    var title: String = "App"
        set(value) { field = value }

    var width: Int = 900
        set(value) {
            val v = value.coerceAtLeast(0)
            field = if (minWidth != null) v.coerceAtLeast(minWidth!!) else v
        }

    var height: Int = 600
        set(value) {
            val v = value.coerceAtLeast(0)
            field = if (minHeight != null) v.coerceAtLeast(minHeight!!) else v
        }

    var minWidth: Int? = null
        set(value) {
            val v = value?.coerceAtLeast(0)
            field = v
            if (v != null && width < v) width = v
        }

    var minHeight: Int? = null
        set(value) {
            val v = value?.coerceAtLeast(0)
            field = v
            if (v != null && height < v) height = v
        }

    var resizable: Boolean = true

    var cornerRadius: Int = 2
        set(value) { field = value.coerceAtLeast(0) }

    var titleBarColor: Int = 0x202020

    var titleBarHeight: Int = 40
        set(value) { field = value.coerceAtLeast(0) }

    fun title(value: String) { this.title = value }
    fun size(w: Int, h: Int) { this.width = w; this.height = h }
    fun size(w: Dp, h: Dp) { size(w.value.roundToInt(), h.value.roundToInt()) }
    fun minSize(w: Int, h: Int) { this.minWidth = w; this.minHeight = h }
    fun minSize(w: Dp, h: Dp) { minSize(w.value.roundToInt(), h.value.roundToInt()) }
    fun minWidth(px: Int) { this.minWidth = px }
    fun minHeight(px: Int) { this.minHeight = px }
    fun resizable(isResizable: Boolean) { this.resizable = isResizable }
    fun cornerRadius(radius: Dp) { this.cornerRadius = radius.value.roundToInt() }
    fun titleBarColor(color: Color) { this.titleBarColor = color.toArgb() }
    fun titleBarHeight(height: Dp) { this.titleBarHeight = height.value.roundToInt() }
}

/**
 * Declarative container for title bar slots. Each slot is a Composable lambda that
 * receives a [TitleBarScope] bound to the current window instance.
 */
class TitleBarConfig {
    internal var startContent: (@Composable TitleBarScope.() -> Unit)? = null
    internal var centerContent: (@Composable TitleBarScope.() -> Unit)? = null
    internal var endContent: (@Composable TitleBarScope.() -> Unit)? = null

    fun start(block: @Composable TitleBarScope.() -> Unit) { startContent = block }
    fun center(block: @Composable TitleBarScope.() -> Unit) { centerContent = block }
    fun end(block: @Composable TitleBarScope.() -> Unit) { endContent = block }
}

/**
 * Native window action callbacks for title bar controls.
 */
class WindowActions(
    val minimize: () -> Unit,
    val toggleMaximize: () -> Unit,
    val close: () -> Unit
)

/**
 * Scope passed to title bar slot content. Provides the resolved [titleBarColor]
 * and [actions] to integrate custom UI with native window controls.
 */
class TitleBarScope internal constructor(
    val titleBarColor: Color,
    val actions: WindowActions
)
