# Compose Windows
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

A tiny library to build a **Windows‑style title bar** in Compose Desktop while keeping **native window behaviors** (Snap, minimize, maximize, close). Provides a clean DSL: `window {}`, `titleBar {}`, and `content {}`.

> JVM 21 · Compose Multiplatform · Windows 10/11

---

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("fr.vyxs.compose.windows:compose-windows-core:0.1.0")
}
```

Requires:

* Kotlin 2.2+
* Compose 1.8+
* JVM toolchain 21

---

## Quick start

```kotlin
fun main() = WindowsApp {
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
        end {
            Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                Minimize { Icon(FeatherIcons.Minus, null, tint = Color(0xFFE6E6E6)) }
                Maximize { Icon(FeatherIcons.Square, null, tint = Color(0xFFE6E6E6)) }
                Close { Icon(FeatherIcons.X, null, tint = Color(0xFFFFEEEE)) }
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
```

---

## API overview

```kotlin
WindowsApp { /* WindowsAppScope */ }

class WindowsAppScope {
  fun window(block: WindowConfig.() -> Unit)
  fun titleBar(block: TitleBarConfig.() -> Unit)
  fun content(block: @Composable () -> Unit)
}

class WindowConfig {
  var title: String
  var width: Int; var height: Int
  var minWidth: Int?; var minHeight: Int?
  var resizable: Boolean
  var cornerRadius: Int
  var titleBarColor: Int
  var titleBarHeight: Int
  fun title(value: String)
  fun size(w: Int, h: Int)
  fun size(w: Dp, h: Dp)
  fun minSize(w: Int, h: Int)
  fun minSize(w: Dp, h: Dp)
  fun minWidth(px: Int); fun minHeight(px: Int)
  fun resizable(isResizable: Boolean)
  fun cornerRadius(radius: Dp)
  fun titleBarColor(color: Color)
  fun titleBarHeight(height: Dp)
}

class TitleBarConfig {
  fun start(block: @Composable TitleBarScope.() -> Unit)
  fun center(block: @Composable TitleBarScope.() -> Unit)
  fun end(block: @Composable TitleBarScope.() -> Unit)
}

class TitleBarScope(val titleBarColor: Color, val actions: WindowActions)
class WindowActions(val minimize: () -> Unit, val toggleMaximize: () -> Unit, val close: () -> Unit)
```

Utility composables for the title bar:

* `TitleBarScope.Minimize(...)`
* `TitleBarScope.Maximize(...)`
* `TitleBarScope.Close(...)`
* `TitleBarButton(...)` (generic)

Color helpers:

* `Color.darker(factor)` · `Color.lighter(factor)` · `Color.tintRed()` · `Color.grayscale()`

---

## Notes & limits

* Designed and tested on **Windows** (Snap support requires native window decorations via FlatLaf).
* Uses **FlatLaf** to enable custom window decorations while preserving OS behaviors.
* Works with Compose Desktop; Linux/macOS are not primary targets.
* ⚠️ Important: when you place composables inside `start {}`, `center {}`, or `end {}`, that area of the title bar **cannot be used to drag the window**. Currently only the empty space of the title bar is draggable.

### Future improvements

* Allowing drag even when elements like `center { Text("title") }` are present.
* Making the **entire window directly composable**, removing some Swing glue code.

---

## Development

Run the sample app:

```bash
./gradlew :sample:run
```

Publish locally:

```bash
./gradlew publishToMavenLocal
```

Release to Maven Central (example):

```bash
./gradlew publish
```

Provide standard `signing.*` and `ossrh*` properties via `gradle.properties` or environment variables.

---

## License

Apache-2.0 © Vyxs

**TL;DR**: You can freely use, modify, and redistribute this library (even commercially). You must keep the license notice, but there is **no warranty**: the library is provided *"as is"*. Contributors grant a patent license too, so you’re safe to use it in your projects.