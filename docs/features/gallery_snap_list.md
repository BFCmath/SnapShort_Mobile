# Feature: Gallery & Snap List

## 1. Overview
The **Gallery** (or "Snaps" tab) serves as the inbox for all captured screenshots. It allows users to view their raw captures and decide their fate: convert them into tasks or delete them.

## 2. User Story
- **As a** user,
- **I want to** see a list of all my recent snapshots that I haven't assigned to a task yet,
- **So that** I can process them later if I didn't do it immediately after capturing.

- **As a** user,
- **I want to** tap on a snapshot in the list,
- **So that** I can edit it and turn it into a task (Assign Task).

## 3. Current Implementation (Analysis)

### Logic (`MainActivity` - `Tab.Snaps`)
- **Data Source**: READS all files in the app's internal storage directory.
- **Filtering**:
    - The app maintains a `ScreenshotMetadata` repository (JSON).
    - It filters the list of files against this metadata.
    - **Condition**: A file is shown in the "Snaps" tab IF:
        - It has NO corresponding metadata entry.
        - OR its metadata entry has NO task fields (Name, Due Date, and Description are all empty/null).
- **UI**:
    - Uses a `LazyVerticalGrid` (inside `GalleryScreen`).
    - **Refresh**: Supports "Pull to Refresh" to reload the file list from disk.

### Actions
- **Single Tap**: Opens the **Edit/Assign** screen (`EditScreenshotActivity`) for that image.
- **Multiselect**: (Implemented in `GalleryScreen`) allows selecting multiple images to delete them in bulk.
- **Delete**: Permanently removes the image file from internal storage.

## 4. Issues & Pain Points
- **Performance**: Reading the file system and filtering against a JSON list on the UI thread (or main coroutine scope) every time can become slow as the number of snapshots grows.
- **Synchronization**: If a user edits a snap and turns it into a task, the list needs to be refreshed manually or rely on `onResume` triggers which might be flaky.
- **Metadata separation**: Storing metadata in a separate JSON file from the image files poses a risk of desynchronization (orphan metadata or orphan files).

## 5. Refactoring Requirements (Proposed)
- **Database-centric Source of Truth**: Use a local database (Room) to track all snapshots. When a file is created, meaningful metadata (path, date) should be inserted into the DB.
- **Live Data**: The UI should observe the database (via Flow) so that changes (converting Snap -> Task) automatically remove items from this list without manual refresh.
- **Async Loading**: Image loading should be efficiently cached and handled (Coil is doing this, but the file listing itself needs to be async).
