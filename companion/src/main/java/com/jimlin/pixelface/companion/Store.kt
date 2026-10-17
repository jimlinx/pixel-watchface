package com.jimlin.pixelface.companion

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import java.time.LocalDate

/** Small shared state between the background services and the complication providers. */
object Store {
    private const val PREFS = "pixelface"
    private const val KEY_DISTANCE_M = "distance_m"
    private const val KEY_DISTANCE_DAY = "distance_day"

    /** Today's distance in metres, or null if Health Services has not reported yet today. */
    fun distanceMetres(context: Context): Double? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_DISTANCE_DAY, null) != LocalDate.now().toString()) return null
        return prefs.getFloat(KEY_DISTANCE_M, 0f).toDouble()
    }

    fun saveDistance(context: Context, metres: Double) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat(KEY_DISTANCE_M, metres.toFloat())
            .putString(KEY_DISTANCE_DAY, LocalDate.now().toString())
            .apply()
        requestUpdate(context, DistanceComplicationService::class.java)
    }

    /** What is playing right now. Kept in memory: the listener service rebuilds it on connect. */
    data class NowPlaying(
        val title: String,
        val artist: String?,
        val isPlaying: Boolean,
        val tapIntent: PendingIntent?,
    )

    @Volatile
    var nowPlaying: NowPlaying? = null
        private set

    fun saveNowPlaying(context: Context, value: NowPlaying?) {
        if (value == nowPlaying) return
        nowPlaying = value
        requestUpdate(context, NowPlayingComplicationService::class.java)
    }

    private fun requestUpdate(context: Context, service: Class<*>) {
        ComplicationDataSourceUpdateRequester
            .create(context, ComponentName(context, service))
            .requestUpdateAll()
    }
}
