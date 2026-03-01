package com.example.meetwoof

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RemindersAdapter(
    private val reminders: MutableList<Reminder>,
    private val onDeleteClick: (Reminder) -> Unit,
    private val onDoneClick: (Reminder, Int) -> Unit
) : RecyclerView.Adapter<RemindersAdapter.ReminderViewHolder>() {

    class ReminderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvReminderTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvReminderDesc)
        val tvTime: TextView = view.findViewById(R.id.tvReminderTime)
        val tvDogName: TextView = view.findViewById(R.id.tvReminderDogName)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteReminder)
        val cbDone: CheckBox = view.findViewById(R.id.cbReminderDone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reminder, parent, false)
        return ReminderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = reminders[position]

        holder.tvTitle.text = reminder.title
        holder.tvDesc.text = reminder.description

        val timeString = if (reminder.date.isNotEmpty() && reminder.time.isNotEmpty()) {
            "${reminder.date} ${reminder.time}"
        } else {
            "No Date/Time"
        }
        holder.tvTime.text = timeString

        holder.tvDogName.text = reminder.dogName

        holder.cbDone.setOnCheckedChangeListener(null)
        holder.cbDone.isChecked = reminder.isDone

        holder.cbDone.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                onDoneClick(reminder, position)
            }
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(reminder)
        }
    }

    override fun getItemCount() = reminders.size

    fun removeItem(position: Int) {
        if (position in 0 until reminders.size) {
            reminders.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun restoreItem(reminder: Reminder, position: Int) {
        reminders.add(position, reminder)
        notifyItemInserted(position)
    }
}