package com.jimlin.pixelface.companion

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

private const val TAG = "PixelFaceMedia"

/**
 * Bound by the system once notification access is granted. Watches active media sessions
 * and keeps [Store.nowPlaying] up to date.
 *
 * Source order:
 *  1. MediaSession on the watch (watch apps, and phone media if Wear OS mirrors it as a session).
 *  2. A media-style notification (covers players that only post a notification).
 */
class MediaListenerService : NotificationListenerService() {

    private lateinit var sessionManager: MediaSessionManager
    private val controllers = mutableMapOf<MediaSession.Token, MediaController>()

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        watch(list.orEmpty())
    }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
        override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
        override fun onSessionDestroyed() = refresh()
    }

    override fun onListenerConnected() {
        sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val me = ComponentName(this, MediaListenerService::class.java)
        try {
            sessionManager.addOnActiveSessionsChangedListener(sessionsListener, me)
            watch(sessionManager.getActiveSessions(me))
        } catch (e: SecurityException) {
            Log.w(TAG, "No notification access yet", e)
        }
    }

    override fun onListenerDisconnected() {
        if (::sessionManager.isInitialized) {
            sessionManager.removeOnActiveSessionsChangedListener(sessionsListener)
        }
        controllers.values.forEach { it.unregisterCallback(callback) }
        controllers.clear()
        Store.saveNowPlaying(this, null)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun watch(list: List<MediaController>) {
        val tokens = list.map { it.sessionToken }.toSet()
        controllers.keys.filter { it !in tokens }.forEach { controllers.remove(it)?.unregisterCallback(callback) }
        list.filter { it.sessionToken !in controllers }.forEach {
            it.registerCallback(callback)
            controllers[it.sessionToken] = it
        }
        refresh()
    }

    private fun refresh() {
        Store.saveNowPlaying(this, fromSessions() ?: fromNotifications())
    }

    private fun fromSessions(): Store.NowPlaying? {
        val candidates = controllers.values.filter { !it.metadata.title().isNullOrBlank() }
        val pick = candidates.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: candidates.firstOrNull()
            ?: return null
        val meta = pick.metadata!!
        return Store.NowPlaying(
            title = meta.title()!!,
            artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
            isPlaying = pick.playbackState?.state == PlaybackState.STATE_PLAYING,
            tapIntent = pick.sessionActivity ?: launchIntent(pick.packageName),
        )
    }

    private fun fromNotifications(): Store.NowPlaying? {
        val sbn = runCatching { activeNotifications }.getOrNull().orEmpty().firstOrNull { it.isMedia() }
            ?: return null
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return null
        return Store.NowPlaying(
            title = title,
            artist = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            isPlaying = true,
            tapIntent = sbn.notification.contentIntent ?: launchIntent(sbn.packageName),
        )
    }

    private fun StatusBarNotification.isMedia(): Boolean {
        val extras = notification.extras
        return extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
            extras.getString(Notification.EXTRA_TEMPLATE)?.contains("MediaStyle") == true ||
            notification.category == Notification.CATEGORY_TRANSPORT
    }

    private fun MediaMetadata?.title(): String? =
        this?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: this?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)

    private fun launchIntent(pkg: String): PendingIntent? =
        packageManager.getLaunchIntentForPackage(pkg)?.let {
            PendingIntent.getActivity(this, pkg.hashCode(), it, PendingIntent.FLAG_IMMUTABLE)
        }
}
