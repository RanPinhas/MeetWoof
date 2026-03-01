package com.example.meetwoof

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyReviewsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: MyReviewsAdapter
    private lateinit var tvEmptyState: TextView
    private val myReviews = mutableListOf<Review>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_reviews)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        tvEmptyState = findViewById(R.id.tvEmptyState)

        val rv = findViewById<RecyclerView>(R.id.rvMyReviews)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = MyReviewsAdapter(myReviews) { reviewToDelete ->
            MaterialAlertDialogBuilder(this)
                .setTitle("Delete Review?")
                .setMessage("Are you sure you want to delete your review for ${reviewToDelete.targetDogName}?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    db.collection("reviews").document(reviewToDelete.id).delete()
                        .addOnSuccessListener {
                            adapter.removeItem(reviewToDelete)
                            checkEmptyState()
                            Toast.makeText(this, "Review deleted", Toast.LENGTH_SHORT).show()
                        }
                }
                .show()
        }
        rv.adapter = adapter

        loadMyReviews()
    }

    private fun loadMyReviews() {
        val myUid = auth.currentUser?.uid ?: return
        db.collection("reviews").whereEqualTo("reviewerId", myUid).get()
            .addOnSuccessListener { documents ->
                myReviews.clear()
                for (doc in documents) {
                    val review = doc.toObject(Review::class.java)
                    review.id = doc.id
                    myReviews.add(review)
                }
                myReviews.sortByDescending { it.timestamp }
                adapter.notifyDataSetChanged()
                checkEmptyState()
            }
    }

    private fun checkEmptyState() {
        tvEmptyState.visibility = if (myReviews.isEmpty()) View.VISIBLE else View.GONE
    }
}