package fr.vyxs.compose.windows.runtime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged

/**
 * Intrinsic-width backplate used to measure slot content and relay its width
 * to the Swing layout so the title bar can be positioned precisely.
 */
@Composable
internal fun TitleSlotBackplate(
    bg: Color,
    contentChangedWidth: (Int) -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        Modifier
            .fillMaxHeight()
            .wrapContentWidth()
            .background(bg)
            .onSizeChanged { contentChangedWidth(it.width) }
    ) { content() }
}
