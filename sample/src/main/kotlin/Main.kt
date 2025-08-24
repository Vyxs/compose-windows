import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

fun main() = WindowsApp {
    sample1()
}

fun WindowsAppScope.sampleMinimal() {
    window {
        title("Sample 1")
        size(900, 600)
        resizable(true)
        cornerRadius(2.dp)
        titleBarColor(Color(0x202020))
        titleBarHeight(40.dp)
    }

    titleBar {
        end {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Minimize { Icon(FeatherIcons.Minus, "Minimize", tint = Color(0xFFE6E6E6)) }
                Maximize { Icon(FeatherIcons.Square, "Maximize", tint = Color(0xFFE6E6E6)) }
                Close { Icon(FeatherIcons.X, "Close", tint = Color(0xFFFFEEEE)) }
            }
        }
    }

    content {
        MaterialTheme {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Hello World", color = Color.Black)
            }
        }
    }
}

fun WindowsAppScope.sample1() {
    window {
        title("Windows App")
        size(900, 600)
        minSize(400, 400)
        resizable(true)
        cornerRadius(2.dp)
        titleBarColor(Color(0x202020))
        titleBarHeight(40.dp)
    }

    titleBar {
        start {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(FeatherIcons.Home, null, tint = Color(0xFFE6E6E6))
                Spacer(Modifier.width(8.dp))
                Text("Window with working Snap !", color = Color(0xFFE6E6E6), fontSize = 14.sp)
            }
        }

        center {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Windows App", color = Color(0xFFE6E6E6), fontSize = 14.sp)
            }
        }

        end {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TitleBarButton(
                    width = 130.dp,
                    backgroundColor = titleBarColor,
                    onClick = {
                        this@end.actions.close()
                    }
                ) {
                    Text("MyCustomButton", color = Color(0xFFE6E6E6), fontSize = 16.sp)
                }
                Minimize(
                    beforeAction = { println("Minimize called !")}
                ) {
                    Icon(FeatherIcons.Minus, "Minimize", tint = Color(0xFFE6E6E6))
                }
                Maximize(
                    beforeAction = { println("Maximize called !")}
                ) {
                    Icon(FeatherIcons.Square, "Maximize", tint = Color(0xFFE6E6E6))
                }
                Close(
                    beforeAction = { println("Close called !")}
                ) {
                    Icon(FeatherIcons.X, "Close", tint = Color(0xFFFFEEEE))
                }
            }
        }
    }

    content {
        MaterialTheme {
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Hello World", color = Color.Black)
                }
            }
        }
    }
}