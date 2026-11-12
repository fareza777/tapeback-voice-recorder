package com.f7developer.tapeback.widget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.f7developer.tapeback.MainActivity
import com.f7developer.tapeback.R
import com.f7developer.tapeback.TapeBackApp

/**
 * Quick Settings "REC" tile. Recording must start with the UI visible, so the tile opens the deck
 * with the record action instead of capturing in the background. The tile shows as active while
 * TapeBack is recording.
 */
class RecordTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val recording = (application as TapeBackApp).container.recordingController.state.value.isCapturing
        qsTile?.apply {
            state = if (recording) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = getString(R.string.tile_record)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = getString(if (recording) R.string.tile_recording else R.string.app_name)
            }
            updateTile()
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java)
            .setAction(MainActivity.ACTION_RECORD)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val launch = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(
                    PendingIntent.getActivity(this, REQUEST, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
                )
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
        if (isLocked) unlockAndRun { launch() } else launch()
    }

    private companion object {
        const val REQUEST = 41
    }
}
