package com.jimlin.pixelface.companion

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** One-time setup: grants the two permissions the complications need. */
class SetupActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var activityButton: Button
    private lateinit var notifButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (resources.displayMetrics.widthPixels * 0.12).toInt()
        status = TextView(this).apply { gravity = Gravity.CENTER; textSize = 14f }
        activityButton = Button(this).apply {
            text = "Allow distance"
            setOnClickListener { requestPermissions(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 1) }
        }
        notifButton = Button(this).apply {
            text = "Allow now playing"
            setOnClickListener { openNotificationAccess() }
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad, pad, pad)
            addView(TextView(context).apply {
                text = "PixelFace"; textSize = 18f; gravity = Gravity.CENTER
            })
            addView(status)
            addView(activityButton)
            addView(notifButton)
        }
        setContentView(ScrollView(this).apply { addView(column) })
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        render()
    }

    private fun render() {
        val distanceOk = DistanceTracking.hasPermission(this)
        val mediaOk = getSystemService(NotificationManager::class.java)
            .isNotificationListenerAccessGranted(ComponentName(this, MediaListenerService::class.java))
        if (distanceOk) DistanceTracking.register(this)
        activityButton.isEnabled = !distanceOk
        notifButton.isEnabled = !mediaOk
        status.text = buildString {
            append(if (distanceOk) "Distance: ready\n" else "Distance: needs permission\n")
            append(if (mediaOk) "Now playing: ready" else "Now playing: needs notification access")
        }
    }

    private fun openNotificationAccess() {
        val intents = listOf(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(this, MediaListenerService::class.java).flattenToString(),
            ),
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
        )
        for (intent in intents) {
            try {
                startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
            }
        }
        // Wear OS often has no notification-access screen. Grant it over adb instead.
        status.text = "No settings screen on this watch.\nGrant from your Mac with:\n" +
            "adb shell cmd notification allow_listener " +
            "com.jimlin.pixelface.companion/.MediaListenerService"
    }
}
