package com.example.meetwoof

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RemindersActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var fabAdd: FloatingActionButton

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val remindersList = mutableListOf<Reminder>()
    private lateinit var adapter: RemindersAdapter

    // רשימות עזר
    private val myDogNames = mutableListOf<String>()
    private val dogNameToId = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reminders)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        NavigationManager.setupBottomNavigation(this)

        initViews()
        setupRecyclerView()
        fetchUserDogsForSpinner()

        listenToReminders()

        fabAdd.setOnClickListener {
            showAddReminderDialog()
        }
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.rvReminders)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        fabAdd = findViewById(R.id.fabAddReminder)
    }

    private fun setupRecyclerView() {
        adapter = RemindersAdapter(
            remindersList,
            onDeleteClick = { reminder ->
                deleteReminderPermanently(reminder)
            },
            onDoneClick = { reminder, position ->
                markAsDoneWithUndo(reminder, position)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun markAsDoneWithUndo(reminder: Reminder, position: Int) {
        adapter.removeItem(position)

        updateReminderStatus(reminder.id, true)

        val snackbar = Snackbar.make(recyclerView, "Task marked as done", Snackbar.LENGTH_LONG)

        snackbar.setAction("UNDO") {
            updateReminderStatus(reminder.id, false)
            adapter.restoreItem(reminder, position)
        }

        snackbar.show()
    }

    private fun updateReminderStatus(docId: String, isDone: Boolean) {
        db.collection("reminders").document(docId)
            .update("isDone", isDone)
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchUserDogsForSpinner() {
        val userId = auth.currentUser?.uid ?: return
        myDogNames.clear()
        dogNameToId.clear()

        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            val dogIds = document.get("myDogs") as? List<String>
            if (!dogIds.isNullOrEmpty()) {
                for (dogId in dogIds) {
                    db.collection("dogs").document(dogId).get().addOnSuccessListener { dogDoc ->
                        val name = dogDoc.getString("name")
                        if (name != null) {
                            myDogNames.add(name)
                            dogNameToId[name] = dogId
                        }
                    }
                }
            }
        }
    }

    private fun showAddReminderDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_reminder, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val switchSetDate = dialogView.findViewById<MaterialSwitch>(R.id.switchSetDate)
        val layoutDateTime = dialogView.findViewById<LinearLayout>(R.id.layoutDateTimeContainer)
        val spDogSelector = dialogView.findViewById<Spinner>(R.id.spDogSelector)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etTitle)
        val etDesc = dialogView.findViewById<TextInputEditText>(R.id.etDesc)
        val btnPickDate = dialogView.findViewById<Button>(R.id.btnPickDate)
        val btnPickTime = dialogView.findViewById<Button>(R.id.btnPickTime)
        val tvSelectedDateTime = dialogView.findViewById<TextView>(R.id.tvSelectedDateTime)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveReminder)
        val btnBack = dialogView.findViewById<Button>(R.id.btnBack)

        val spinnerItems = if (myDogNames.isNotEmpty()) myDogNames else listOf("My Dog")
        spDogSelector.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, spinnerItems)

        val calendar = Calendar.getInstance()
        updateDateTimeText(tvSelectedDateTime, calendar)

        switchSetDate.setOnCheckedChangeListener { _, isChecked ->
            layoutDateTime.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        btnBack.setOnClickListener { dialog.dismiss() }

        btnPickDate.setOnClickListener {
            val datePicker = DatePickerDialog(this, { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                updateDateTimeText(tvSelectedDateTime, calendar)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
            datePicker.show()
        }

        btnPickTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                updateDateTimeText(tvSelectedDateTime, calendar)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            val dogName = spDogSelector.selectedItem?.toString() ?: "My Dog"
            val isDateEnabled = switchSetDate.isChecked

            if (title.isEmpty()) {
                Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var timeString = ""
            var dateString = ""
            var reminderTimestamp = 0L

            if (isDateEnabled) {
                if (calendar.timeInMillis > System.currentTimeMillis()) {
                    scheduleNotification(title, desc, calendar.timeInMillis)
                }
                timeString = String.format(Locale.getDefault(), "%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
                dateString = String.format(Locale.getDefault(), "%02d/%02d/%d", calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
                reminderTimestamp = calendar.timeInMillis
            }

            // קריאה לפונקציית השמירה החדשה
            saveReminderToFirebase(title, desc, timeString, dateString, dogName, reminderTimestamp)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun updateDateTimeText(textView: TextView, cal: Calendar) {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        textView.text = format.format(cal.time)
    }

    private fun scheduleNotification(title: String, message: String, timeInMillis: Long) {
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, timeInMillis.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun saveReminderToFirebase(title: String, desc: String, time: String, date: String, dogName: String, reminderTimestamp: Long) {
        val currentUserId = auth.currentUser?.uid ?: return
        val dogId = dogNameToId[dogName]

        if (dogId != null) {
            db.collection("dogs").document(dogId).get()
                .addOnSuccessListener { document ->
                    val owners = document.get("owners") as? List<String> ?: listOf(currentUserId)

                    saveDocumentToSharedCollection(title, desc, time, date, dogName, reminderTimestamp, owners, dogId)
                }
                .addOnFailureListener {
                    saveDocumentToSharedCollection(title, desc, time, date, dogName, reminderTimestamp, listOf(currentUserId), dogId)
                }
        } else {
            saveDocumentToSharedCollection(title, desc, time, date, dogName, reminderTimestamp, listOf(currentUserId), null)
        }
    }

    private fun saveDocumentToSharedCollection(
        title: String, desc: String, time: String, date: String,
        dogName: String, reminderTimestamp: Long, owners: List<String>, dogId: String?
    ) {
        val reminderData = hashMapOf(
            "title" to title,
            "description" to desc,
            "time" to time,
            "date" to date,
            "dogName" to dogName,
            "timestamp" to System.currentTimeMillis(),
            "reminderTimestamp" to reminderTimestamp,
            "owners" to owners,
            "isDone" to false,
            "dogId" to (dogId ?: "")
        )

        db.collection("reminders").add(reminderData)
            .addOnSuccessListener {
                Toast.makeText(this, "Task shared successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error saving task", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenToReminders() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("reminders")
            .whereArrayContains("owners", userId)
            .whereEqualTo("isDone", false)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    remindersList.clear()
                    for (doc in snapshots) {
                        val r = doc.toObject(Reminder::class.java)
                        r.id = doc.id
                        remindersList.add(r)
                    }

                    remindersList.sortByDescending { it.timestamp }

                    adapter.notifyDataSetChanged()
                    updateEmptyState()
                }
            }
    }

    private fun updateEmptyState() {
        tvEmptyState.visibility = if (remindersList.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (remindersList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun deleteReminderPermanently(reminder: Reminder) {
        db.collection("reminders").document(reminder.id).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show()
            }
    }
}