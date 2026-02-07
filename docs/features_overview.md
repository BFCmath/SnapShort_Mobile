# Features Overview

## Current Implementation Status
**As of:** 2026-02-08
**App State:** Core Features + AI Integration + Task Management

The application currently supports the **Capture**, **Preview**, **Gallery**, **Unified Detail View**, and **AI Task Extraction** workflows. Advanced file management (bulk delete) and persistent task tracking are also implemented.

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
- **Smart Filtering**: Displays *only* raw snapshots (files that haven't been converted to tasks yet).
- **Optimization**: Uses image downsampling (300x300 thumbnails) and `Set`-based selection lookup for high performance.
- **Data Source**: Combines real-time file observation (`ScreenshotRepository`) with database records (`TaskRepository`) to filter the view.
- **Interaction**:
    - **View**: Tap a thumbnail to open the Unified Detail View.
    - **Navigation**: Uses Jetpack Navigation Compose.

#### 4. Unified Detail View (Snap & Task)
- **UI**: Full-screen viewer (`TaskDetailScreen`) that adapts context (New Snap vs. Existing Task).
- **Capabilities**:
    - **View**: Full-resolution image zooming and panning.
    - **Task Conversion**: Simply adding a Title or Due Date promotes a generic "Snap" to a tracked "Task".
    - **Task Demotion**: Clearing the title/date removes the task record but keeps the image aka "Demote to Snap".
    - **AI Integration**: "Star" button triggers Gemini to analyze the image and suggest task details.
- **Interaction**:
    - **Swipe Navigation**:
        - **Left/Right**: Navigate between images in the current list (Gallery or Task context).
        - **Down**: Dismiss/Go Back.
        - **Up**: Focus the "Task Name" field for quick editing.
    - **Delete**: 
        - For Snaps: Deletes the image file.
        - For Tasks: Deletes both the task record and the image file.

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
- **Persistence**: Powered by **Room Database** for robust local storage.
- **Organization**:
    - **Filters**: Helper chips to view "All", "Active" (Note), or "Done" tasks.
    - **Visuals**: Tasks display the initial snapshot as a background with a gradient overlay for text readability.
- **Interaction**:
    - **Swipe Actions**:
        - **Swipe Right (Start-to-End)**: Mark task as **Done** (Green) or **Active**.
        - **Swipe Left (End-to-Start)**: **Delete** the task and its image (Red).
    - **Clear Done**: Button to bulk delete all completed tasks and their images.
    - **Tap**: Open specific task details.
    - **Empty State**: Friendly message when no tasks exist.

#### 7. UX Improvements
- **Text Navigation**:
    - **Trigger**: Pressing "Enter" (or "Next") in the **Task Name** field.
    - **Action**: Automatically moves focus to the **Description** field for seamless typing.
    - **Scope**: Applied to Edit Screen, Task Detail Screen, and Gallery Detail View.
- **Keyboard Handling**: Tapping outside text fields dismisses the keyboard and clears focus.
- **Visuals**: Enhanced text shadows and gradients for better readability on varied image backgrounds.

#### 8. Gallery Multi-Select & Bulk Actions
- **Selection Mode**:
    - **Trigger**: Long-press on any image in the gallery.
    - **Action**: Enters selection mode with visual indicators (checkboxes/borders).
- **Interaction**:
    - **Tap**: Toggle selection for individual images.
    - **Counter**: Top bar updates to show the number of selected items.
- **Bulk Delete**:
    - **Action**: Tap the "Delete" icon in the top bar to remove all selected images at once.
    - **Cleanup**: Automatically exits selection mode after deletion or when cleared.

#### 9. AI Task Extraction (Gemini)
- **Engine**: Google Gemini 1.5 Flash.
- **Trigger**: "Star" icon in Unified Detail View.
- **Capability**: Analyzes the image to automatically suggest:
    - **Task Name**: Short title.
    - **Description**: Brief summary.
    - **Due Date**: Infers specific dates from text (e.g., "tomorrow", "next Friday").
- **UX**: 
    - Shows a loading indicator during analysis.
    - Presents a dialog with editable suggested fields before applying to the task.
