# Feature: Preview & Edit

## 1. Overview
After a snapshot is taken, users are presented with a non-intrusive preview. They can choose to ignore it (auto-dismiss), dismiss it manually, or interact with it to edit and convert the snapshot into a structured task.

## 2. User Story
- **As a** user,
- **I want to** see a small preview of the screenshot I just took in the corner of my screen,
- **So that** I can decide whether to keep it, edit it, or discard it immediately.

- **As a** user,
- **I want to** tap the preview to open an editor,
- **So that** I can crop the image to the relevant area and add task details like a name and due date.

## 3. Current Implementation (Analysis)

### Preview Phase (`PreviewActivity`)
- **UI**: A transparent Activity displaying a `Card` in the bottom-left corner.
- **Auto-Dismiss**: The preview automatically slides off-screen to the left after **2.5 seconds** of inactivity.
- **Gestures**:
    - **Swipe Left**: Manually dismisses the preview immediately.
    - **Tap**: Opens the `EditScreenshotActivity` and closes the preview.
    - **Drag Right**: Has a resistance effect (elastic rubber banding) to indicate "no action" in that direction.

### Edit Phase (`EditScreenshotActivity`)
- **Image Editor**:
    - Uses a legacy `CropImageView` wrapped in an `AndroidView` within Jetpack Compose.
    - Allows the user to draw a rectangle to crop the image.
    - **Feature Request**: User wants to "drag a rectangle to crop quickly".
- **Metadata Entry**:
    - **Task Name**: Optional text field.
    - **Due Date**: Date picker dialog.
    - **Description**: Multiline text field.
- **Actions**:
    - **Delete**: Removes the file and closes the screen.
    - **Save**:
        - Crops the image (overwriting or creating new - currently creates a cropped copy in memory then saves).
        - Saves metadata to a local JSON repository.
        - **Logic**: If any metadata (Name, Date, Description) is entered, it is treated as a **Task**. If not, it is treated as a raw **Snap**.
        - **Navigation**: If saved as a Task, a flag is set to auto-navigate the main app to the "Tasks" tab.

## 4. Issues & Pain Points
- **Tech Debt**: Mixing View-based `CropImageView` with Jetpack Compose creates complexity in state management and touch handling.
- **Performance**: The image is re-loaded from disk in the Edit activity, which can be slow.
- **UX**:
    - The auto-dismiss timer might be too short or too long for some users.
    - Text fields might obscure the image on smaller screens.

## 5. Refactoring Requirements (Proposed)
- **Pure Compose Editor**: Investigate a native Compose library for image cropping to remove the legacy View dependency.
- **Shared Element Transition**: Animate the preview expanding into the edit screen for a seamless feel.
- **In-Place Editing**: Consider allowing simple edits (like quick crop) directly in the preview stage if possible, or make the transition instant.
- **Smart Fields**: Auto-suggest task names or dates based on image content (future AI scope? - *Keep in mind for later*).
