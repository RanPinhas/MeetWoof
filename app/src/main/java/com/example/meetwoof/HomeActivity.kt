package com.example.meetwoof

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DogReport(
    val dogName: String,
    val imageUrl: String,
    val matchesCount: Int,
    val likesSentCount: Int,
    var aiMotivation: String = ""
)

class HomeActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var containerReminders: LinearLayout
    private lateinit var tvNoReminders: TextView
    private var userProfileListener: ListenerRegistration? = null

    private lateinit var pbMagicBowl: ProgressBar
    private lateinit var btnBowlClickArea: View
    private lateinit var tvCountdownTimer: TextView
    private var bowlTimer: CountDownTimer? = null
    private val TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000L
    private val MAX_CLIP_PROGRESS = 10000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        NavigationManager.setupBottomNavigation(this)

        containerReminders = findViewById(R.id.containerTodayReminders)
        tvNoReminders = findViewById(R.id.tvNoRemindersToday)

        pbMagicBowl = findViewById(R.id.pbMagicBowl)
        btnBowlClickArea = findViewById(R.id.btnBowlClickArea)
        tvCountdownTimer = findViewById(R.id.tvCountdownTimer)

        findViewById<FloatingActionButton>(R.id.btnGoToChats).setOnClickListener {
            NavigationManager.vibrate(it)
            startActivity(Intent(this, RecentChatsActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener { view ->
            NavigationManager.vibrate(view)
            showSettingsMenu(view)
        }

        findViewById<CardView>(R.id.cardRemindersCenter).setOnClickListener {
            NavigationManager.vibrate(it)
            startActivity(Intent(this, RemindersActivity::class.java))
        }

        btnBowlClickArea.setOnClickListener {
            if (pbMagicBowl.progress == MAX_CLIP_PROGRESS) {
                handleMagicBowlClick()
            } else {
                Toast.makeText(this, "The bowl is still filling up! 🦴", Toast.LENGTH_SHORT).show()
            }
        }

        setupDynamicGreeting()
        setupMagicBowl()
    }

    override fun onResume() {
        super.onResume()
        loadTodayReminders()
    }

    override fun onDestroy() {
        super.onDestroy()
        userProfileListener?.remove()
        bowlTimer?.cancel()
    }

    private fun setupDynamicGreeting() {
        val uid = auth.currentUser?.uid ?: return
        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)

        userProfileListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) {
                    return@addSnapshotListener
                }

                val fullName = snapshot.getString("name") ?: ""
                val firstName = fullName.split(" ").firstOrNull() ?: fullName
                val myDogs = snapshot.get("myDogs") as? List<String> ?: emptyList()

                if (myDogs.isEmpty()) {
                    tvGreeting.text = "Hello $firstName"
                } else {
                    fetchFirstValidDogName(firstName, myDogs, 0, tvGreeting)
                }
            }
    }

    private fun fetchFirstValidDogName(firstName: String, dogIds: List<String>, index: Int, tvGreeting: TextView) {
        if (index >= dogIds.size) {
            tvGreeting.text = "Hello $firstName"
            return
        }

        val currentDogId = dogIds[index]
        db.collection("dogs").document(currentDogId).get()
            .addOnSuccessListener { dogDoc ->
                if (dogDoc.exists()) {
                    val dogName = dogDoc.getString("name") ?: ""
                    if (dogName.isNotEmpty()) {
                        tvGreeting.text = "Hello $firstName & $dogName"
                    } else {
                        fetchFirstValidDogName(firstName, dogIds, index + 1, tvGreeting)
                    }
                } else {
                    fetchFirstValidDogName(firstName, dogIds, index + 1, tvGreeting)
                }
            }
            .addOnFailureListener {
                fetchFirstValidDogName(firstName, dogIds, index + 1, tvGreeting)
            }
    }

    private fun loadTodayReminders() {
        val userId = auth.currentUser?.uid ?: return
        val todayDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)

        db.collection("reminders")
            .whereArrayContains("owners", userId)
            .whereEqualTo("isDone", false)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                if (snapshots != null) {
                    containerReminders.removeAllViews()

                    val todaysReminders = mutableListOf<Reminder>()

                    for (doc in snapshots) {
                        val reminder = doc.toObject(Reminder::class.java)
                        reminder.id = doc.id
                        if (reminder.date == todayDate) {
                            todaysReminders.add(reminder)
                        }
                    }

                    todaysReminders.sortBy { it.time }
                    val top5Reminders = todaysReminders.take(5)

                    if (top5Reminders.isNotEmpty()) {
                        tvNoReminders.visibility = View.GONE
                        containerReminders.visibility = View.VISIBLE
                        for (reminder in top5Reminders) {
                            addReminderView(reminder)
                        }
                    } else {
                        tvNoReminders.visibility = View.VISIBLE
                        containerReminders.visibility = View.GONE
                    }
                }
            }
    }

    private fun addReminderView(reminder: Reminder) {
        val itemLayout = LinearLayout(this)
        itemLayout.orientation = LinearLayout.HORIZONTAL
        itemLayout.gravity = Gravity.CENTER_VERTICAL
        itemLayout.setPadding(16, 16, 16, 16)
        itemLayout.setBackgroundColor(android.graphics.Color.parseColor("#FFF5E5"))

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, 16)
        itemLayout.layoutParams = params

        val checkBox = CheckBox(this)
        val colorStateList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#6d112a"))
        checkBox.buttonTintList = colorStateList

        val textView = TextView(this)
        val timeDisplay = if (reminder.time.isNotEmpty()) reminder.time else "All Day"
        textView.text = "${reminder.title} ($timeDisplay)"
        textView.textSize = 16f
        textView.setTextColor(android.graphics.Color.parseColor("#444444"))
        textView.setPadding(24, 0, 0, 0)

        checkBox.setOnClickListener {
            if (checkBox.isChecked) {
                db.collection("reminders").document(reminder.id)
                    .update("isDone", true)
                    .addOnSuccessListener {
                        Snackbar.make(containerReminders, "Task done!", Snackbar.LENGTH_LONG)
                            .setAction("UNDO") {
                                db.collection("reminders").document(reminder.id).update("isDone", false)
                            }
                            .show()
                    }
            }
        }

        itemLayout.addView(checkBox)
        itemLayout.addView(textView)
        containerReminders.addView(itemLayout)
    }

    private fun setupMagicBowl() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val lastClickTime = doc.getLong("lastMagicBowlClick") ?: 0L
            val currentTime = System.currentTimeMillis()
            val timePassed = currentTime - lastClickTime

            if (timePassed >= TWENTY_FOUR_HOURS_MS) {
                pbMagicBowl.progress = MAX_CLIP_PROGRESS
                tvCountdownTimer.text = "Treats are Ready! 🍖"
                tvCountdownTimer.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                btnBowlClickArea.isEnabled = true
            } else {
                val timeRemaining = TWENTY_FOUR_HOURS_MS - timePassed
                btnBowlClickArea.isEnabled = false
                tvCountdownTimer.setTextColor(android.graphics.Color.parseColor("#757575"))
                startCountdownTimer(timeRemaining, lastClickTime)
            }
        }
    }

    private fun startCountdownTimer(millisInFuture: Long, lastClickTime: Long) {
        bowlTimer?.cancel()
        bowlTimer = object : CountDownTimer(millisInFuture, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
                val minutes = (millisUntilFinished / (1000 * 60)) % 60
                val seconds = (millisUntilFinished / 1000) % 60

                tvCountdownTimer.text = String.format(Locale.getDefault(), "Filling up... %02d:%02d:%02d", hours, minutes, seconds)

                val timePassed = System.currentTimeMillis() - lastClickTime
                val ratio = timePassed.toDouble() / TWENTY_FOUR_HOURS_MS.toDouble()
                val progress = (ratio * MAX_CLIP_PROGRESS).toInt()

                pbMagicBowl.progress = progress
            }

            override fun onFinish() {
                pbMagicBowl.progress = MAX_CLIP_PROGRESS
                tvCountdownTimer.text = "Treats are Ready! 🍖"
                tvCountdownTimer.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                btnBowlClickArea.isEnabled = true
            }
        }.start()
    }

    private fun handleMagicBowlClick() {
        val uid = auth.currentUser?.uid ?: return
        val now = System.currentTimeMillis()

        db.collection("users").document(uid).set(
            hashMapOf("lastMagicBowlClick" to now), SetOptions.merge()
        )
        setupMagicBowl()

        Toast.makeText(this, "Analyzing data & asking AI... 🧠", Toast.LENGTH_LONG).show()

        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val myDogs = doc.get("myDogs") as? List<String> ?: emptyList()
            if (myDogs.isNotEmpty()) {
                val reportsList = mutableListOf<DogReport>()
                fetchDogReports(myDogs, 0, reportsList)
            } else {
                Toast.makeText(this, "You need a dog profile first!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchDogReports(dogIds: List<String>, index: Int, reports: MutableList<DogReport>) {
        if (index >= dogIds.size) {
            generateAiMotivationsAndShow(reports)
            return
        }

        val dogId = dogIds[index]
        db.collection("dogs").document(dogId).get().addOnSuccessListener { dogDoc ->
            val dogName = dogDoc.getString("name") ?: "Your Dog"
            val imageUrl = dogDoc.getString("imageUrl") ?: ""
            val matchesArray = dogDoc.get("matches") as? List<String> ?: emptyList()
            val matchesCount = matchesArray.size

            db.collection("likes").whereEqualTo("fromDogId", dogId).get().addOnSuccessListener { likesSnapshot ->
                val likesSentCount = likesSnapshot.size()

                reports.add(DogReport(dogName, imageUrl, matchesCount, likesSentCount))
                fetchDogReports(dogIds, index + 1, reports)

            }.addOnFailureListener {
                reports.add(DogReport(dogName, imageUrl, matchesCount, 0))
                fetchDogReports(dogIds, index + 1, reports)
            }
        }.addOnFailureListener {
            fetchDogReports(dogIds, index + 1, reports)
        }
    }

    private fun generateAiMotivationsAndShow(reports: List<DogReport>) {
        if (reports.isEmpty()) return

        val client = OkHttpClient()
        val apiKey = BuildConfig.GEMINI_API_KEY.replace("\"", "").trim()

        if (apiKey.isEmpty() || apiKey.contains("YOUR_API_KEY")) {
            runOnUiThread {
                Toast.makeText(this, "Error: Key is empty!", Toast.LENGTH_LONG).show()
                reports.forEach { it.aiMotivation = "Keep swiping! 🐾" }
                showDogReportDialog(reports, 0)
            }
            return
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        var completedRequests = 0

        for (report in reports) {
            val prompt = "My dog ${report.dogName} has ${report.matchesCount} matches and swiped right ${report.likesSentCount} times on a dating app for dogs. Write exactly ONE short, funny, and encouraging sentence of advice for him."

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val requestBody = RequestBody.create("application/json".toMediaType(), jsonBody.toString())
            val request = Request.Builder().url(url).post(requestBody).build()

            // שולחים בקשה נפרדת לכל כלב
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    report.aiMotivation = "Keep swiping! 🐾"
                    checkIfAllDone()
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseData = response.body?.string()
                    var aiText = "Keep swiping! 🐾"

                    if (response.isSuccessful && responseData != null) {
                        try {
                            val jsonObject = JSONObject(responseData)
                            val candidates = jsonObject.getJSONArray("candidates")
                            val content = candidates.getJSONObject(0).getJSONObject("content")
                            val parts = content.getJSONArray("parts")
                            aiText = parts.getJSONObject(0).getString("text").trim()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        Log.e("GeminiTest", "API Error: ${response.code} for dog ${report.dogName}")
                    }

                    report.aiMotivation = "AI says: $aiText"
                    checkIfAllDone()
                }

                // פונקציה פנימית שמוודאת שכל הכלבים קיבלו תשובה לפני פתיחת הפופ-אפ
                private fun checkIfAllDone() {
                    synchronized(this@HomeActivity) {
                        completedRequests++
                        if (completedRequests == reports.size) {
                            runOnUiThread {
                                showDogReportDialog(reports, 0)
                            }
                        }
                    }
                }
            })
        }
    }

    private fun showDogReportDialog(reports: List<DogReport>, startIndex: Int) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = layoutInflater.inflate(R.layout.dialog_dog_report, null)
        dialog.setContentView(view)

        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.9).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val imgDog = view.findViewById<ImageView>(R.id.imgReportDog)
        val tvTitle = view.findViewById<TextView>(R.id.tvReportTitle)
        val tvStats = view.findViewById<TextView>(R.id.tvReportStats)
        val tvMotivation = view.findViewById<TextView>(R.id.tvReportMotivation)
        val btnPrev = view.findViewById<ImageButton>(R.id.btnReportPrev)
        val btnNext = view.findViewById<ImageButton>(R.id.btnReportNext)
        val btnClose = view.findViewById<Button>(R.id.btnCloseReport)

        var currentIndex = startIndex

        fun updateUI() {
            val report = reports[currentIndex]
            tvTitle.text = "${report.dogName}'s Report"

            val missed = Math.max(0, report.likesSentCount - report.matchesCount)
            tvStats.text = "Matches: ${report.matchesCount}\nSwiped Right: ${report.likesSentCount}\nPending / Missed: $missed"

            tvMotivation.text = report.aiMotivation

            if (report.imageUrl.isNotEmpty()) {
                Glide.with(this@HomeActivity).load(report.imageUrl).centerCrop().into(imgDog)
            } else {
                imgDog.setImageResource(R.drawable.img_logo)
            }

            btnPrev.visibility = if (currentIndex > 0) View.VISIBLE else View.INVISIBLE
            btnNext.visibility = if (currentIndex < reports.size - 1) View.VISIBLE else View.INVISIBLE
        }

        btnPrev.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                updateUI()
            }
        }

        btnNext.setOnClickListener {
            if (currentIndex < reports.size - 1) {
                currentIndex++
                updateUI()
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        updateUI()
        dialog.show()
    }

    private fun showSettingsMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add("Change Password")
        popup.menu.add("Delete Account")
        popup.menu.add("Logout")

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Change Password" -> showChangePasswordDialog()
                "Delete Account" -> showDeleteAccountDialog()
                "Logout" -> performLogout()
            }
            true
        }
        popup.show()
    }

    private fun showChangePasswordDialog() {
        val editText = EditText(this)
        editText.hint = "Enter new password"
        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        val container = android.widget.FrameLayout(this)
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = 60
        params.rightMargin = 60
        editText.layoutParams = params
        container.addView(editText)

        MaterialAlertDialogBuilder(this)
            .setTitle("Change Password")
            .setMessage("Please enter your new password:")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Update") { _, _ ->
                val newPassword = editText.text.toString()
                if (newPassword.length >= 6) {
                    updatePasswordInFirebase(newPassword)
                } else {
                    Toast.makeText(this, "Password must be at least 6 chars", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun updatePasswordInFirebase(newPass: String) {
        val user = auth.currentUser
        user?.updatePassword(newPass)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error: Re-login required.", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun showDeleteAccountDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone and will delete your dogs.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deleteUserAccountAndDogs()
            }
            .show()
    }

    private fun deleteUserAccountAndDogs() {
        val user = auth.currentUser
        val uid = user?.uid ?: return

        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val userPhone = userDoc.getString("phone")

            db.collection("dogs").whereEqualTo("primaryOwnerId", uid).get()
                .addOnSuccessListener { querySnapshot ->
                    val batch = db.batch()

                    for (document in querySnapshot.documents) {
                        batch.delete(document.reference)
                    }

                    if (!userPhone.isNullOrEmpty()) {
                        val phoneRef = db.collection("phones_lookup").document(userPhone)
                        batch.delete(phoneRef)
                    }

                    val userRef = db.collection("users").document(uid)
                    batch.delete(userRef)

                    batch.commit().addOnSuccessListener {
                        user.delete().addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Account and dogs deleted", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, SplashActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this, "Error: Re-login required to delete", Toast.LENGTH_LONG).show()
                            }
                        }
                    }.addOnFailureListener {
                        Toast.makeText(this, "Error deleting data", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error finding dogs", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener {
            Toast.makeText(this, "Error retrieving user data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performLogout() {
        com.firebase.ui.auth.AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                val intent = Intent(this, SplashActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
    }
}