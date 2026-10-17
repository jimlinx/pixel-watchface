package com.jimlin.pixelface.companion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState

/**
 * Debug-only. Starts or stops a fake media session:
 *   adb shell am broadcast -a com.jimlin.pixelface.FAKE_MEDIA -p com.jimlin.pixelface.companion --es title "Song" --es artist "Artist"
 *   adb shell am broadcast -a com.jimlin.pixelface.FAKE_MEDIA -p com.jimlin.pixelface.companion --ez stop true
 */
class FakeMediaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        session?.release()
        session = null
        if (intent.getBooleanExtra("stop", false)) return
        session = MediaSession(context.applicationContext, "PixelFaceFake").apply {
            setMetadata(
                MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, intent.getStringExtra("title") ?: "Test Song")
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, intent.getStringExtra("artist") ?: "Test Artist")
                    .build(),
            )
            setPlaybackState(
                PlaybackState.Builder()
                    .setState(PlaybackState.STATE_PLAYING, 0, 1f)
                    .setActions(PlaybackState.ACTION_PLAY_PAUSE)
                    .build(),
            )
            isActive = true
        }
    }

    companion object {
        private var session: MediaSession? = null
    }
}
