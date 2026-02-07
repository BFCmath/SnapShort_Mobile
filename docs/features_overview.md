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
    - **Tap**: Triggers the **Edit & Crop** workflow (`EditScreenshotActivity`).

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

#### 5. Image Editing & Cropping
- **UI**: Full-screen editor with zoom and pan capabilities.
- **Cropping**:
    - **Interaction**: Drag standard "L-shaped" handles at the corners to define a crop area.
    - **Visual Feedback**: The area outside the selection is dimmed for focus.
- **Workflow**:
    - **Save**: Crops and replaces the original image (or saves to cache if file access is restricted).
    - **Delete**: Quick access to discard the image immediately.
    - **Reset**: A contextual "Close" button to remove the crop rectangle and start over.

#### 6. Task Management
- **UI**: A dedicated "My Tasks" screen listing all saved snaps as tasks.
- **Organization**:
    - **Filters**: Helper chips to view "All", "Active" (Note), or "Done" tasks.
    - **Visuals**: Tasks display the initial snapshot as a background with a gradient overlay for text readability.
- **Interaction**:
    - **Swipe Actions**:
        - **Swipe Right (Start-to-End)**: Mark task as **Done** (Green).
        - **Swipe Left (End-to-Start)**: **Delete** the task (Red).
    - **Tap**: Open specific task details.
    - **Empty State**: Friendly message when no tasks exist.

---

