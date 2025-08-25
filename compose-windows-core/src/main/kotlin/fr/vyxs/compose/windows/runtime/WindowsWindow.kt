package fr.vyxs.compose.windows.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.Color
import com.formdev.flatlaf.FlatDarkLaf
import fr.vyxs.compose.windows.api.TitleBarScope
import fr.vyxs.compose.windows.api.WindowActions
import fr.vyxs.compose.windows.api.WindowConfig
import fr.vyxs.compose.windows.api.WindowsAppScope
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

/**
 * Hosts a Compose-driven custom title bar and content inside a Swing/FlatLaf window,
 * preserving native Windows behaviors (Snap, minimize, maximize, close).
 *
 * The [setup] Composable runs once in an invisible bootstrap composition so that
 * the top-level DSL can use standard Compose APIs (`remember`, `derivedStateOf`, etc.).
 * Slot lambdas capture that state and are then mounted into visible Compose roots.
 */
internal class WindowsWindow(
    private val win: WindowConfig,
    private val scope: WindowsAppScope,
    private val setup: @Composable WindowsAppScope.() -> Unit
) {
    private companion object { private const val PROVISIONAL_W = 4096 }

    private val lastWidths = mutableMapOf<ComposePanel, Int>()
    private var bound = false

    private lateinit var frame: JFrame
    private lateinit var topBar: JPanel
    private lateinit var startPanel: ComposePanel
    private lateinit var centerPanel: ComposePanel
    private lateinit var endPanel: ComposePanel
    private lateinit var contentPanel: ComposePanel
    private lateinit var titleBgAwt: AwtColor
    private var titleBg: Color = Color.Transparent
    private lateinit var barScope: TitleBarScope

    /** Creates and shows the window on the EDT. */
    fun show() {
        if (ensureEdt()) return
        setupLookAndFeel()
        initFrame()
        applyRootProperties()
        applyMinConstraints()
        initColors()
        initTopBar()
        initSlotPanels()
        initContentPanel()
        initActions()
        installListeners()
        installBootstrap()
        mountFrame()
        showFrame()
        postRelayout()
    }

    /** Returns true if call was rescheduled on EDT. */
    private fun ensureEdt(): Boolean {
        if (SwingUtilities.isEventDispatchThread()) return false
        SwingUtilities.invokeLater { show() }
        return true
    }

    /** Configures FlatLaf and global UI defaults. */
    private fun setupLookAndFeel() {
        System.setProperty("flatlaf.useWindowDecorations", "true")
        FlatDarkLaf.setup()
        UIManager.put("Component.arc", win.cornerRadius)
        UIManager.put("Button.arc", win.cornerRadius)
        UIManager.put("TextComponent.arc", win.cornerRadius)
        UIManager.put("TitlePane.unifiedBackground", true)
    }

    /** Builds the JFrame shell. */
    private fun initFrame() {
        frame = JFrame(win.title).apply {
            defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            layout = BorderLayout()
            isResizable = win.resizable
        }
    }

    /** Applies FlatLaf root pane flags and title bar height. */
    private fun applyRootProperties() {
        val root = frame.rootPane
        root.putClientProperty("FlatLaf.fullWindowContent", true)
        root.putClientProperty("JRootPane.titleBarShowIcon", false)
        root.putClientProperty("JRootPane.titleBarShowTitle", false)
        root.putClientProperty("JRootPane.titleBarShowIconify", false)
        root.putClientProperty("JRootPane.titleBarShowMaximize", false)
        root.putClientProperty("JRootPane.titleBarShowClose", false)
        root.putClientProperty("JRootPane.titleBarHeight", win.titleBarHeight)
    }

    /** Enforces minimum size if configured. */
    private fun applyMinConstraints() {
        val mw = win.minWidth
        val mh = win.minHeight
        if (mw != null || mh != null) {
            frame.minimumSize = Dimension((mw ?: 0).coerceAtLeast(0), (mh ?: 0).coerceAtLeast(0))
        }
    }

    /** Resolves title bar colors and applies them to the frame/root. */
    private fun initColors() {
        titleBgAwt = AwtColor(win.titleBarColor, (win.titleBarColor ushr 24) != 0)
        titleBg = titleBgAwt.toCompose()
        frame.background = titleBgAwt
        frame.rootPane.putClientProperty("JRootPane.titleBarBackground", titleBgAwt)
    }

    /** Creates the top bar container (null layout, height enforced by FlatLaf). */
    private fun initTopBar() {
        topBar = JPanel(null).apply {
            background = titleBgAwt
            isOpaque = true
            border = EmptyBorder(0, 0, 0, 0)
            preferredSize = Dimension(0, win.titleBarHeight)
        }
    }

    /** Creates title bar slot panels. */
    private fun initSlotPanels() {
        startPanel = createSlotPanel(titleBgAwt).also { topBar.add(it) }
        centerPanel = createSlotPanel(titleBgAwt).also { topBar.add(it) }
        endPanel = createSlotPanel(titleBgAwt).also { topBar.add(it) }
    }

    /** Creates the main content panel. */
    private fun initContentPanel() {
        contentPanel = ComposePanel()
    }

    /** Prepares window action callbacks and the [TitleBarScope]. */
    private fun initActions() {
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
        barScope = TitleBarScope(titleBg, actions)
    }

    /** Installs minimal listeners that re-run title bar layout on size changes. */
    private fun installListeners() {
        topBar.addComponentListener(object : ComponentAdapter() {
            override fun componentShown(e: ComponentEvent) = relayout()
            override fun componentResized(e: ComponentEvent) = relayout()
        })
        frame.addComponentListener(object : ComponentAdapter() {
            override fun componentMoved(e: ComponentEvent) {}
            override fun componentResized(e: ComponentEvent) {}
        })
    }

    /**
     * Creates an invisible bootstrap composition that executes the user DSL once,
     * then binds slot lambdas and content into visible roots.
     */
    private fun installBootstrap() {
        val bootstrap = ComposePanel().apply {
            isVisible = false
            size = Dimension(0, 0)
            setContent {
                with(scope) { setup() }
                SideEffect { bindIfNeeded() }
            }
        }
        frame.layeredPane.add(bootstrap, JLayeredPane.PALETTE_LAYER)
    }

    /** Binds title bar slots and content exactly once after DSL setup runs. */
    private fun bindIfNeeded() {
        if (bound) return
        bound = true
        scope.bar.startContent?.let { bindTitleSlot(startPanel, it) }
        scope.bar.centerContent?.let { bindTitleSlot(centerPanel, it) }
        scope.bar.endContent?.let { bindTitleSlot(endPanel, it) }
        contentPanel.setContent { scope.contentComposable?.invoke() }
    }

    /** Adds components to the frame. */
    private fun mountFrame() {
        frame.contentPane.add(topBar, BorderLayout.NORTH)
        frame.contentPane.add(contentPanel, BorderLayout.CENTER)
    }

    /** Sizes and shows the frame. */
    private fun showFrame() {
        frame.setSize(win.width, win.height)
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }

    /** Posts an initial layout pass to position slot panels. */
    private fun postRelayout() {
        SwingUtilities.invokeLater { relayout() }
    }

    /** Lays out the 3 slot panels inside the top bar based on measured widths. */
    private fun relayout() {
        val w = max(1, topBar.width)
        val h = win.titleBarHeight
        val pad = 0
        val startW = panelWidth(startPanel)
        val endW = panelWidth(endPanel)
        val centerW = panelWidth(centerPanel)
        startPanel.setBounds(pad, 0, startW, h)
        val endX = (w - endW - pad).coerceAtLeast(pad)
        endPanel.setBounds(endX, 0, endW, h)
        val maxX = w - centerW - pad
        val cx = if (maxX >= pad) ((w - centerW) / 2).coerceIn(pad, maxX) else pad
        centerPanel.setBounds(cx, 0, centerW, h)
    }

    /** Returns the last measured width for a slot panel, or a safe fallback. */
    private fun panelWidth(panel: ComposePanel): Int =
        (lastWidths[panel] ?: panel.preferredSize?.width ?: 0).coerceAtLeast(0)

    /** Wraps title bar slot content with a measuring backplate and mounts it. */
    private fun bindTitleSlot(panel: ComposePanel, slot: @Composable TitleBarScope.() -> Unit) {
        bindSlot(panel) {
            TitleSlotBackplate(
                bg = titleBg,
                contentChangedWidth = { w -> onMeasuredWidthAsync(panel, w) }
            ) { with(barScope) { slot() } }
        }
    }

    /** Sets minimum/preferred sizes and composes [content] into [panel]. */
    private fun bindSlot(panel: ComposePanel, content: @Composable () -> Unit) {
        val h = win.titleBarHeight
        panel.minimumSize = Dimension(1, h)
        panel.preferredSize = Dimension(PROVISIONAL_W, h)
        panel.setContent(content)
    }

    /** Schedules a width update for the given panel on the EDT. */
    private fun onMeasuredWidthAsync(panel: ComposePanel, w: Int) {
        SwingUtilities.invokeLater { onMeasuredWidth(panel, w) }
    }

    /** Records the measured width and relayouts if it changed. */
    private fun onMeasuredWidth(panel: ComposePanel, w: Int) {
        if (w <= 0) return
        val old = lastWidths[panel]
        if (old == w) return
        lastWidths[panel] = w
        setPanelWidth(panel, w, win.titleBarHeight, topBar)
        relayout()
    }

    /** Creates a Compose panel for a title bar slot. */
    private fun createSlotPanel(backgroundColor: AwtColor) = ComposePanel().apply {
        isOpaque = true
        background = backgroundColor
    }

    /** Applies exact width for a slot panel and refreshes its container. */
    private fun setPanelWidth(panel: ComposePanel, widthPx: Int, heightPx: Int, container: JComponent) {
        panel.preferredSize = Dimension(widthPx, heightPx)
        panel.minimumSize = Dimension(widthPx.coerceAtLeast(1), heightPx)
        panel.revalidate()
        container.revalidate()
        container.repaint()
    }
}

private fun AwtColor.toCompose(): Color = Color(red, green, blue, alpha)
