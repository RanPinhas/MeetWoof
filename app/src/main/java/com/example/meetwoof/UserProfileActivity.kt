package com.example.meetwoof

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class UserProfileActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val randomUserImages = listOf(
        "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=500&q=80",
        "https://images.unsplash.com/photo-1599566150163-29194dcaad36?w=500&q=80",
        "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=500&q=80",
        "https://images.unsplash.com/photo-1527980965255-d3b416303d12?w=500&q=80",
        "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=500&q=80",
        "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=500&q=80"
    )

    private lateinit var etFullName: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etBio: TextInputEditText
    private lateinit var spGender: Spinner
    private lateinit var imgUserProfile: ImageView
    private lateinit var flProfileImage: FrameLayout

    private var currentImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        NavigationManager.setupBottomNavigation(this)

        etFullName = findViewById(R.id.etFullName)
        etPhone = findViewById(R.id.etPhone)
        etBio = findViewById(R.id.etBio)
        spGender = findViewById(R.id.spGender)
        imgUserProfile = findViewById(R.id.imgUserProfile)
        flProfileImage = findViewById(R.id.flProfileImage)

        val btnSave = findViewById<Button>(R.id.btnSaveProfile)

        val genderOptions = arrayOf("Male", "Female", "Prefer not to say")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spGender.adapter = adapter

        flProfileImage.setOnClickListener {
            Toast.makeText(this, "Photo upload coming soon!", Toast.LENGTH_SHORT).show()
        }

        loadUserProfile()

        btnSave.setOnClickListener {
            saveUserProfileToCloud()
        }
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name")
                    val phone = document.getString("phone")
                    val bio = document.getString("bio")
                    val gender = document.getString("gender")

                    currentImageUrl = document.getString("profileImageUrl")

                    if (!name.isNullOrEmpty()) etFullName.setText(name)
                    if (!phone.isNullOrEmpty()) etPhone.setText(phone)
                    if (!bio.isNullOrEmpty()) etBio.setText(bio)

                    if (gender != null) {
                        val adapter = spGender.adapter as ArrayAdapter<String>
                        val position = adapter.getPosition(gender)
                        if (position >= 0) spGender.setSelection(position)
                    }

                    if (!currentImageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(currentImageUrl)
                            .placeholder(R.drawable.img_person)
                            .error(R.drawable.img_person)
                            .into(imgUserProfile)
                    }

                } else {
                    auth.currentUser?.let { user ->
                        etFullName.setText(user.displayName)
                        etPhone.setText(user.phoneNumber)
                    }
                }
            }
    }

    private fun saveUserProfileToCloud() {
        val name = etFullName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val bio = etBio.text.toString().trim()
        val gender = spGender.selectedItem.toString()
        val userId = auth.currentUser?.uid

        if (name.isEmpty()) {
            etFullName.error = "Name is required"
            return
        }

        if (userId == null) return

        val imageUrlToSave = currentImageUrl ?: randomUserImages.random()

        val userMap = hashMapOf(
            "name" to name,
            "phone" to phone,
            "bio" to bio,
            "gender" to gender,
            "profileImageUrl" to imageUrlToSave
        )

        db.collection("users").document(userId).set(userMap, SetOptions.merge())
            .addOnSuccessListener {
                if (phone.isNotEmpty()) {
                    val phoneLookup = hashMapOf("uid" to userId)
                    db.collection("phones_lookup").document(phone).set(phoneLookup)
                }

                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                navigateToHome()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}