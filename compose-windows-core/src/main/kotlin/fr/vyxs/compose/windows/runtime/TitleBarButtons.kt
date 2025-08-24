package fr.vyxs.compose.windows.runtime

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.vyxs.compose.windows.api.TitleBarScope
import androidx.compose.foundation.background

/**
 * Generic title bar button with hover/pressed feedback.
 *
 * @param width Button width.
 * @param height Button height.
 * @param backgroundColor Base background color.
 * @param hoverBackgroundColor Background color on hover.
 * @param pressedBackgroundColor Background color when pressed.
 * @param onClick Optional click handler.
 * @param content Button content.
 */
@Composable
fun TitleBarButton(
    width: Dp = 40.dp,
    height: Dp = 40.dp,
    backgroundColor: Color,
    hoverBackgroundColor: Color = backgroundColor.lighter(0.10f),
    pressedBackgroundColor: Color = backgroundColor.lighter(0.18f),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val fill = when {
        pressed -> pressedBackgroundColor
        hovered -> hoverBackgroundColor
        else -> backgroundColor
    }
    Box(
        modifier = Modifier
            .size(width, height)
            .background(fill)
            .hoverable(interaction)
            .pointerInput(onClick) { detectTapGestures(onTap = { onClick?.invoke() }) },
        contentAlignment = Alignment.Center
    ) { content() }
}

/**
 * A minimize button bound to the current window via [TitleBarScope.actions].
 *
 * @param backgroundColor Base background color.
 * @param hoverBackgroundColor Background color on hover.
 * @param pressedBackgroundColor Background color when pressed.
 * @param beforeAction Optional callback executed before minimizing.
 * @param content Composable content placed inside the button.
 */
@Composable
fun TitleBarScope.Minimize(
    backgroundColor: Color = titleBarColor,
    hoverBackgroundColor: Color = titleBarColor.lighter(0.10f),
    pressedBackgroundColor: Color = titleBarColor.lighter(0.18f),
    beforeAction: (() -> Unit)? = null,
    content: @Composable () -> Unit
) = TitleBarButton(
    backgroundColor = backgroundColor,
    hoverBackgroundColor = hoverBackgroundColor,
    pressedBackgroundColor = pressedBackgroundColor,
    onClick = { beforeAction?.invoke(); actions.minimize() },
    content = content
)

/**
 * A maximize/restore button bound to the current window via [TitleBarScope.actions].
 *
 * @param backgroundColor Base background color.
 * @param hoverBackgroundColor Background color on hover.
 * @param pressedBackgroundColor Background color when pressed.
 * @param beforeAction Optional callback executed before toggling maximize.
 * @param content Composable content placed inside the button.
 */
@Composable
fun TitleBarScope.Maximize(
    backgroundColor: Color = titleBarColor,
    hoverBackgroundColor: Color = titleBarColor.lighter(0.10f),
    pressedBackgroundColor: Color = titleBarColor.lighter(0.18f),
    beforeAction: (() -> Unit)? = null,
    content: @Composable () -> Unit
) = TitleBarButton(
    backgroundColor = backgroundColor,
    hoverBackgroundColor = hoverBackgroundColor,
    pressedBackgroundColor = pressedBackgroundColor,
    onClick = { beforeAction?.invoke(); actions.toggleMaximize() },
    content = content
)

/**
 * A close button bound to the current window via [TitleBarScope.actions].
 *
 * @param backgroundColor Base background color.
 * @param hoverBackgroundColor Background color on hover. Defaults to a red-tinted variant.
 * @param pressedBackgroundColor Background color when pressed. Defaults to a red-tinted variant.
 * @param beforeAction Optional callback executed before closing.
 * @param content Composable content placed inside the button.
 */
@Composable
fun TitleBarScope.Close(
    backgroundColor: Color = titleBarColor,
    hoverBackgroundColor: Color = titleBarColor.tintRed().lighter(0.10f),
    pressedBackgroundColor: Color = titleBarColor.tintRed().lighter(0.18f),
    beforeAction: (() -> Unit)? = null,
    content: @Composable () -> Unit
) = TitleBarButton(
    backgroundColor = backgroundColor,
    hoverBackgroundColor = hoverBackgroundColor,
    pressedBackgroundColor = pressedBackgroundColor,
    onClick = { beforeAction?.invoke(); actions.close() },
    content = content
)