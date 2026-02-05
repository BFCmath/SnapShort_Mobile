# Feature: Task Management

## 1. Overview
The **Task Management** (or "Tasks" tab) is where processed snapshots live. These are no longer just images; they are actionable items with associated work.

## 2. User Story
- **As a** user,
- **I want to** see a prioritized list of my tasks,
- **So that** I know what I need to do.

- **As a** user,
- **I want to** see the snapshot associated with the task,
- **So that** I have the context (the "what") immediately available.

- **As a** user,
- **I want to** mark a task as done,
- **So that** I can track my progress.

## 3. Current Implementation (Analysis)

### Logic (`MainActivity` - `Tab.Tasks` & `TasksScreen`)
- **Data Source**: READS the separate `metadata.json` file.
- **Filtering**: Shows only items where `isTask == true` (deduced from having a name, due date, or description).
- **Sorting**:
    1.  Incomplete tasks first.
    2.  Then by Due Date (earliest first).
    3.  Then by Creation Date (newest first).

### UI Representation
- **List Item**:
    - **Checkbox**: To toggle completion status.
    - **Thumbnail**: Small square showing the crop of the snapshot.
    - **Title**: The task name (or "Untitled Task").
    - **Date**: Color-coded (Red if overdue/near due).
    - **Icon**: Shows if there is a description.
- **Interaction**:
    - **Click**: Opens `EditScreenshotActivity` to view details or modify them.
    - **Check**: Updates the `isCompleted` flag in the JSON metadata.

## 4. Issues & Pain Points
- **Data Integrity**: Relying on a single JSON file for all metadata is fragile and not scalable.
- **Search/Sort**: Advanced sorting or searching is hard with the current flat list structure.
- **Performance**: As the list grows, parsing the entire JSON blob on every change is inefficient.

## 5. Refactoring Requirements (Proposed)
- **Room Database**: Essential for efficient querying, sorting (e.g., "Overdue", "Today"), and updates.
- **Rich Task Model**: Expand the data model to potentially include priorities, tags, or categories.
- **Contextual Actions**: Swipe actions on tasks (Delete, Edit, Share).
- **Notifications**: Local notifications when a due date is approaching.
