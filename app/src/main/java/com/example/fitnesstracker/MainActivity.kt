package com.example.fitnesstracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.NumberFormat
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class MainActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val adapter = WorkoutAdapter()
    private var workoutsListener: ListenerRegistration? = null

    private lateinit var tvWelcome: TextView
    private lateinit var tvTotalCalories: TextView
    private lateinit var tvTotalMinutes: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Gate before showing any protected UI.
        if (auth.currentUser == null) {
            goToLogin()
            return
        }

        setContentView(R.layout.activity_main)

        tvWelcome = findViewById(R.id.tvWelcome)
        tvTotalCalories = findViewById(R.id.tvTotalCalories)
        tvTotalMinutes = findViewById(R.id.tvTotalMinutes)

        auth.currentUser?.displayName?.takeIf { it.isNotBlank() }?.let {
            tvWelcome.text = "Hi, $it"
        }

        findViewById<RecyclerView>(R.id.rvWorkouts).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        // --- SWIPE TO DELETE LOGIC ---
        val swipeToDelete = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false // drag-to-reorder not needed

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val workout = adapter.currentList.getOrNull(position)

                // Nothing valid to delete: snap the row back.
                if (workout == null || workout.id.isBlank()) {
                    if (position != RecyclerView.NO_POSITION) adapter.notifyItemChanged(position)
                    return
                }

                db.collection("workouts").document(workout.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this@MainActivity, "Workout deleted", Toast.LENGTH_SHORT).show()
                        // No UI work needed: the snapshot listener removes the row.
                    }
                    .addOnFailureListener {
                        // Bring the swiped row back, since the data didn't change.
                        val index = adapter.currentList.indexOf(workout)
                        if (index >= 0) adapter.notifyItemChanged(index)
                        Toast.makeText(this@MainActivity, "Couldn't delete workout", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        ItemTouchHelper(swipeToDelete).attachToRecyclerView(findViewById(R.id.rvWorkouts))
        // -----------------------------

        findViewById<ExtendedFloatingActionButton>(R.id.fabAddWorkout).setOnClickListener {
            startActivity(Intent(this, AddWorkoutActivity::class.java))
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            stopListening() // avoid a permission-denied callback after sign-out
            auth.signOut()
            goToLogin()
        }

        updateSummary(emptyList())
    }

    override fun onStart() {
        super.onStart()
        if (isFinishing) return
        val uid = auth.currentUser?.uid
        if (uid == null) {
            goToLogin()
            return
        }
        startListening(uid)
    }

    override fun onStop() {
        super.onStop()
        stopListening()
    }

    private fun startListening(uid: String) {
        stopListening() // never stack two listeners

        workoutsListener = db.collection("workouts")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Workout listener failed", error)
                    Toast.makeText(this, "Couldn't load workouts.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val workouts = snapshot?.documents
                    ?.mapNotNull { doc ->
                        // Copy the Firestore document ID into Workout.id
                        doc.toObject(Workout::class.java)?.copy(id = doc.id)
                    }
                    .orEmpty()
                    // Sorted here so no composite Firestore index is required.
                    .sortedByDescending { it.timestamp }

                adapter.submitList(workouts)
                updateSummary(workouts)
            }
    }

    private fun stopListening() {
        workoutsListener?.remove()
        workoutsListener = null
    }

    private fun updateSummary(workouts: List<Workout>) {
        val totalCalories = workouts.sumOf { it.caloriesBurned }
        val totalMinutes = workouts.sumOf { it.durationMinutes }

        tvTotalCalories.text = NumberFormat.getIntegerInstance().format(totalCalories)
        tvTotalMinutes.text = formatMinutes(totalMinutes)
    }

    private fun formatMinutes(total: Int): String =
        if (total >= 60) "${total / 60}h ${total % 60}m" else "$total mins"

    private fun goToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}

