package com.jimlin.pixelface.companion

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

/** Shows the current track title (text) and artist (title). Tap opens the media controls. */
class NowPlayingComplicationService : SuspendingComplicationDataSourceService() {

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? =
        build(request.complicationType, Store.nowPlaying)

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        build(type, Store.NowPlaying("Song title", "Artist", isPlaying = true, tapIntent = null))

    private fun build(type: ComplicationType, np: Store.NowPlaying?): ComplicationData? {
        val (main, sub) = when {
            !hasNotificationAccess() -> "Tap to enable" to "Now playing"
            np == null -> "Nothing playing" to null
            np.isPlaying -> np.title to np.artist
            else -> np.title to listOfNotNull("Paused", np.artist).joinToString(" · ")
        }
        val tap = if (hasNotificationAccess()) mediaControls() ?: np?.tapIntent else null
        val tapAction = tap ?: PendingIntent.getActivity(
            this, 2, Intent(this, SetupActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val description = PlainComplicationText.Builder("Now playing").build()
        val image = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_music)).build()
        return when (type) {
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                PlainComplicationText.Builder(main).build(), description,
            ).apply { sub?.let { setTitle(PlainComplicationText.Builder(it).build()) } }
                .setMonochromaticImage(image).setTapAction(tapAction).build()
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                PlainComplicationText.Builder(main).build(), description,
            ).setMonochromaticImage(image).setTapAction(tapAction).build()
            else -> null
        }
    }

    /** The Wear OS media controls screen, the same one the watch face's music button opens. */
    private fun mediaControls(): PendingIntent? {
        val intent = Intent(MEDIA_CONTROLS_ACTION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(packageManager) == null) return null
        return PendingIntent.getActivity(this, 3, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun hasNotificationAccess(): Boolean {
        val nm = getSystemService(android.app.NotificationManager::class.java)
        return nm.isNotificationListenerAccessGranted(ComponentName(this, MediaListenerService::class.java))
    }
}

private const val MEDIA_CONTROLS_ACTION = "com.google.android.wearable.action.MEDIA_CONTROLS"
