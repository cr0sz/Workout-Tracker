package com.workouttracker.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workouttracker.data.model.CARDIO_TYPES
import com.workouttracker.data.model.CardioSession
import com.workouttracker.service.TrackingService
import com.workouttracker.ui.viewmodel.WorkoutViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackingScreen(
    viewModel: WorkoutViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Collect live state from the service
    val isTracking by TrackingService.isTracking.collectAsStateWithLifecycle()
    val routePoints by TrackingService.routePoints.collectAsStateWithLifecycle()
    val distanceKm by TrackingService.distanceKm.collectAsStateWithLifecycle()
    val elapsedSeconds by TrackingService.elapsedSeconds.collectAsStateWithLifecycle()

    // Permission state
    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    var permissionsGranted by remember {
        mutableStateOf(
            locationPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
    }

    // Track when the user has stopped a run (isTracking flipped false after being true)
    var trackingCompleted by remember { mutableStateOf(false) }
    var prevWasTracking by remember { mutableStateOf(false) }
    LaunchedEffect(isTracking) {
        if (prevWasTracking && !isTracking) trackingCompleted = true
        prevWasTracking = isTracking
    }

    var showSaveDialog by remember { mutableStateOf(false) }

    // OSMDroid MapView reference + overlays (created once)
    val mapViewRef          = remember { mutableStateOf<MapView?>(null) }
    val polyline            = remember { Polyline() }
    val myLocationOverlay   = remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    // Center the map — prefer the overlay's live position, fall back to FusedLocation
    fun centerOnCurrentLocation() {
        if (!permissionsGranted) return
        myLocationOverlay.value?.myLocation?.let { geoPoint ->
            mapViewRef.value?.controller?.animateTo(geoPoint)
            return
        }
        LocationServices.getFusedLocationProviderClient(context).lastLocation
            .addOnSuccessListener { loc ->
                loc ?: return@addOnSuccessListener
                mapViewRef.value?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
                mapViewRef.value?.controller?.setZoom(17.0)
            }
    }

    // Center map on the user's position when the screen first opens
    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) centerOnCurrentLocation()
    }

    // Update the polyline on the map whenever route changes
    LaunchedEffect(routePoints) {
        mapViewRef.value?.let { map ->
            polyline.setPoints(routePoints)
            if (routePoints.isNotEmpty()) {
                map.controller.animateTo(routePoints.last())
            }
            map.invalidate()
        }
    }

    // Format elapsed time
    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val secs = elapsedSeconds % 60
    val timeString = if (hours > 0) "%d:%02d:%02d".format(hours, minutes, secs)
                     else "%d:%02d".format(minutes, secs)

    // Format pace
    val paceString = if (distanceKm > 0.01f && elapsedSeconds > 0) {
        val minPerKm = (elapsedSeconds / 60f) / distanceKm
        val paceMin = minPerKm.toInt()
        val paceSec = ((minPerKm - paceMin) * 60).toInt()
        "%d:%02d /km".format(paceMin, paceSec)
    } else {
        "--:-- /km"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Track Run", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { if (!isTracking) onBack() },
                        enabled = !isTracking
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Map ──────────────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(17.0)
                            polyline.outlinePaint.apply {
                                color = 0xFFE53935.toInt()   // Material Red 600
                                strokeWidth = 12f
                            }
                            overlays.add(polyline)
                            // Blue dot showing the user's current position + direction arrow
                            val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                            locationOverlay.enableMyLocation()
                            overlays.add(locationOverlay)
                            myLocationOverlay.value = locationOverlay
                            mapViewRef.value = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Locate-me button (bottom-end of the map)
                if (permissionsGranted) {
                    SmallFloatingActionButton(
                        onClick = { centerOnCurrentLocation() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "My location",
                            modifier = Modifier.size(20.dp))
                    }
                }

                // Overlay: no-permission notice
                if (!permissionsGranted) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOff,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Location permission needed to track your run",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = { permissionLauncher.launch(locationPermissions) }) {
                                Text("Grant permission")
                            }
                        }
                    }
                }
            }

            // ── Stats row ─────────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TrackingStat(label = "Time", value = timeString)
                    VerticalDivider(modifier = Modifier.height(40.dp))
                    TrackingStat(label = "Distance", value = "%.2f km".format(distanceKm))
                    VerticalDivider(modifier = Modifier.height(40.dp))
                    TrackingStat(label = "Pace", value = paceString)
                }
            }

            // ── Action buttons ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    !permissionsGranted -> {
                        // handled by the map overlay above
                    }
                    trackingCompleted -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = {
                                    TrackingService.reset()
                                    onBack()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null,
                                    modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Discard")
                            }
                            Button(
                                onClick = { showSaveDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null,
                                    modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Save Run", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    isTracking -> {
                        Button(
                            onClick = {
                                context.startService(
                                    Intent(context, TrackingService::class.java)
                                        .apply { action = "STOP" }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Stop", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    else -> {
                        Button(
                            onClick = {
                                context.startForegroundService(
                                    Intent(context, TrackingService::class.java)
                                        .apply { action = "START" }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Start Run", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }

    // Manage MapView lifecycle (resume/pause/detach with the composable)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapViewRef.value?.onResume()
                    myLocationOverlay.value?.enableMyLocation()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapViewRef.value?.onPause()
                    myLocationOverlay.value?.disableMyLocation()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef.value?.onDetach()
        }
    }

    // Save dialog
    if (showSaveDialog) {
        SaveRunDialog(
            distanceKm = distanceKm,
            elapsedSeconds = elapsedSeconds,
            onDismiss = { showSaveDialog = false },
            onSave = { type, name, notes ->
                val routeJson = routePoints.joinToString(";") { "${it.latitude},${it.longitude}" }
                viewModel.addCardioSession(
                    CardioSession(
                        date            = viewModel.todayDateString(),
                        type            = type,
                        name            = name,
                        distanceKm      = distanceKm.takeIf { it > 0.01f },
                        durationMinutes = (elapsedSeconds / 60L).toInt().takeIf { it > 0 },
                        notes           = notes.trim(),
                        routeJson       = routeJson
                    )
                )
                TrackingService.reset()
                showSaveDialog = false
                onBack()
            }
        )
    }
}

// ── Stat column ───────────────────────────────────────────────────────────────

@Composable
private fun TrackingStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Save dialog ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveRunDialog(
    distanceKm: Float,
    elapsedSeconds: Long,
    onDismiss: () -> Unit,
    onSave: (type: String, name: String, notes: String) -> Unit
) {
    var selectedType by remember { mutableStateOf("Run") }
    var sessionName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    val durationMin = (elapsedSeconds / 60L).toInt()
    val paceStr = if (distanceKm > 0.01f && durationMin > 0) {
        val minPerKm = durationMin / distanceKm
        val m = minPerKm.toInt()
        val s = ((minPerKm - m) * 60).toInt()
        "%d:%02d /km".format(m, s)
    } else null

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Save Run",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Summary chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("%.2f km".format(distanceKm))
                    StatChip("$durationMin min")
                    paceStr?.let { StatChip(it) }
                }

                // Activity type dropdown
                ExposedDropdownMenuBox(
                    expanded = typeMenuExpanded,
                    onExpandedChange = { typeMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Activity type") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeMenuExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false }
                    ) {
                        CARDIO_TYPES.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedType = type
                                    typeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Session name
                OutlinedTextField(
                    value = sessionName,
                    onValueChange = { sessionName = it },
                    label = { Text("Name (optional)") },
                    placeholder = { Text("e.g. Morning run, Park loop…") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Cancel") }

                    Button(
                        onClick = { onSave(selectedType, sessionName.trim(), notes) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Save", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
