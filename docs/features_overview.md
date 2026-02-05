# Features Overview

## Current Implementation Status
**As of:** 2026-02-06
**App State:** Early Refactoring / MVP

The application currently supports the core **Capture** and **Preview** workflows, but lacks the **Gallery** and **Editing** capabilities described in design documents.

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

---

### ❌ Missing / Not Yet Implemented
*These features are described in documentation but are not present in the current codebase.*

#### 1. Image Editor (`EditScreenshotActivity`)
- **Status**: The `PreviewActivity` has a placeholder `TODO` for launching this activity.
- **Missing**: No cropping UI, no task metadata entry (name, due date), no save/delete logic.

#### 2. Gallery / Snaps Tab (`MainActivity`)
- **Status**: The `MainActivity` currently displays a default "Hello Android!" scaffold.
- **Missing**:
    - Grid view of captured snapshots.
    - Filtering logic (Snap vs. Task).
    - Multi-select and delete functionality.

#### 3. Task Management Integration
- No database or logic to convert snapshots into tasks is currently implemented.
