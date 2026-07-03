# DailyAutomator

A reusable Java infrastructure library for automating daily tasks in mobile/gacha games on PC.

## Overview

DailyAutomator provides a foundation of platform-level automation primitives:

- **ScreenCapture** â€?Foreground screen capture using `java.awt.Robot`
- **MouseController** â€?Foreground mouse simulation (click, drag, scroll)
- **KeyboardController** â€?Dual-mode keyboard: Win32 `PostMessage` for background (no focus steal) + `java.awt.Robot` fallback
- **WindowFinder** â€?Native window lookup via JNA / `user32.dll`
- **TemplateMatcher** â€?OpenCV-based image template matching (`matchTemplate`)
- **MainApp** â€?JavaFX GUI for configuration, status, and log monitoring

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Windows (JNA/PostMessage + OpenCV native bindings are platform-specific)

### Build

```bash
mvn clean package
```

### Run (GUI)

```bash
mvn javafx:run
```

Or:

```bash
java -jar target/daily-automator-1.0.0.jar
```

### Run (CLI Demo)

```bash
java -jar target/daily-automator-1.0.0.jar --cli
```

### Run Tests

```bash
mvn test
```

## Project Structure

```
Project_Zongse/
â”œâ”€â”€ pom.xml
â”œâ”€â”€ src/
â”?  â”œâ”€â”€ main/java/com/dailyautomator/
â”?  â”?  â”œâ”€â”€ core/
â”?  â”?  â”?  â”œâ”€â”€ ScreenCapture.java
â”?  â”?  â”?  â”œâ”€â”€ MouseController.java
â”?  â”?  â”?  â”œâ”€â”€ KeyboardController.java
â”?  â”?  â”?  â”œâ”€â”€ WindowFinder.java
â”?  â”?  â”?  â””â”€â”€ TemplateMatcher.java
â”?  â”?  â”œâ”€â”€ gui/
â”?  â”?  â”?  â””â”€â”€ MainApp.java
â”?  â”?  â””â”€â”€ DailyAutomator.java
â”?  â””â”€â”€ test/java/com/dailyautomator/core/
â”?      â”œâ”€â”€ KeyboardControllerTest.java
â”?      â”œâ”€â”€ MouseControllerTest.java
â”?      â”œâ”€â”€ ScreenCaptureTest.java
â”?      â”œâ”€â”€ TemplateMatcherTest.java
â”?      â””â”€â”€ WindowFinderTest.java
â””â”€â”€ README.md
```

## Module Details

### ScreenCapture

Full-screen, region, and monitor-specific capture using `java.awt.Robot`. Supports saving to disk in PNG, JPG, or other formats.

### MouseController

Foreground mouse operations: move, left/right/double click, drag with smooth interpolation, and scroll.

### KeyboardController

Dual-mode keyboard simulation:
- **BACKGROUND (default)** â€?Sends keystrokes via Win32 `PostMessage`. Does not steal focus.
- **FOREGROUND** â€?Falls back to `java.awt.Robot` for scenarios where background input is insufficient.

Combination keys follow strict order: press modifiers â†?press key â†?release key â†?release modifiers.

### WindowFinder

Static utility for finding, inspecting, and monitoring native Windows windows using JNA/`user32.dll`.

- `findByTitle` / `findAllByTitle` â€?Search visible windows by title keyword
- `getWindowRect` â€?Get bounding rectangle in screen coordinates
- `waitForWindow` â€?Poll until a matching window appears

### TemplateMatcher

OpenCV-powered template matching using `Imgproc.matchTemplate()` (default `TM_CCOEFF_NORMED`).

- Match templates on full-screen or region captures
- Multi-match support with configurable confidence threshold
- TM_SQDIFF/SQDIFF_NORMED results are automatically converted so higher = better

### GUI (MainApp)

JavaFX-based graphical interface with:

- Target window search and validation
- Daily task checkboxes (pluggable for future game-specific tasks)
- Confidence threshold slider (0.6â€?.0)
- Real-time status, progress bar, and scrolling log
- Start/Stop/View Screenshot/Clear Log controls

## Design Decisions

1. **Background keyboard + foreground mouse**: Keyboard via `PostMessage` avoids focus stealing; mouse via `Robot` for reliable simulation.
2. **Dual-mode keyboard**: BACKGROUND (PostMessage) by default, FOREGROUND (Robot) as fallback.
3. **TemplateMatcher as infrastructure**: No game-specific logic â€?generic image matching only.
4. **GUI layered above core**: `MainApp` imports from `core/` but does not modify it.
5. **CLI via `--cli` flag**: `java -jar daily-automator.jar` starts GUI; `--cli` starts interactive demo.


## ?? Safety Warning

**Running tests will physically move your mouse and send keystrokes.**

The test suite uses java.awt.Robot to simulate mouse and keyboard input. When you run mvn test, the following tests execute on your actual desktop:

- MouseControllerTest ¡ª moves the mouse, clicks, drags, scrolls
- KeyboardControllerTest ¡ª sends keystrokes, types text, triggers keyboard shortcuts
- ScreenCaptureTest ¡ª takes screenshots of your screen
- TemplateMatcherTest ¡ª uses Robot for screen capture

**These tests are disabled by default.** To enable them, pass:
`
mvn test -Ddailyautomator.enableRobotTests=true
`

Before enabling, make sure no critical windows are in focus and save your work.

### Which tests are safe?

| Test class | Robot-free? | Runs by default? |
|---|---|---|
| WindowFinderTest | Yes (JNA only) | Yes |
| KeyboardControllerTest | No | No |
| MouseControllerTest | No | No |
| ScreenCaptureTest | No | No |
| TemplateMatcherTest | No | No |

## License

MIT

