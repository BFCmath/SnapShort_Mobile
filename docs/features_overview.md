# Features Overview

## Current Implementation Status
**As of:** 2026-02-06
**App State:** Core Features Implemented

The application currently supports the **Capture**, **Preview**, **Gallery**, and **Detail View** workflows. Basic file management (delete) is also implemented.

### ✅ Implemented Features

#### 1. Snapshot Capture
- **Trigger**: Quick Settings Tile (`ScreenshotTileService`).
- **Mechanism**: Uses `AccessibilityService` (`ScreenshotAccessibilityService`) to:
    - Dismiss the Notification Shade/Status Bar.
    - Wait for the closing animation.
    - Capture the screen content.
- **Support**:
    - *Android 12+*: Uses `GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE`.
    - *Android 11+*: Uses `takeScreenshot()` API for direct hardware bitmap capture.
    - *Legacy*: Fallback to `GLOBAL_ACTION_TAKE_SCREENSHOT`.

#### 2. Immediate Preview
- **UI**: A floating, non-intrusive card appears in the bottom-left corner (`PreviewActivity`).
- **Interaction**:
    - **Auto-Dismiss**: Disappears automatically after 2.5 seconds of inactivity.
    - **Swipe Left**: Manually dismisses the preview.
    - **Elastic Drag**: "Rubber-banding" effect when dragging right (indicating no action).
    - **Tap**: Currently configured to trigger an "Edit" action (see Missing Features).

#### 3. Gallery & Management
- **UI**: Grid view of all captured screenshots (`GalleryScreen`).
- **Data Source**: Real-time observation of the app's internal storage `screenshots/` directory using `FileObserver` in `ScreenshotRepository`.
- **Interaction**:
    - **View**: Tap a thumbnail to open the Detail View.
    - **Navigation**: Uses Jetpack Navigation Compose.

#### 4. Detail View
- **UI**: Full-screen image viewer (`DetailScreen`) with a black background.
- **Interaction**:
    - **Delete**: Remove the image permanently via the trash icon.
    - **Back**: Return to the gallery.

---

