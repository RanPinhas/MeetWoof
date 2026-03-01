package com.example.meetwoof

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MyDogsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddDog: FloatingActionButton
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar

    private val dogsList = mutableListOf<Dog>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_dogs)

        initViews()

        NavigationManager.setupBottomNavigation(this)
    }

    override fun onResume() {
        super.onResume()
        loadDogs()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.rvMyDogs)
        fabAddDog = findViewById(R.id.fabAddDog)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        progressBar = findViewById(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(this)

        fabAddDog.setOnClickListener {
            val intent = Intent(this, DogProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadDogs() {
        val myUid = auth.currentUser?.uid ?: return

        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        tvEmptyState.visibility = View.GONE

        db.collection("users").document(myUid).get()
            .addOnSuccessListener { document ->
                val myDogsIds = document.get("myDogs") as? List<String> ?: listOf()

                if (myDogsIds.isNotEmpty()) {
                    dogsList.clear()
                    var loadedCount = 0

                    for (dogId in myDogsIds) {
                        db.collection("dogs").document(dogId).get()
                            .addOnSuccessListener { dogDoc ->
                                val dog = dogDoc.toObject(Dog::class.java)
                                if (dog != null) {
                                    dog.id = dogDoc.id
                                    dogsList.add(dog)
                                }
                                loadedCount++
                                if (loadedCount == myDogsIds.size) updateUI()
                            }
                            .addOnFailureListener {
                                loadedCount++
                                if (loadedCount == myDogsIds.size) updateUI()
                            }
                    }
                } else {
                    dogsList.clear()
                    updateUI()
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error loading dogs", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI() {
        progressBar.visibility = View.GONE

        if (dogsList.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            setupAdapter()
        }
    }

    private fun setupAdapter() {
        val myUid = auth.currentUser?.uid ?: ""

        val adapter = DogsAdapter(
            dogsList,
            myUid,
            onDogClick = { dog ->
                val intent = Intent(this, DogProfileActivity::class.java)
                intent.putExtra("DOG_ID", dog.id)
                startActivity(intent)
            },
            onDeleteClick = { dog ->
                handleDeleteOrLeave(dog)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun handleDeleteOrLeave(dog: Dog) {
        val myUid = auth.currentUser?.uid ?: return
        val isPrimaryOwner = (dog.primaryOwnerId == myUid) || (dog.primaryOwnerId.isEmpty())

        if (isPrimaryOwner) {
            showDeleteDialog(dog)
        } else {
            showLeaveDialog(dog)
        }
    }

    private fun showDeleteDialog(dog: Dog) {
        AlertDialog.Builder(this)
            .setTitle("Delete Dog")
            .setMessage("Are you sure? This will delete ${dog.name} permanently for everyone.")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("dogs").document(dog.id).delete()
                    .addOnSuccessListener {
                        loadDogs()
                        Toast.makeText(this, "Dog deleted", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLeaveDialog(dog: Dog) {
        AlertDialog.Builder(this)
            .setTitle("Leave Shared Dog")
            .setMessage("Remove yourself from ${dog.name}? The dog will remain with the owner.")
            .setPositiveButton("Leave") { _, _ ->
                removeSelfFromDog(dog)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeSelfFromDog(dog: Dog) {
        val myUid = auth.currentUser?.uid ?: return
        val batch = db.batch()

        val dogRef = db.collection("dogs").document(dog.id)
        batch.update(dogRef, "owners", FieldValue.arrayRemove(myUid))

        val userRef = db.collection("users").document(myUid)
        batch.update(userRef, "myDogs", FieldValue.arrayRemove(dog.id))

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "You left ${dog.name}", Toast.LENGTH_SHORT).show()
                loadDogs()
            }
    }
}