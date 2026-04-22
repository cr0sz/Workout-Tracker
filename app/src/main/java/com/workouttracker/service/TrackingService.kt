package com.workouttracker.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.workouttracker.MainActivity
import com.workouttracker.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.osmdroid.util.GeoPoint
import kotlin.math.*

class TrackingService : Service() {

    companion object {
        private const val CHANNEL_ID = "gps_tracking"
        private const val NOTIFICATION_ID = 2001

        private val _isTracking = MutableStateFlow(false)
        val isTracking: StateFlow<Boolean> = _isTracking

        private val _routePoints = MutableStateFlow<List<GeoPoint>>(emptyList())
        val routePoints: StateFlow<List<GeoPoint>> = _routePoints

        private val _distanceKm = MutableStateFlow(0f)
        val distanceKm: StateFlow<Float> = _distanceKm

        private val _elapsedSeconds = MutableStateFlow(0L)
        val elapsedSeconds: StateFlow<Long> = _elapsedSeconds

        // Called after the user saves or discards the completed run
        fun reset() {
            _isTracking.value = false
            _routePoints.value = emptyList()
            _distanceKm.value = 0f
            _elapsedSeconds.value = 0L
        }
    }

    private lateinit var fusedLocation: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = MainScope()
    private var timerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocation = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> startTracking()
            "STOP"  -> stopTracking()
        }
        return START_NOT_STICKY
    }

    private fun startTracking() {
        _isTracking.value = true
        _routePoints.value = emptyList()
        _distanceKm.value = 0f
        _elapsedSeconds.value = 0L

        // Tick every second
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                _elapsedSeconds.value += 1
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val newPoint = GeoPoint(loc.latitude, loc.longitude)
                val current = _routePoints.value
                if (current.isNotEmpty()) {
                    _distanceKm.value += haversineKm(current.last(), newPoint)
                }
                _routePoints.value = current + newPoint
            }
        }

        fusedLocation.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    private fun stopTracking() {
        _isTracking.value = false
        timerJob?.cancel()
        if (::locationCallback.isInitialized) {
            fusedLocation.removeLocationUpdates(locationCallback)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "GPS Run Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Active while a run is being tracked" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tracking run")
            .setContentText("GPS run tracking is active")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun haversineKm(a: GeoPoint, b: GeoPoint): Float {
        val r = 6371.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val sinDLat = sin(dLat / 2)
        val sinDLon = sin(dLon / 2)
        val h = sinDLat * sinDLat +
                cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) *
                sinDLon * sinDLon
        return (2 * r * asin(sqrt(h))).toFloat()
    }
}
