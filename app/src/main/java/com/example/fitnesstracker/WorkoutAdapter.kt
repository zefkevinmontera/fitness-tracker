package com.example.fitnesstracker

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class WorkoutAdapter :
    ListAdapter<Workout, WorkoutAdapter.WorkoutViewHolder>(DiffCallback) {

    class WorkoutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvWorkoutTitle)
        val tvDetails: TextView = view.findViewById(R.id.tvWorkoutDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout, parent, false)
        return WorkoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val workout = getItem(position)
        holder.tvTitle.text = workout.title
        holder.tvDetails.text = "${workout.durationMinutes} mins • ${workout.caloriesBurned} kcal"

        // Tap a workout to edit it. AddWorkoutActivity switches to edit mode when it receives an id.
        holder.itemView.setOnClickListener { v ->
            if (workout.id.isBlank()) return@setOnClickListener
            val intent = Intent(v.context, AddWorkoutActivity::class.java).apply {
                putExtra(AddWorkoutActivity.EXTRA_ID, workout.id)
                putExtra(AddWorkoutActivity.EXTRA_TITLE, workout.title)
                putExtra(AddWorkoutActivity.EXTRA_DURATION, workout.durationMinutes)
                putExtra(AddWorkoutActivity.EXTRA_CALORIES, workout.caloriesBurned)
            }
            v.context.startActivity(intent)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Workout>() {
        override fun areItemsTheSame(old: Workout, new: Workout) = old.id == new.id
        override fun areContentsTheSame(old: Workout, new: Workout) = old == new
    }
}