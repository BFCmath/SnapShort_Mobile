# Feature: Snapshot Capture

## 1. Overview
The **Snapshot Capture** feature is the core entry point of the application. It allows users to quickly capture what is currently on their screen without navigating away from their current context.

## 2. User Story
- **As a** user,
- **I want to** pull down my system status bar and tap a "Snap" button,
- **So that** I can capture the current screen content immediately.

## 3. Current Implementation (Analysis)
The current implementation relies on Android's **AccessibilityService** capabilities.

### Workflow:
1.  **Trigger**: 
    - User taps the **Quick Settings Tile** (`ScreenshotTileService`).
    - The tile checks if the `ScreenshotAccessibilityService` is enabled.
    - If disabled: Prompts user to enable it in Settings.
    - If enabled: Sends a broadcast/signal to the service.

2.  **Execution** (`ScreenshotAccessibilityService`):
    - **Dismiss Shade**: The service attempts to dismiss the notification shade/status bar to reveal the content behind it.
        - *Android 12+*: Uses `GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE`.
        - *Android <12*: Relies on system behavior or timing.
    - **Wait**: A hardcoded delay (e.g., 450ms) allows the animation to complete.
    - **Capture**: 
        - *Android 11+*: Uses `takeScreenshot()` API to get a hardware bitmap.
        - *Android <11*: Uses `GLOBAL_ACTION_TAKE_SCREENSHOT` (system default behavior).
    - **Save**: The bitmap is saved to the app's internal storage via `ScreenshotRepository`.
    - **Feedback**: A toast message confirms success/failure.

3.  **Post-Capture**:
    - The service immediately launches the **Preview Activity** passing the URI of the saved image.

## 4. Issues & Pain Points
- **Delay Reliability**: The 450ms delay is arbitrary. usage on different devices might result in capturing the notification shade partially closing.
- **Service Dependency**: Requires the user to manually enable an Accessibility Service, which is a high-friction setup step (though necessary for this approach).
- **Performance**: Saving the bitmap triggers disk I/O on the main/service thread context before launching the UI.

## 5. Refactoring Requirements (Proposed)
- **Floating Widget Option**: Consider adding a floating overlay button (using `SYSTEM_ALERT_WINDOW`) as an alternative trigger for users who don't want to use the notification shade.
- **Optimization**: Optimize the bitmap saving process to be purely asynchronous to launch the preview faster.
- **Feedback**: Replace Toasts with smoother UI feedback (e.g., a flash or haptic feedback).
