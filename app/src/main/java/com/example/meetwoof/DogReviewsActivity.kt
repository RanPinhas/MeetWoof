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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class DogReviewsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var rvReviews: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: ReviewsAdapter
    private val reviewsList = mutableListOf<Review>()

    private var targetDogId: String = ""
    private var targetDogName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dog_reviews)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        targetDogId = intent.getStringExtra("TARGET_DOG_ID") ?: ""
        targetDogName = intent.getStringExtra("TARGET_DOG_NAME") ?: ""

        findViewById<TextView>(R.id.tvTitle).text = "Reviews for $targetDogName"
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        rvReviews = findViewById(R.id.rvDogReviews)
        tvEmpty = findViewById(R.id.tvEmptyReviews)

        rvReviews.layoutManager = LinearLayoutManager(this)
        adapter = ReviewsAdapter(reviewsList) { review ->
            val myUid = auth.currentUser?.uid ?: return@ReviewsAdapter
            if (review.reviewerId == myUid) {
                Toast.makeText(this, "You can't like your own review!", Toast.LENGTH_SHORT).show()
                return@ReviewsAdapter
            }
            val reviewRef = db.collection("reviews").document(review.id)
            if (review.likedBy.contains(myUid)) {
                reviewRef.update("likedBy", FieldValue.arrayRemove(myUid))
                review.likedBy = review.likedBy - myUid
            } else {
                reviewRef.update("likedBy", FieldValue.arrayUnion(myUid))
                review.likedBy = review.likedBy + myUid
            }
            rvReviews.adapter?.notifyDataSetChanged()
        }
        rvReviews.adapter = adapter

        loadReviews()
    }

    private fun loadReviews() {
        if (targetDogId.isEmpty()) return
        db.collection("reviews").whereEqualTo("targetDogId", targetDogId).get()
            .addOnSuccessListener { docs ->
                reviewsList.clear()
                for (d in docs) {
                    val r = d.toObject(Review::class.java)
                    r.id = d.id
                    reviewsList.add(r)
                }
                reviewsList.sortByDescending { it.timestamp }
                adapter.notifyDataSetChanged()
                tvEmpty.visibility = if (reviewsList.isEmpty()) View.VISIBLE else View.GONE
            }
    }
}