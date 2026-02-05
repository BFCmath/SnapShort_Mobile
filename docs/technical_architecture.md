# Technical Architecture

## Core Components Structure

### 1. Service Layer (Capture)
The capture logic is split between a Tile interaction and a persistent Accessibility Service.

- **`ScreenshotTileService`**
    - **Role**: Entry point via Quick Settings Panel.
    - **Action**: Checks if Accessibility Service is enabled.
        - *If Yes*: Broadcasts `ACTION_TAKE_SCREENSHOT`.
        - *If No*: Shows a dialog prompting the user to enable the service in Android Settings.

- **`ScreenshotAccessibilityService`**
    - **Role**: Handles system-level interactions and background processing.
    - **Flow**:
        1.  Receives `ACTION_TAKE_SCREENSHOT` broadcast.
        2.  Dismisses Notification Shade (`GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE`).
        3.  Waits `450ms` (hardcoded) for shade animation.
        4.  Calls `takeScreenshot()` (Android 11 API).
        5.  Converts hardware buffer to software bitmap.
        6.  Saves image to internal storage using `ScreenshotRepository`.
        7.  Launches `PreviewActivity`.

### 2. UI Layer (Presentation)

- **`PreviewActivity`**
    - **Role**: Transient, overlay-style activity to show the captured image.
    - **Theme**: Transparent, excludes from Recents (`android:excludeFromRecents="true"`).
    - **Implementation**: Jetpack Compose (`PreviewScreen`).
    - **Animations**:
        - Uses `Animatable` offset for slide-in/out effects.
        - Timer-based auto-dismiss (2.5s).
    - **Navigation**:
        - Explicit intent to `EditScreenshotActivity` (Currently TODO).

- **`MainActivity`**
    - **Role**: Application Dashboard / Gallery.
    - **Current State**: Scaffold with "Hello World" (Implementation Pending).

### 3. Data Layer

- **`ScreenshotRepository`**
    - **Role**: Handles file I/O operations for saving bitmap data to local storage.

## Key Technical Decisions

- **Why Accessibility Service?**
    - Required to utilize `takeScreenshot()` API globally without specific app permissions or root.
    - Required to programmatically dismiss the notification shade to ensure a clean capture.

- **Why Quick Settings Tile?**
    - Provides instant access from any screen without leaving the context.

- **Hardware vs. Software Bitmaps**
    - The `takeScreenshot()` API returns a `HardwareBuffer`. This must be copied to a software `Bitmap.Config.ARGB_8888` before it can be saved to a file, which is an expensive operation performed on `Dispatchers.IO`.

## Current Limitations / TODOs

1.  **Delay Reliability**: The 450ms delay for shade dismissal is hardcoded and may be flaky on some devices.
2.  **Missing Edit Flow**: The link between Preview and Edit is broken (`TODO` in `PreviewActivity`).
3.  **App Shell**: The main app interface content is missing.
