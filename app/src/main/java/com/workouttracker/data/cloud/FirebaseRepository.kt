package com.workouttracker.data.cloud

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.workouttracker.data.model.*
import kotlinx.coroutines.tasks.await

/**
 * All data lives under:
 *   users/{uid}/workouts/{date}
 *   users/{uid}/workout_exercises/{id}
 *   users/{uid}/exercise_sets/{id}
 *   users/{uid}/cardio/{id}
 *   users/{uid}/bodyweight/{date}
 *
 * Security rule (paste into Firebase Console → Firestore → Rules):
 *
 *   rules_version = '2';
 *   service cloud.firestore {
 *     match /databases/{database}/documents {
 *       match /users/{userId}/{document=**} {
 *         allow read, write: if request.auth != null && request.auth.uid == userId;
 *       }
 *     }
 *   }
 *
 * This means ONLY the authenticated user can read or write their own data.
 * No other user, no admin, nobody can access it from the client.
 */
class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun userRoot(uid: String) = db.collection("users").document(uid)
    private fun col(uid: String, name: String) = userRoot(uid).collection(name)

    // ── Upload (local → cloud) ────────────────────────────────────────────────

    suspend fun uploadAll(
        uid: String,
        workouts: List<Workout>,
        exercises: List<WorkoutExercise>,
        sets: List<ExerciseSet>,
        cardio: List<CardioSession>,
        bodyweight: List<BodyweightEntry>
    ) {
        // Firestore batch writes are limited to 500 ops — chunk if needed
        val batches = mutableListOf<com.google.firebase.firestore.WriteBatch>()
        var current = db.batch()
        var opCount = 0

        fun addOp(ref: com.google.firebase.firestore.DocumentReference, data: Map<String, Any?>) {
            if (opCount >= 490) {
                batches.add(current)
                current = db.batch()
                opCount = 0
            }
            current.set(ref, data, SetOptions.merge())
            opCount++
        }

        workouts.forEach { w ->
            addOp(col(uid, "workouts").document(w.date),
                mapOf("date" to w.date, "notes" to w.notes))
        }
        exercises.forEach { e ->
            addOp(col(uid, "workout_exercises").document(e.id.toString()),
                mapOf("id" to e.id, "workoutDate" to e.workoutDate,
                    "exerciseName" to e.exerciseName, "orderIndex" to e.orderIndex))
        }
        sets.forEach { s ->
            addOp(col(uid, "exercise_sets").document(s.id.toString()),
                mapOf("id" to s.id, "exerciseId" to s.exerciseId, "setNumber" to s.setNumber,
                    "reps" to s.reps, "weight" to s.weight, "isBodyweight" to s.isBodyweight))
        }
        cardio.forEach { c ->
            addOp(col(uid, "cardio").document(c.id.toString()),
                mapOf("id" to c.id, "date" to c.date, "type" to c.type,
                    "distanceKm" to c.distanceKm, "durationMinutes" to c.durationMinutes,
                    "weightKg" to c.weightKg, "calories" to c.calories, "notes" to c.notes))
        }
        bodyweight.forEach { b ->
            addOp(col(uid, "bodyweight").document(b.date),
                mapOf("date" to b.date, "weightKg" to b.weightKg))
        }

        batches.add(current)
        batches.forEach { it.commit().await() }

        // Update last sync timestamp
        userRoot(uid).set(mapOf("lastSync" to System.currentTimeMillis()), SetOptions.merge()).await()
    }

    // ── Download (cloud → local structs) ─────────────────────────────────────

    suspend fun downloadAll(uid: String): CloudData {
        val workoutDocs   = col(uid, "workouts").get().await()
        val exerciseDocs  = col(uid, "workout_exercises").get().await()
        val setDocs       = col(uid, "exercise_sets").get().await()
        val cardioDocs    = col(uid, "cardio").get().await()
        val bwDocs        = col(uid, "bodyweight").get().await()

        val workouts = workoutDocs.documents.mapNotNull { doc ->
            val date = doc.getString("date") ?: return@mapNotNull null
            Workout(date = date, notes = doc.getString("notes") ?: "")
        }
        val exercises = exerciseDocs.documents.mapNotNull { doc ->
            WorkoutExercise(
                id          = (doc.getLong("id") ?: 0),
                workoutDate = doc.getString("workoutDate") ?: return@mapNotNull null,
                exerciseName= doc.getString("exerciseName") ?: return@mapNotNull null,
                orderIndex  = (doc.getLong("orderIndex") ?: 0).toInt()
            )
        }
        val sets = setDocs.documents.mapNotNull { doc ->
            ExerciseSet(
                id          = (doc.getLong("id") ?: 0),
                exerciseId  = (doc.getLong("exerciseId") ?: return@mapNotNull null),
                setNumber   = (doc.getLong("setNumber") ?: 0).toInt(),
                reps        = (doc.getLong("reps") ?: 0).toInt(),
                weight      = (doc.getDouble("weight") ?: 0.0).toFloat(),
                isBodyweight= doc.getBoolean("isBodyweight") ?: false
            )
        }
        val cardio = cardioDocs.documents.mapNotNull { doc ->
            CardioSession(
                id              = (doc.getLong("id") ?: 0),
                date            = doc.getString("date") ?: return@mapNotNull null,
                type            = doc.getString("type") ?: "Walk",
                distanceKm      = doc.getDouble("distanceKm")?.toFloat(),
                durationMinutes = doc.getLong("durationMinutes")?.toInt(),
                weightKg        = doc.getDouble("weightKg")?.toFloat(),
                calories        = doc.getLong("calories")?.toInt(),
                notes           = doc.getString("notes") ?: ""
            )
        }
        val bw = bwDocs.documents.mapNotNull { doc ->
            BodyweightEntry(
                date    = doc.getString("date") ?: return@mapNotNull null,
                weightKg= (doc.getDouble("weightKg") ?: return@mapNotNull null).toFloat()
            )
        }

        val lastSync = userRoot(uid).get().await().getLong("lastSync")

        return CloudData(workouts, exercises, sets, cardio, bw, lastSync)
    }

    suspend fun getLastSyncTime(uid: String): Long? =
        userRoot(uid).get().await().getLong("lastSync")
}

data class CloudData(
    val workouts: List<Workout>,
    val exercises: List<WorkoutExercise>,
    val sets: List<ExerciseSet>,
    val cardio: List<CardioSession>,
    val bodyweight: List<BodyweightEntry>,
    val lastSync: Long?
)
