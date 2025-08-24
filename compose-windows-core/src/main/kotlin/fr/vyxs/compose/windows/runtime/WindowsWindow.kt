package fr.vyxs.compose.windows.runtime

import androidx.compose.runtime.Composable
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.Color
import com.formdev.flatlaf.FlatDarkLaf
import fr.vyxs.compose.windows.api.TitleBarConfig
import fr.vyxs.compose.windows.api.TitleBarScope
import fr.vyxs.compose.windows.api.WindowActions
import fr.vyxs.compose.windows.api.WindowConfig
import java.awt.BorderLayout
import java.awt.Color as AwtColor
import java.awt.Dimension
import java.awt.Frame
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JComponent
import javax.swing.JLayeredPane
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.WindowConstants
import javax.swing.border.EmptyBorder
import kotlin.math.max

internal class WindowsWindow(
    private val win: WindowConfig,
    private val bar: TitleBarConfig,
    private val content: (@Composable () -> Unit)?
) {
    private companion object {
        private const val PROVISIONAL_W = 4096
    }

    private val lastWidths = mutableMapOf<ComposePanel, Int>()

    fun show() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater { show() }
            return
        }

        System.setProperty("flatlaf.useWindowDecorations", "true")
        FlatDarkLaf.setup()

        UIManager.put("Component.arc", win.cornerRadius)
        UIManager.put("Button.arc", win.cornerRadius)
        UIManager.put("TextComponent.arc", win.cornerRadius)
        UIManager.put("TitlePane.unifiedBackground", true)

        val frame = JFrame(win.title).apply {
            defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            layout = BorderLayout()
            isResizable = win.resizable
        }

        val root = frame.rootPane
        root.putClientProperty("FlatLaf.fullWindowContent", true)
        root.putClientProperty("JRootPane.titleBarShowIcon", false)
        root.putClientProperty("JRootPane.titleBarShowTitle", false)
        root.putClientProperty("JRootPane.titleBarShowIconify", false)
        root.putClientProperty("JRootPane.titleBarShowMaximize", false)
        root.putClientProperty("JRootPane.titleBarShowClose", false)
        root.putClientProperty("JRootPane.titleBarHeight", win.titleBarHeight)

        run {
            val mw = win.minWidth
            val mh = win.minHeight
            if (mw != null || mh != null) {
                frame.minimumSize = Dimension(
                    (mw ?: 0).coerceAtLeast(0),
                    (mh ?: 0).coerceAtLeast(0)
                )
            }
        }

        val titleBgAwt = AwtColor(win.titleBarColor, (win.titleBarColor ushr 24) != 0)
        val titleBg = titleBgAwt.toCompose()
        frame.background = titleBgAwt
        root.putClientProperty("JRootPane.titleBarBackground", titleBgAwt)

        val topBar = JPanel(null).apply {
            background = titleBgAwt
            isOpaque = true
            border = EmptyBorder(0, 0, 0, 0)
            preferredSize = Dimension(0, win.titleBarHeight)
        }

        val startPanel = makeSlotPanel(titleBgAwt).also { topBar.add(it) }
        val centerPanel = makeSlotPanel(titleBgAwt).also { topBar.add(it) }
        val endPanel = makeSlotPanel(titleBgAwt).also { topBar.add(it) }

        val actions = WindowActions(
            minimize = { frame.extendedState = frame.extendedState or Frame.ICONIFIED },
            toggleMaximize = {
                val st = frame.extendedState
                frame.extendedState =
                    if ((st and Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) Frame.NORMAL
                    else Frame.MAXIMIZED_BOTH
            },
            close = { frame.dispose() }
        )
        val scope = TitleBarScope(titleBg, actions)

        fun relayout() {
            val w = max(1, topBar.width)
            val h = win.titleBarHeight
            val pad = 0

            fun layoutW(panel: ComposePanel): Int =
                (lastWidths[panel] ?: panel.preferredSize?.width ?: 0).coerceAtLeast(0)

            val startW = layoutW(startPanel)
            val endW = layoutW(endPanel)
            val centerW = layoutW(centerPanel)

            startPanel.setBounds(pad, 0, startW, h)

            val endX = (w - endW - pad).coerceAtLeast(pad)
            endPanel.setBounds(endX, 0, endW, h)

            val maxX = w - centerW - pad
            val cx = if (maxX >= pad) ((w - centerW) / 2).coerceIn(pad, maxX) else pad
            centerPanel.setBounds(cx, 0, centerW, h)
        }

        fun bindSlot(panel: ComposePanel, content: @Composable () -> Unit) {
            val h = win.titleBarHeight
            panel.minimumSize = Dimension(1, h)
            panel.preferredSize = Dimension(PROVISIONAL_W, h)
            panel.setContent(content)
        }

        val expectedSlots = listOfNotNull(bar.startContent, bar.centerContent, bar.endContent).size
        var measuredSlots = 0
        var overlayActive = expectedSlots > 0

        val barOverlay = if (overlayActive) JPanel().apply {
            isOpaque = true
            background = titleBgAwt
        } else null

        fun layoutOverlay() {
            if (!overlayActive || barOverlay == null) return
            val p = SwingUtilities.convertPoint(topBar, 0, 0, frame.layeredPane)
            barOverlay.setBounds(p.x, p.y, topBar.width, topBar.height)
            barOverlay.isVisible = true
            frame.layeredPane.repaint()
        }

        fun removeOverlayIfReady() {
            if (!overlayActive || barOverlay == null) return
            if (measuredSlots >= expectedSlots) {
                frame.layeredPane.remove(barOverlay)
                overlayActive = false
                frame.layeredPane.revalidate()
                frame.layeredPane.repaint()
            }
        }

        fun onMeasuredWidth(panel: ComposePanel, w: Int) {
            if (w <= 0) return
            val old = lastWidths[panel]
            if (old == w) return
            val wasKnown = old != null
            lastWidths[panel] = w
            setPanelWidth(panel, w, win.titleBarHeight, topBar)
            relayout()
            if (!wasKnown && expectedSlots > 0) {
                measuredSlots++
                removeOverlayIfReady()
            }
        }

        bar.startContent?.let { c ->
            bindSlot(startPanel) {
                TitleSlotBackplate(
                    bg = titleBg,
                    contentChangedWidth = { w -> SwingUtilities.invokeLater { onMeasuredWidth(startPanel, w) } }
                ) { with(scope) { c() } }
            }
        }

        bar.centerContent?.let { c ->
            bindSlot(centerPanel) {
                TitleSlotBackplate(
                    bg = titleBg,
                    contentChangedWidth = { w -> SwingUtilities.invokeLater { onMeasuredWidth(centerPanel, w) } }
                ) { with(scope) { c() } }
            }
        }

        bar.endContent?.let { c ->
            bindSlot(endPanel) {
                TitleSlotBackplate(
                    bg = titleBg,
                    contentChangedWidth = { w -> SwingUtilities.invokeLater { onMeasuredWidth(endPanel, w) } }
                ) { with(scope) { c() } }
            }
        }

        topBar.addComponentListener(object : ComponentAdapter() {
            override fun componentShown(e: ComponentEvent) {
                relayout(); layoutOverlay()
            }
            override fun componentResized(e: ComponentEvent) {
                relayout(); layoutOverlay()
            }
            override fun componentMoved(e: ComponentEvent) {
                layoutOverlay()
            }
        })
        frame.addComponentListener(object : ComponentAdapter() {
            override fun componentMoved(e: ComponentEvent) = layoutOverlay()
            override fun componentResized(e: ComponentEvent) = layoutOverlay()
        })

        val contentPanel = ComposePanel().apply { setContent { content?.invoke() } }

        frame.contentPane.add(topBar, BorderLayout.NORTH)
        frame.contentPane.add(contentPanel, BorderLayout.CENTER)

        if (overlayActive && barOverlay != null) {
            frame.layeredPane.add(barOverlay, JLayeredPane.DRAG_LAYER)
        }

        frame.setSize(win.width, win.height)
        frame.setLocationRelativeTo(null)
        frame.isVisible = true

        layoutOverlay()
        SwingUtilities.invokeLater { relayout() }
    }

    private fun makeSlotPanel(backgroundColor: AwtColor) = ComposePanel().apply {
        isOpaque = true
        background = backgroundColor
    }

    private fun setPanelWidth(panel: ComposePanel, widthPx: Int, heightPx: Int, container: JComponent) {
        panel.preferredSize = Dimension(widthPx, heightPx)
        panel.minimumSize = Dimension(widthPx.coerceAtLeast(1), heightPx)
        panel.revalidate()
        container.revalidate()
        container.repaint()
    }
}

private fun AwtColor.toCompose(): Color = Color(red, green, blue, alpha)