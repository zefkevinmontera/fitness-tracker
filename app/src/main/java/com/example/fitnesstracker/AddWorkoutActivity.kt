package com.example.fitnesstracker

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddWorkoutActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private lateinit var tvScreenTitle: TextView
    private lateinit var tilTitle: TextInputLayout
    private lateinit var tilDuration: TextInputLayout
    private lateinit var tilCalories: TextInputLayout
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDuration: TextInputEditText
    private lateinit var etCalories: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var progress: ProgressBar

    /** Non-null means we're editing an existing workout. */
    private var workoutId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Never show this screen to a signed-out user.
        if (auth.currentUser == null) {
            finish()
            return
        }

        setContentView(R.layout.activity_add_workout)

        tvScreenTitle = findViewById(R.id.tvScreenTitle)
        tilTitle = findViewById(R.id.tilWorkoutTitle)
        tilDuration = findViewById(R.id.tilDuration)
        tilCalories = findViewById(R.id.tilCalories)
        etTitle = findViewById(R.id.etWorkoutTitle)
        etDuration = findViewById(R.id.etDuration)
        etCalories = findViewById(R.id.etCalories)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        progress = findViewById(R.id.progress)

        // Edit mode: an "id" extra means we were opened from a workout item.
        workoutId = intent.getStringExtra(EXTRA_ID)?.takeIf { it.isNotBlank() }
        if (workoutId != null) {
            tvScreenTitle.text = "Edit workout"
            btnSave.text = "Update Workout"
            if (savedInstanceState == null) { // don't clobber edits after rotation
                etTitle.setText(intent.getStringExtra(EXTRA_TITLE).orEmpty())
                etDuration.setText(intent.getIntExtra(EXTRA_DURATION, 0).toString())
                etCalories.setText(intent.getIntExtra(EXTRA_CALORIES, 0).toString())
            }
        }

        btnSave.setOnClickListener { saveWorkout() }
        btnCancel.setOnClickListener { finish() }

        // Pressing "Done" on the keyboard also saves.
        etCalories.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveWorkout()
                true
            } else false
        }
    }

    private fun saveWorkout() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "You need to be signed in.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tilTitle.error = null
        tilDuration.error = null
        tilCalories.error = null

        val title = etTitle.text?.toString().orEmpty().trim()
        val duration = etDuration.text?.toString().orEmpty().trim().toIntOrNull()
        val calories = etCalories.text?.toString().orEmpty().trim().toIntOrNull()

        var valid = true
        if (title.isEmpty()) {
            tilTitle.error = "Enter a workout title"
            valid = false
        }
        if (duration == null || duration !in 1..1440) {
            tilDuration.error = "Enter minutes between 1 and 1440"
            valid = false
        }
        if (calories == null || calories !in 0..20000) {
            tilCalories.error = "Enter calories between 0 and 20000"
            valid = false
        }
        if (!valid || duration == null || calories == null) return

        setLoading(true)

        val id = workoutId
        if (id == null) addWorkout(uid, title, duration, calories)
        else updateWorkout(id, title, duration, calories)
    }

    private fun addWorkout(uid: String, title: String, duration: Int, calories: Int) {
        val workout = Workout(
            userId = uid,
            title = title,
            durationMinutes = duration,
            caloriesBurned = calories,
            timestamp = System.currentTimeMillis()
        )
        db.collection("workouts")
            .add(workout)
            .addOnSuccessListener {
                Toast.makeText(this, "Workout saved", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { onSaveFailed() }
    }

    private fun updateWorkout(id: String, title: String, duration: Int, calories: Int) {
        // Update only the editable fields; userId and timestamp stay untouched.
        val changes = mapOf(
            "title" to title,
            "durationMinutes" to duration,
            "caloriesBurned" to calories
        )
        db.collection("workouts").document(id)
            .update(changes)
            .addOnSuccessListener {
                Toast.makeText(this, "Workout updated", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { onSaveFailed() }
    }

    private fun onSaveFailed() {
        setLoading(false)
        Toast.makeText(this, "Couldn't save workout. Please try again.", Toast.LENGTH_LONG).show()
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnSave.isEnabled = !loading
        btnCancel.isEnabled = !loading
    }

    companion object {
        const val EXTRA_ID = "extra_workout_id"
        const val EXTRA_TITLE = "extra_workout_title"
        const val EXTRA_DURATION = "extra_workout_duration"
        const val EXTRA_CALORIES = "extra_workout_calories"
    }
}