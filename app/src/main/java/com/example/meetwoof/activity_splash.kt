package com.example.meetwoof

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.meetwoof.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract(),
    ) { res ->
        this.onSignInResult(res)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.imgLogo.setOnClickListener {
            checkUser()
        }
    }

    private fun checkUser() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            saveUserToFirestore(user)
        } else {
            startLogin()
        }
    }

    private fun startLogin() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setLogo(R.drawable.img_logo)
            .setTheme(R.style.Theme_MeetWoof)
            .setIsSmartLockEnabled(false)
            .build()

        signInLauncher.launch(signInIntent)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == RESULT_OK) {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                // שינוי 2: התחברות הצליחה -> שולחים לפונקציית השמירה
                saveUserToFirestore(user)
            }
        } else {
            Toast.makeText(this, "Sign in cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUserToFirestore(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(firebaseUser.uid)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                startHomeActivity()
            } else {
                val name = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "User"
                val email = firebaseUser.email ?: ""

                val userData = hashMapOf(
                    "uid" to firebaseUser.uid,
                    "name" to name,
                    "email" to email,
                    "dogs" to arrayListOf<String>() // מתחיל בלי כלבים
                )

                userRef.set(userData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profile Created Successfully!", Toast.LENGTH_SHORT).show()
                        startHomeActivity()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error creating profile: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Network error, entering offline mode", Toast.LENGTH_SHORT).show()
            startHomeActivity()
        }
    }

    private fun startHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}