package com.jimlin.pixelface.companion

import android.app.PendingIntent
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
import java.util.Locale

/** Shows today's distance in kilometres. */
class DistanceComplicationService : SuspendingComplicationDataSourceService() {

    override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
        DistanceTracking.register(this)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val metres = if (DistanceTracking.hasPermission(this)) Store.distanceMetres(this) ?: 0.0 else null
        return build(request.complicationType, metres)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? = build(type, 5230.0)

    private fun build(type: ComplicationType, metres: Double?): ComplicationData? {
        val unit = "KM"
        // No permission yet: show a hint and open the setup screen on tap.
        val value = metres?.let { String.format(Locale.getDefault(), "%.1f", it / 1000.0) } ?: "!"
        val text = PlainComplicationText.Builder(value).build()
        val title = PlainComplicationText.Builder(unit).build()
        val description = PlainComplicationText.Builder("Distance today").build()
        val image = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_distance)).build()
        val tap = PendingIntent.getActivity(
            this, 1, Intent(this, SetupActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(text, description)
                .setTitle(title).setMonochromaticImage(image).setTapAction(tap).build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                PlainComplicationText.Builder("$value ${unit.lowercase()}").build(), description,
            ).setTitle(PlainComplicationText.Builder("Today").build())
                .setMonochromaticImage(image).setTapAction(tap).build()
            else -> null
        }
    }
}
