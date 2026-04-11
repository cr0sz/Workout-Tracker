package com.workouttracker.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.workouttracker.R
import com.workouttracker.data.cloud.CloudData
import com.workouttracker.data.cloud.FirebaseRepository
import com.workouttracker.data.db.AppDatabase
import com.workouttracker.data.model.*
import com.workouttracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

enum class SyncState { IDLE, SYNCING, SUCCESS, ERROR }

data class SyncStatus(
    val state: SyncState = SyncState.IDLE,
    val lastSyncTime: Long? = null,
    val message: String = ""
)

class SyncViewModel(application: Application) : AndroidViewModel(application) {

    private val auth       = FirebaseAuth.getInstance()
    private val cloudRepo  = FirebaseRepository()
    private val localRepo  = WorkoutRepository(AppDatabase.getDatabase(application).workoutDao())

    private val _user = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    val isSignedIn get() = _user.value != null

    init {
        auth.addAuthStateListener { _user.value = it.currentUser }
    }

    // ── Google Sign-In ────────────────────────────────────────────────────────

    fun getGoogleSignInClient(context: Context): com.google.android.gms.auth.api.signin.GoogleSignInClient {
        val webClientId = context.getString(R.string.default_web_client_id)
        
        // Safety check to ensure the ID is not empty or a placeholder
        if (webClientId.isBlank() || !webClientId.contains(".apps.googleusercontent.com")) {
            throw IllegalStateException("Invalid Web Client ID: Check your secrets.xml or strings.xml")
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
            
        return GoogleSignIn.getClient(context, gso)
    }

    fun handleGoogleSignInResult(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                _user.value = auth.currentUser
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Sign-in failed")
            }
        }
    }

    fun signInWithEmail(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
                _user.value = auth.currentUser
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Sign-in failed")
            }
        }
    }

    fun createAccountWithEmail(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email.trim(), password).await()
                _user.value = auth.currentUser
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Account creation failed")
            }
        }
    }

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email.trim()).await()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to send reset email")
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _user.value = null
        _syncStatus.value = SyncStatus()
    }

    // ── Upload (phone → cloud) ────────────────────────────────────────────────

    fun uploadToCloud() {
        val uid = _user.value?.uid ?: return
        viewModelScope.launch {
            _syncStatus.value = SyncStatus(SyncState.SYNCING, message = "Uploading to cloud…")
            try {
                val workouts   = localRepo.getAllWorkoutsSync()
                val exercises  = localRepo.getAllExercisesSync()
                val sets       = localRepo.getAllSetsSync()
                val cardio     = localRepo.getAllCardioSync()
                val bodyweight = localRepo.getAllBodyweightSync()

                cloudRepo.uploadAll(uid, workouts, exercises, sets, cardio, bodyweight)

                _syncStatus.value = SyncStatus(
                    state        = SyncState.SUCCESS,
                    lastSyncTime = System.currentTimeMillis(),
                    message      = "Upload complete"
                )
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus(SyncState.ERROR, message = "Upload failed: ${e.message}")
            }
        }
    }

    // ── Restore (cloud → phone) ───────────────────────────────────────────────

    fun restoreFromCloud(onComplete: () -> Unit) {
        val uid = _user.value?.uid ?: return
        viewModelScope.launch {
            _syncStatus.value = SyncStatus(SyncState.SYNCING, message = "Restoring from cloud…")
            try {
                val data = cloudRepo.downloadAll(uid)

                // Write everything into local Room DB
                data.workouts.forEach   { localRepo.insertWorkout(it) }
                data.exercises.forEach  { localRepo.insertExercise(it) }
                data.sets.forEach       { localRepo.insertSet(it) }
                data.cardio.forEach     { localRepo.insertCardio(it) }
                data.bodyweight.forEach { localRepo.insertBodyweight(it) }

                _syncStatus.value = SyncStatus(
                    state        = SyncState.SUCCESS,
                    lastSyncTime = data.lastSync ?: System.currentTimeMillis(),
                    message      = "Restore complete — ${data.workouts.size} workouts restored"
                )
                onComplete()
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus(SyncState.ERROR, message = "Restore failed: ${e.message}")
            }
        }
    }

    // ── Fetch last sync time ──────────────────────────────────────────────────

    fun fetchLastSyncTime() {
        val uid = _user.value?.uid ?: return
        viewModelScope.launch {
            val t = cloudRepo.getLastSyncTime(uid)
            if (t != null) _syncStatus.value = _syncStatus.value.copy(lastSyncTime = t)
        }
    }
}

fun formatSyncTime(ms: Long): String {
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return sdf.format(Date(ms))
}
