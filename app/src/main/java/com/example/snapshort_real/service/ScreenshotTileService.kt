package com.example.snapshort_real.service

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import com.example.snapshort_real.R

class ScreenshotTileService : TileService() {

    override fun onClick() {
        super.onClick()

        if (isAccessibilityServiceEnabled()) {
            // Send broadcast to trigger screenshot
            android.util.Log.d("ScreenshotTileService", "Sending screenshot broadcast...")
            val intent = Intent(ScreenshotAccessibilityService.ACTION_TAKE_SCREENSHOT)
            intent.setPackage(packageName)
            sendBroadcast(intent)
            
            // Close the notification shade / quick settings panel
            // Ideally the service handles this, but TileService can also try.
            // Actually, we want the shade to close so we capture the screen content, not the shade.
            // The AccessibilityService performs GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE equivalent
            // or we can try to close it here if possible. 
            // TileService doesn't have a direct closeShade() method exposed easily for this intent 
            // without being an identifying activity or similar.
            // But standard behavior is to collapse when an action is fired if we want.
            // However, usually we rely on the AccessibilityService to do the cleanup/prep.
        } else {
            // Show a dialog to request the user to enable the service
            showDialog(getPermissionDialog())
        }
    }

    private fun getPermissionDialog(): android.app.AlertDialog {
        return android.app.AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("SnapShort requires Accessibility Service to take screenshots. Please enable it in Settings.")
            .setPositiveButton("Enable") { _, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                
                if (android.os.Build.VERSION.SDK_INT >= 34) {
                    val pendingIntent = android.app.PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    startActivityAndCollapse(pendingIntent)
                } else {
                    startActivityAndCollapse(intent)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_ACTIVE // Always active, just acts differently if service is off
        tile.label = getString(R.string.tile_label)
        tile.updateTile()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = android.content.ComponentName(this, ScreenshotAccessibilityService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val stringSplitter = android.text.TextUtils.SimpleStringSplitter(':')
        stringSplitter.setString(enabledServicesSetting)

        while (stringSplitter.hasNext()) {
            val componentNameString = stringSplitter.next()
            val enabledComponent = android.content.ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent != null && enabledComponent == expectedComponentName) {
                return true
            }
        }
        return false
    }
}
