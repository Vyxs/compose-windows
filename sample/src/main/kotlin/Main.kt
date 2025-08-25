import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.*
import fr.vyxs.compose.windows.api.WindowsApp
import fr.vyxs.compose.windows.api.WindowsAppScope
import fr.vyxs.compose.windows.runtime.Close
import fr.vyxs.compose.windows.runtime.Maximize
import fr.vyxs.compose.windows.runtime.Minimize
import fr.vyxs.compose.windows.runtime.TitleBarButton

/**
 * Minimal Windows-style demo:
 * - Custom PlusButton in the title bar (uses PhaserIcons.PlusCircle)
 * - Native Minimize / Maximize / Close controls
 * - Shared state between title bar and content
 */
fun main() = WindowsApp({
    title("Compose Windows — Demo")
    size(1000, 680)
    resizable(true)
    cornerRadius(6.dp)
    titleBarColor(Color(0xFF202020))
    titleBarHeight(40.dp)
}) {
    DemoApp()
}

@Composable
private fun WindowsAppScope.DemoApp() {
    var counter by remember { mutableStateOf(0) }

    titleBar {
        start {
            Row(
                Modifier.fillMaxHeight().padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(FeatherIcons.Monitor, contentDescription = "App", tint = Color(0xFFE6E6E6))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Compose Windows",
                    color = Color(0xFFE6E6E6),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        end {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlusButton(
                    onClick = { counter++ },
                )
                Minimize { Icon(FeatherIcons.Minus, "Minimize", tint = Color(0xFFE6E6E6)) }
                Maximize { Icon(FeatherIcons.Square, "Maximize", tint = Color(0xFFE6E6E6)) }
                Close { Icon(FeatherIcons.X, "Close", tint = Color(0xFFFFEEEE)) }
            }
        }
    }

    content {
        MaterialTheme {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF7F7F7))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text("Hello World !", fontSize = 28.sp, color = Color(0xFF222222), fontWeight = FontWeight.Bold)
                Text(
                    "This window uses a custom title bar and keeps native Windows features (Snap, Min/Max/Close).",
                    fontSize = 14.sp,
                    color = Color(0xFF444444)
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { counter++ }) {
                        Icon(FeatherIcons.PlusCircle, contentDescription = "Increment")
                        Spacer(Modifier.width(8.dp))
                        Text("Increment")
                    }
                    Button(onClick = { counter = 0 }) {
                        Icon(FeatherIcons.RotateCcw, contentDescription = "Reset")
                        Spacer(Modifier.width(8.dp))
                        Text("Reset")
                    }
                    Text("Counter: $counter", fontSize = 16.sp, color = Color(0xFF222222))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Try dragging the title bar and snapping the window to edges or corners.",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

/**
 * Custom "+" action for the title bar using FeatherIcons.PlusCircle.
 * Keeps the Windows title bar color for a native look-and-feel.
 */
@Composable
private fun fr.vyxs.compose.windows.api.TitleBarScope.PlusButton(
    onClick: () -> Unit,
    background: Color = titleBarColor
) {
    TitleBarButton(
        backgroundColor = background,
        onClick = onClick
    ) {
        Icon(FeatherIcons.PlusCircle, contentDescription = "Plus", tint = Color(0xFFE6E6E6))
    }
}
