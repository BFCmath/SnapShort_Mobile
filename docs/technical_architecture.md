# Technical Architecture

## Architecture Overview
The application follows modern Android development practices, utilizing **MVVM (Model-View-ViewModel)** architecture and **Hilt** for dependency injection.

### Core Technologies
- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose
- **Dependency Injection**: Hilt
- **Persistence**: Room Database (SQLite abstraction)
- **Navigation**: Jetpack Navigation Compose
- **Async Processing**: Kotlin Coroutines & Flow

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
        - Launches `EditScreenshotActivity` via `ActivityResultLauncher`.

- **`EditScreenshotActivity` / `EditScreenshotScreen`**
    - **Role**: Dedicated screen for modifying the captured image.
    - **Features**:
        - **Zoom/Pan**: Handled via `pointerInput` and `graphicsLayer` transformations.
        - **Cropping Logic**: Custom implementation using `Canvas` for the overlay and `Bitmap.createBitmap` for the final operation.
        - **Dimmed Overlay**: Uses `Path.combine` with `PathOperation.Difference` to dim non-selected areas.
    - **Data Flow**: Receives `IMAGE_URI` via intent, saves changes back to the same URI or a new cache file.

- **`MainActivity`**
    - **Role**: Single Activity entry point.
    - **Implementation**: Hosts the `NavHost`.
    - **Navigation Graph**:
        - `gallery`: Shows `GalleryScreen`.
        - `tasks`: Shows `TasksScreen`.
        - `task_detail`: Shows `TaskDetailScreen`.
        - **Unified Navigation**: Both Gallery items and Tasks navigate to `task_detail`, passing either an `imagePath` (new snap) or `taskId` (existing task).

- **`GalleryScreen`**
    - **Role**: Displays a grid of available *snapshots* (unprocessed images).
    - **ViewModel**: `GalleryViewModel`
        - Exposes `GalleryUiState` (Loading, Success, Empty).
        - **Smart Filtering**: Combines `ScreenshotRepository` (files) and `TaskRepository` (DB) streams. Images that exist in the DB are *hidden* from the Gallery to prevent duplication.
        - **Performance**: Downsamples images to 300x300 thumbnails using Coil.
    - **Multi-Select**: Maintains a `Set<File>` state for batch deletion.

- **`TaskDetailScreen`** (Unified Detail View)
    - **Role**: Versatile screen for viewing images, creating tasks, and editing task details.
    - **Modes**:
        - **Snapshot Mode**: Triggered by passing `imagePath`. View image, delete file, or "Save" to convert to Task.
        - **Task Mode**: Triggered by passing `taskId`. View full details, edit metadata, mark complete, or delete.
    - **AI Integration**: Triggers `GeminiRepository` to fill task fields.

- **`TasksScreen`**
    - **Role**: List view for managing all saved tasks.
    - **ViewModel**: `TaskViewModel`
        - **State**: Exposes `tasks` (List<Task>) and `filterType` (All/Active/Done).
        - **Logic**: Combines repository data with filter state.
    - **Interactions**:
        - **Swipe-to-Dismiss**:
            - Left-to-Right: Toggle Done/Active.
            - Right-to-Left: Delete (File + Record).
        - **Filter Chips**: Toggle between task states.



### 3. Data Layer

- **`ScreenshotRepository`**
    - **Role**: Source of truth for *raw image files*.
    - **Key Mechanism**:
        - Uses `FileObserver` to watch `screenshots/` directory.
        - Exposes a `Flow<List<File>>` reflecting the file system state.

- **`TaskRepository`** & **`AppDatabase`** (Room)
    - **Role**: Source of truth for *Task metadata*.
    - **Components**:
        - **`Task` Entity**: Stores `id`, `imagePath`, `title`, `description`, `dueDate`, `isCompleted`.
        - **`TaskDao`**: Handles SQL operations (`SELECT`, `INSERT`, `UPDATE`, `DELETE`).
        - **Repository**: Mediator exposing `Flow<List<Task>>` to ViewModels.
    - **Deletion Logic**:
        - **Task Deletion**: Removes DB record. ViewModels typically orchestrate deleting the associated file via `ScreenshotRepository` as well.

- **`GeminiRepository`**
    - **Role**: Handles interaction with Google Gemini AI API.
    - **Model**: `gemini-1.5-flash`.
    - **Input**: Bitmap (Screenshot).
    - **Output**: JSON containing `task_name`, `description`, and `due_date`.
    - **Key Mechanism**:
        - Encodes image and prompt.
        - Parses JSON response to `TaskSuggestion` data object.

## Key Technical Decisions

- **Why Accessibility Service?**
    - Required to utilize `takeScreenshot()` API globally without specific app permissions or root.
    - Required to programmatically dismiss the notification shade to ensure a clean capture.

- **Why Quick Settings Tile?**
    - Provides instant access from any screen without leaving the context.

- **Hardware vs. Software Bitmaps**
    - The `takeScreenshot()` API returns a `HardwareBuffer`. This must be copied to a software `Bitmap.Config.ARGB_8888` before it can be saved to a file, which is an expensive operation performed on `Dispatchers.IO`.

- **Database-File Synchronization**
    - **Problem**: Images are files, Tasks are DB records.
    - **Solution**: The `GalleryViewModel` observes *both* sources. It filters out files that are referenced in the `Task` table. This creates a clear workflow: Capture -> Gallery -> (Convert) -> Tasks. An image is either a "Raw Snap" (Gallery) or a "Task" (Task List), never both.

- **AI Integration Strategy**
    - **Gemini Flash**: Selected for low latency.
    - **Structured Output**: Returns JSON to ensure reliable parsing into app domain objects.
    - **User-in-the-Loop**: AI suggestions are presented in a dialog for review/editing, never auto-applied, ensuring accuracy.

## Current Limitations / TODOs

1.  **Delay Reliability**: The 450ms delay for shade dismissal is hardcoded.
2.  **API Key Security**: API Key is currently hardcoded.
3.  **File Consistency**: If a file is deleted externally, the DB record might become orphaned (needs robust sync/cleanup).
