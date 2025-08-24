package fr.vyxs.compose.windows.runtime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged

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
