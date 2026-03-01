package com.example.meetwoof

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.math.abs
import kotlin.math.min

class DogMatchActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val filteredDogs = mutableListOf<Dog>()
    private var currentDogIndex = 0
    private var myDogId: String = ""

    private lateinit var tvDogName: TextView
    private lateinit var tvDogAge: TextView
    private lateinit var tvDogBreed: TextView
    private lateinit var imgDog: ImageView
    private lateinit var cardDogProfile: CardView
    private lateinit var imgLikeIndicator: ImageView
    private lateinit var imgPassIndicator: ImageView

    private var dX = 0f
    private var initialX = 0f
    private var initialY = 0f
    private var cardInitialX = 0f
    private var cardInitialY = 0f
    private val swipeLimit = 150f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dog_match)

        NavigationManager.setupBottomNavigation(this)
        initViews()
        cardDogProfile.visibility = View.INVISIBLE
        identifyMyDog()
    }

    private fun initViews() {
        tvDogName = findViewById(R.id.tvDogName)
        tvDogAge = findViewById(R.id.tvDogAge)
        tvDogBreed = findViewById(R.id.tvDogBreed)
        imgDog = findViewById(R.id.imgDog)
        cardDogProfile = findViewById(R.id.cardDogProfile)
        imgLikeIndicator = findViewById(R.id.imgLikeIndicator)
        imgPassIndicator = findViewById(R.id.imgPassIndicator)

        setupSwipeListener()

        findViewById<ImageButton>(R.id.btnInfo).setOnClickListener {
            NavigationManager.vibrate(it)
            showDogDetailsBottomSheet()
        }
    }

    private fun identifyMyDog() {
        myDogId = intent.getStringExtra("MY_DOG_ID") ?: ""
        if (myDogId.isNotEmpty()) {
            loadFiltersAndFetchDogsFromFirebase()
        } else {
            val myUid = auth.currentUser?.uid ?: return
            db.collection("dogs").whereArrayContains("owners", myUid).get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        myDogId = documents.documents[0].id
                        loadFiltersAndFetchDogsFromFirebase()
                    } else {
                        showNoDogDialog()
                    }
                }
        }
    }

    private fun showNoDogDialog() {
        AlertDialog.Builder(this)
            .setTitle("Missing Dog Profile")
            .setMessage("Please create a dog profile first.")
            .setPositiveButton("Create") { _, _ ->
                startActivity(Intent(this, DogProfileActivity::class.java))
                finish()
            }
            .setNegativeButton("Exit") { _, _ -> finish() }
            .show()
    }

    private fun loadFiltersAndFetchDogsFromFirebase() {
        val maxDistance = intent.getFloatExtra("FILTER_DISTANCE", 100f)
        val minAge = intent.getFloatExtra("FILTER_MIN_AGE", 0f)
        val maxAge = intent.getFloatExtra("FILTER_MAX_AGE", 20f)
        val minEnergy = intent.getFloatExtra("FILTER_MIN_ENERGY", 1f)
        val maxEnergy = intent.getFloatExtra("FILTER_MAX_ENERGY", 5f)
        val gender = intent.getStringExtra("FILTER_GENDER") ?: "Both"
        val breedQuery = intent.getStringExtra("FILTER_BREED") ?: ""

        val myUid = auth.currentUser?.uid ?: return

        db.collection("users").get().addOnSuccessListener { usersSnapshot ->
            val userLocations = mutableMapOf<String, Pair<Double, Double>>()
            var myLocation: Pair<Double, Double>? = null

            for (doc in usersSnapshot) {
                val locMap = doc.get("location") as? Map<String, Any>
                if (locMap != null) {
                    val lat = (locMap["latitude"] as? Number)?.toDouble()
                    val lng = (locMap["longitude"] as? Number)?.toDouble()
                    if (lat != null && lng != null) {
                        userLocations[doc.id] = Pair(lat, lng)
                        if (doc.id == myUid) {
                            myLocation = Pair(lat, lng)
                        }
                    }
                }
            }

            if (myLocation == null) {
                Toast.makeText(this, "Please pin your location on the map first!", Toast.LENGTH_LONG).show()
                finish()
                return@addOnSuccessListener
            }

            db.collection("dogs").get().addOnSuccessListener { documents ->
                filteredDogs.clear()
                for (doc in documents) {
                    try {
                        val dog = doc.toObject(Dog::class.java)
                        dog.id = doc.id

                        if (dog.id == myDogId) continue
                        if (dog.owners.contains(myUid)) continue
                        if (dog.matches.contains(myDogId)) continue

                        val dogAge = dog.age.toString().toIntOrNull() ?: 0
                        val ageMatch = dogAge >= minAge && dogAge <= maxAge
                        val energyMatch = dog.energyLevel >= minEnergy && dog.energyLevel <= maxEnergy
                        val genderMatch = gender == "Both" || dog.gender.equals(gender, ignoreCase = true)
                        val breedMatch = breedQuery.isEmpty() || dog.breed.contains(breedQuery, ignoreCase = true)

                        if (ageMatch && energyMatch && genderMatch && breedMatch) {
                            val ownerId = dog.owners.firstOrNull()
                            val ownerLoc = userLocations[ownerId]

                            if (ownerLoc != null) {
                                val results = FloatArray(1)
                                android.location.Location.distanceBetween(
                                    myLocation!!.first, myLocation!!.second,
                                    ownerLoc.first, ownerLoc.second,
                                    results
                                )
                                val distanceInKm = results[0] / 1000f

                                if (distanceInKm <= maxDistance) {
                                    filteredDogs.add(dog)
                                }
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }

                if (filteredDogs.isEmpty()) {
                    Toast.makeText(this, "No dogs found nearby matching your filters", Toast.LENGTH_SHORT).show()
                } else {
                    loadDog(0)
                }
            }
        }
    }

    private fun loadDog(index: Int) {
        resetCardAndIndicators(cardDogProfile)
        if (index < filteredDogs.size) {
            val dog = filteredDogs[index]
            tvDogName.text = dog.name
            tvDogAge.text = ", ${dog.age}"
            tvDogBreed.text = "${dog.breed} • Nearby"

            if (dog.imageUrl.isNotEmpty()) {
                Glide.with(this).load(dog.imageUrl).centerCrop().into(imgDog)
            } else {
                imgDog.setImageResource(R.drawable.img_logo)
            }

            cardDogProfile.visibility = View.VISIBLE
        } else {
            cardDogProfile.visibility = View.INVISIBLE
            Toast.makeText(this, "That's all for now!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSwipeListener() {
        cardDogProfile.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.rawX
                    cardInitialX = view.x
                    cardInitialY = view.y
                    imgLikeIndicator.visibility = View.VISIBLE
                    imgPassIndicator.visibility = View.VISIBLE
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    dX = event.rawX - initialX
                    view.x = cardInitialX + dX
                    view.rotation = dX * 0.05f
                    val swipeProgress = min(abs(dX) / swipeLimit, 1f)
                    if (dX > 0) {
                        imgLikeIndicator.alpha = swipeProgress
                        imgPassIndicator.alpha = 0f
                    } else {
                        imgPassIndicator.alpha = swipeProgress
                        imgLikeIndicator.alpha = 0f
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (dX > swipeLimit) performSwipe(true)
                    else if (dX < -swipeLimit) performSwipe(false)
                    else resetCardAndIndicators(view)
                    true
                }
                else -> false
            }
        }
    }

    private fun resetCardAndIndicators(view: View) {
        view.animate().x(cardInitialX).y(cardInitialY).rotation(0f).setDuration(200).start()
        imgLikeIndicator.animate().alpha(0f).setDuration(200).start()
        imgPassIndicator.animate().alpha(0f).setDuration(200).start()
    }

    private fun performSwipe(isLike: Boolean) {
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val targetX = if (isLike) screenWidth else -screenWidth

        cardDogProfile.animate().x(targetX).rotation(if (isLike) 20f else -20f).setDuration(300)
            .withEndAction {
                if (isLike) handleLike(filteredDogs[currentDogIndex])
                currentDogIndex++
                loadDog(currentDogIndex)
            }.start()

        imgLikeIndicator.animate().alpha(0f).setDuration(300).start()
        imgPassIndicator.animate().alpha(0f).setDuration(300).start()
    }

    private fun handleLike(targetDog: Dog) {
        if (myDogId.isEmpty()) return

        val likeId = "${myDogId}_${targetDog.id}"
        val likeData = hashMapOf(
            "fromDogId" to myDogId,
            "toDogId" to targetDog.id,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("likes").document(likeId).set(likeData)
            .addOnSuccessListener {
                checkForMatch(targetDog)
            }
    }

    private fun checkForMatch(targetDog: Dog) {
        val reverseLikeId = "${targetDog.id}_${myDogId}"

        db.collection("likes").document(reverseLikeId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    registerMatchOnDogs(targetDog)
                    createChatForMatch(targetDog)
                } else {
                    Toast.makeText(this, "Waiting for a match with ${targetDog.name}...", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registerMatchOnDogs(targetDog: Dog) {
        val myUpdate = hashMapOf("matches" to FieldValue.arrayUnion(targetDog.id))
        db.collection("dogs").document(myDogId).set(myUpdate, SetOptions.merge())

        val targetUpdate = hashMapOf("matches" to FieldValue.arrayUnion(myDogId))
        db.collection("dogs").document(targetDog.id).set(targetUpdate, SetOptions.merge())
    }

    private fun createChatForMatch(targetDog: Dog) {
        val currentUserId = auth.currentUser?.uid ?: return
        val targetUserId = if (targetDog.owners.isNotEmpty()) targetDog.owners[0] else ""
        val chatId = if (myDogId < targetDog.id) "${myDogId}_${targetDog.id}" else "${targetDog.id}_${myDogId}"

        val chatData = hashMapOf(
            "participants" to listOf(myDogId, targetDog.id),
            "users" to listOf(currentUserId, targetUserId),
            "lastMessage" to "It's a Match! Say Hi 👋",
            "lastMessageTime" to FieldValue.serverTimestamp(),
            "chatId" to chatId
        )

        db.collection("chats").document(chatId).set(chatData, SetOptions.merge())
            .addOnSuccessListener {
                if (targetUserId.isNotEmpty()) {
                    db.collection("users").document(targetUserId).get().addOnSuccessListener { userDoc ->
                        val targetUserName = userDoc.getString("name") ?: "Owner"
                        val fullTitle = "$targetUserName & ${targetDog.name}"

                        val intent = Intent(this, ChatActivity::class.java)
                        intent.putExtra("CHAT_ID", chatId)
                        intent.putExtra("CHAT_NAME", fullTitle)
                        intent.putExtra("TARGET_DOG_ID", targetDog.id)
                        intent.putExtra("TARGET_DOG_NAME", targetDog.name)
                        startActivity(intent)
                    }
                } else {
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.putExtra("CHAT_ID", chatId)
                    intent.putExtra("CHAT_NAME", targetDog.name)
                    intent.putExtra("TARGET_DOG_ID", targetDog.id)
                    intent.putExtra("TARGET_DOG_NAME", targetDog.name)
                    startActivity(intent)
                }
            }
    }

    private fun showDogDetailsBottomSheet() {
        if (currentDogIndex >= filteredDogs.size) return
        val currentDog = filteredDogs[currentDogIndex]
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_dog_details, null)
        dialog.setContentView(view)

        try {
            val btnBack = view.findViewById<Button>(R.id.btnBack)
            val tvTitle = view.findViewById<TextView>(R.id.btnBack).parent.let { (it as android.view.ViewGroup).getChildAt(1) as TextView }
            tvTitle.text = "About ${currentDog.name}"

            val descriptionView = (view as android.view.ViewGroup).getChildAt(2) as TextView
            descriptionView.text = currentDog.bio

            val tvReviewCount = view.findViewById<TextView>(R.id.tvReviewCount)
            val rvReviews = view.findViewById<RecyclerView>(R.id.rvReviews)
            rvReviews.layoutManager = LinearLayoutManager(this)

            db.collection("reviews").whereEqualTo("targetDogId", currentDog.id).get()
                .addOnSuccessListener { docs ->
                    val reviewsList = mutableListOf<Review>()
                    for (d in docs) {
                        val r = d.toObject(Review::class.java)
                        r.id = d.id
                        reviewsList.add(r)
                    }
                    reviewsList.sortByDescending { it.timestamp }
                    tvReviewCount.text = "(${reviewsList.size})"

                    val adapter = ReviewsAdapter(reviewsList) { review ->
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
                }

            btnBack.setOnClickListener { dialog.dismiss() }
        } catch (e: Exception) { e.printStackTrace() }

        dialog.show()
    }
}