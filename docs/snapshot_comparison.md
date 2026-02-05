# Implementation Comparison: `snapshort` vs `snapshort_real`

This document outlines the key differences between the two project directories regarding the snapshot feature, specifically focusing on the implementation of capture, quick access, and the preview overlay.

## 1. Project Structure

| Feature | `snapshort` (Reference/Clean) | `snapshort_real` (Active/Flat) |
| :--- | :--- | :--- |
| **Package Organization** | Modular: Service logic in `.service`, UI in `.ui`, Data in `.data`. | Flat: Services and Activities mixed in the root package (`com.example.snapshort_real`). |

## 2. Snapshot Capture (AccessbilityService)

Both projects utilize an `AccessibilityService` to listen for a broadcast action and capture the screen using the `takeScreenshot` API (Android 11+) or `GlobalAction` (Android 9+).

**Key Differences:**
*   **Log & Comments**: `snapshort` contains more detailed comments and explicit handling/logging for `HardwareBuffer` resource management and color space handling.
*   **Logic**: `snapshort_real` has a slightly more concise implementation but follows the same core logic flow (Dismiss Notification Shade -> Delay -> Capture -> Save -> Launch Preview).

## 3. Preview Overlay ("Picture in Corner")

Both projects implement the "picture in the corner" using a transparent `PreviewActivity`, not a floating WindowManager overlay.

| Feature | `snapshort` (`.ui.PreviewActivity`) | `snapshort_real` (`PreviewActivity`) |
| :--- | :--- | :--- |
| **Auto-Disappear** | **2.5 seconds** delay. | **2.0 seconds** delay. |
| **Swipe Gesture** | **Refined**: Uses `detectHorizontalDragGestures`. Includes "resistance" when dragging right (allows slight overdrag) and clear threshold logic (`<-150f`). | **Basic**: Uses `draggable`. Only allows dragging left (`delta <= 0`). Harder cutoff (`<-200f`). |
| **Animation** | Animates out to `-screenWidthPx` (dynamic based on screen width). | Animates out to fixed `-1000f`. |
| **UI Size** | `120.dp` x `200.dp`. | `120.dp` width, `200.dp` height container. |

## 4. Quick Access (TileService)

Both implement a `ScreenshotTileService` that checks if the accessibility service is enabled and sends a broadcast (`ACTION_REQUEST_SCREENSHOT`/`ACTION_TAKE_SCREENSHOT`). The implementation is functionally identical.

## 5. Data Persistence (Repository)

| Feature | `snapshort` (`.data.ScreenshotRepository`) | `snapshort_real` (`SnapshotRepository`) |
| :--- | :--- | :--- |
| **File Format** | **PNG** (`Bitmap.Config.ARGB_8888`) | **JPEG** (Compressed) |
| **Directory** | `filesDir/screenshots` | `filesDir/snapshots` |
| **Naming** | `screenshot_{timestamp}.png` | `SNAP_{formatted_date}.jpg` |
| **Return Type** | Returns `File` object. | Returns `Uri` object. |
| **Functionality** | Includes helper methods for listing, copying, and deleting files. | Minimal implementation (save only). |

## Summary & Recommendation

*   **`snapshort`** appears to be the more robust and well-structured implementation. It features better code organization, more refined UI gestures (resistance, dynamic sizing), and a more capable repository (management features).
*   **`snapshort_real`** seems to be a flatter, slightly simplified version.

**Recommendation for "Detail Plan":**
To "handle the snapshot ability... step by step", it is recommended to **adopt the structure and logic from `snapshort`** into `snapshort_real` (or whichever is the target). Specifically:
1.  **Refactor Structure**: Move services and UI into their respective packages.
2.  **Enhance UI**: Use the `detectHorizontalDragGestures` logic from `snapshort` for a better user feel.
3.  **Standardize Data**: Decide on PNG vs JPEG (PNG is better for text/screenshots, JPEG for photos) and standardize the folder name.
