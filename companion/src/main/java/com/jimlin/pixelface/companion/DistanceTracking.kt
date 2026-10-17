package com.jimlin.pixelface.companion

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveListenerConfig

private const val TAG = "PixelFaceDistance"

object DistanceTracking {
    fun hasPermission(context: Context) =
        context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    /** Registers for daily distance updates. Safe to call repeatedly. */
    fun register(context: Context) {
        if (!hasPermission(context)) return
        val config = PassiveListenerConfig.builder()
            .setDataTypes(setOf(DataType.DISTANCE_DAILY))
            .build()
        val future = HealthServices.getClient(context).passiveMonitoringClient
            .setPassiveListenerServiceAsync(DistancePassiveService::class.java, config)
        future.addListener({
            runCatching { future.get() }
                .onSuccess { Log.i(TAG, "Registered for daily distance") }
                .onFailure { Log.w(TAG, "Health Services registration failed", it) }
        }, context.mainExecutor)
    }
}

/** Health Services pushes batched daily distance here, even while the app is closed. */
class DistancePassiveService : PassiveListenerService() {
    override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
        val latest = dataPoints.getData(DataType.DISTANCE_DAILY)
            .maxByOrNull { it.endDurationFromBoot }
            ?: return
        Store.saveDistance(this, latest.value)
    }

    override fun onPermissionLost() {
        Log.w(TAG, "Activity recognition permission lost")
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) DistanceTracking.register(context)
    }
}
