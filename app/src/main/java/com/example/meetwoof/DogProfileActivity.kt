package com.example.meetwoof

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class DogProfileActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val randomDogImages = listOf(
        "https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=500&q=80",
        "https://images.unsplash.com/photo-1517849845537-4d257902454a?w=500&q=80",
        "https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=500&q=80",
        "https://images.unsplash.com/photo-1537151608828-ea2b11777ee8?w=500&q=80",
        "https://images.unsplash.com/photo-1583337130417-3346a1be7dee?w=500&q=80",
        "https://images.unsplash.com/photo-1593134257782-e89567b7718a?w=500&q=80",
        "https://images.unsplash.com/photo-1552053831-71594a27632d?w=500&q=80",
        "https://images.unsplash.com/photo-1505628346881-b72b27e84530?w=500&q=80",
        "https://images.unsplash.com/photo-1558788353-f76d92427f16?w=500&q=80",
        "https://images.unsplash.com/photo-1507146426996-ef05306b995a?w=500&q=80"
    )

    private lateinit var btnBack: ImageButton
    private lateinit var imgDog: ImageView
    private lateinit var btnEditPhoto: FloatingActionButton
    private lateinit var etDogName: TextInputEditText
    private lateinit var etBreed: MaterialAutoCompleteTextView
    private lateinit var etAge: TextInputEditText
    private lateinit var rgGender: RadioGroup
    private lateinit var etBio: TextInputEditText
    private lateinit var sliderEnergy: Slider
    private lateinit var etCoOwnerPhone: TextInputEditText
    private lateinit var btnAddCoOwner: Button
    private lateinit var llCoOwnersList: LinearLayout
    private lateinit var btnSaveDog: Button
    private lateinit var btnDeleteDog: Button

    private lateinit var cvAddPartner: CardView
    private lateinit var cvOwnersList: CardView

    private var dogId: String? = null
    private var currentDog: Dog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dog_profile)

        dogId = intent.getStringExtra("DOG_ID")

        initViews()

        if (dogId != null) {
            loadDogData()
            btnSaveDog.text = "Update Profile"
        } else {
            btnSaveDog.text = "Create New Dog"
            cvAddPartner.visibility = View.VISIBLE
            cvOwnersList.visibility = View.GONE
            btnDeleteDog.visibility = View.GONE
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        imgDog = findViewById(R.id.imgDogProfile)
        btnEditPhoto = findViewById(R.id.btnEditPhoto)
        etDogName = findViewById(R.id.etDogName)
        etBreed = findViewById(R.id.etBreed)
        etAge = findViewById(R.id.etAge)
        rgGender = findViewById(R.id.rgGender)
        etBio = findViewById(R.id.etDogBio)
        sliderEnergy = findViewById(R.id.sliderEnergy)
        etCoOwnerPhone = findViewById(R.id.etCoOwnerPhone)
        btnAddCoOwner = findViewById(R.id.btnAddCoOwner)
        llCoOwnersList = findViewById(R.id.llCoOwnersList)
        btnSaveDog = findViewById(R.id.btnSaveDog)
        btnDeleteDog = findViewById(R.id.btnDeleteDog)

        cvAddPartner = findViewById(R.id.cvAddPartner)
        cvOwnersList = findViewById(R.id.cvOwnersList)

        val breeds = resources.getStringArray(R.array.dog_breeds)
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, breeds)
        etBreed.setAdapter(adapter)
        etBreed.setOnClickListener { etBreed.showDropDown() }
        etBreed.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) etBreed.showDropDown() }

        btnAddCoOwner.setOnClickListener {
            if (dogId == null) {
                Toast.makeText(this, "Please save the dog first!", Toast.LENGTH_SHORT).show()
            } else {
                val phoneInput = etCoOwnerPhone.text.toString().trim()
                if (phoneInput.isNotEmpty()) {
                    addPartnerByPhone(phoneInput)
                } else {
                    etCoOwnerPhone.error = "Enter phone number"
                }
            }
        }

        btnSaveDog.setOnClickListener {
            saveDogChanges()
        }

        btnDeleteDog.setOnClickListener {
            val myUid = auth.currentUser?.uid
            val isPrimaryOwner = (currentDog?.primaryOwnerId == myUid) || (currentDog?.primaryOwnerId?.isEmpty() == true)

            val title = if (isPrimaryOwner) "Delete Dog" else "Remove Dog"
            val message = if (isPrimaryOwner) "Are you sure? This will delete the dog for EVERYONE involved." else "Are you sure you want to remove this dog from your list?"
            val btnText = if (isPrimaryOwner) "Delete" else "Remove"

            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(btnText) { _, _ ->
                    if (isPrimaryOwner) {
                        deleteDogCompletely()
                    } else {
                        removeMyselfFromDog()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnEditPhoto.setOnClickListener {
            Toast.makeText(this, "Photo upload coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDogData() {
        db.collection("dogs").document(dogId!!).get()
            .addOnSuccessListener { document ->
                currentDog = document.toObject(Dog::class.java)
                if (currentDog != null) {
                    currentDog!!.id = document.id
                    checkPermissionsAndVisibility(currentDog!!)
                    populateFields(currentDog!!)
                    loadOwnersList(currentDog!!)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading dog data", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun checkPermissionsAndVisibility(dog: Dog) {
        val myUid = auth.currentUser?.uid
        val isPrimaryOwner = (dog.primaryOwnerId == myUid) || (dog.primaryOwnerId.isEmpty())

        if (isPrimaryOwner) {
            cvAddPartner.visibility = View.VISIBLE
            btnDeleteDog.text = "Delete Dog"
        } else {
            cvAddPartner.visibility = View.GONE
            btnDeleteDog.text = "Remove from My Dogs"
        }
        btnDeleteDog.visibility = View.VISIBLE
        cvOwnersList.visibility = View.VISIBLE
    }

    private fun populateFields(dog: Dog) {
        try {
            etDogName.setText(dog.name)
            etBreed.setText(dog.breed)
            etAge.setText(dog.age.toString())
            etBio.setText(dog.bio)

            val energy = dog.energyLevel.toFloat()
            sliderEnergy.value = if (energy < 1f) 1f else if (energy > 5f) 5f else energy

            if (dog.gender == "Female") {
                findViewById<RadioButton>(R.id.rbFemale).isChecked = true
            } else {
                findViewById<RadioButton>(R.id.rbMale).isChecked = true
            }

            if (dog.imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(dog.imageUrl)
                    .placeholder(android.R.drawable.ic_menu_camera)
                    .error(android.R.drawable.ic_delete)
                    .into(imgDog)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadOwnersList(dog: Dog) {
        llCoOwnersList.removeAllViews()

        val myUid = auth.currentUser?.uid
        val amIManager = (myUid == dog.primaryOwnerId) || (dog.primaryOwnerId.isEmpty())

        for (ownerId in dog.owners) {
            db.collection("users").document(ownerId).get()
                .addOnSuccessListener { userDoc ->
                    val userName = userDoc.getString("name") ?: "Unknown"
                    val userContact = userDoc.getString("phone") ?: userDoc.getString("email") ?: ""

                    addOwnerRowToLayout(userName, userContact, ownerId, amIManager, dog.primaryOwnerId)
                }
        }
    }

    private fun addOwnerRowToLayout(name: String, contact: String, ownerId: String, amIManager: Boolean, primaryId: String) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, 16, 0, 16)

        val textView = TextView(this)
        var displayText = "$name ($contact)"
        if (ownerId == primaryId) displayText += " ⭐"

        textView.text = displayText
        textView.textSize = 16f
        textView.setTextColor(Color.parseColor("#6d112a"))

        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        textView.layoutParams = params
        row.addView(textView)

        if (amIManager && ownerId != primaryId) {
            val deleteBtn = ImageButton(this)
            deleteBtn.setImageResource(android.R.drawable.ic_menu_delete)
            deleteBtn.background = null
            deleteBtn.setColorFilter(Color.parseColor("#D32F2F"))
            deleteBtn.setOnClickListener { removePartner(ownerId, name) }
            row.addView(deleteBtn)
        }

        llCoOwnersList.addView(row)

        val divider = View(this)
        divider.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
        divider.setBackgroundColor(Color.parseColor("#E0E0E0"))
        llCoOwnersList.addView(divider)
    }

    private fun addPartnerByPhone(phoneNumber: String) {
        db.collection("users").whereEqualTo("phone", phoneNumber).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userIdToAdd = documents.documents[0].id

                    if (currentDog?.owners?.contains(userIdToAdd) == true) {
                        Toast.makeText(this, "User is already an owner", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val batch = db.batch()
                    val dogRef = db.collection("dogs").document(dogId!!)
                    val userRef = db.collection("users").document(userIdToAdd)

                    batch.update(dogRef, "owners", FieldValue.arrayUnion(userIdToAdd))
                    batch.update(userRef, "myDogs", FieldValue.arrayUnion(dogId))

                    batch.commit().addOnSuccessListener {
                        Toast.makeText(this, "Partner Added!", Toast.LENGTH_SHORT).show()
                        etCoOwnerPhone.text?.clear()
                        loadDogData()
                    }
                } else {
                    Toast.makeText(this, "User not found with phone: $phoneNumber", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun removePartner(userIdToRemove: String, name: String) {
        AlertDialog.Builder(this)
            .setTitle("Remove Partner")
            .setMessage("Remove $name?")
            .setPositiveButton("Yes") { _, _ ->
                val batch = db.batch()
                batch.update(db.collection("dogs").document(dogId!!), "owners", FieldValue.arrayRemove(userIdToRemove))
                batch.update(db.collection("users").document(userIdToRemove), "myDogs", FieldValue.arrayRemove(dogId))
                batch.commit().addOnSuccessListener { loadDogData() }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteDogCompletely() {
        if (dogId == null) return

        val batch = db.batch()
        val dogRef = db.collection("dogs").document(dogId!!)

        batch.delete(dogRef)

        currentDog?.owners?.forEach { ownerId ->
            val userRef = db.collection("users").document(ownerId)
            batch.update(userRef, "myDogs", FieldValue.arrayRemove(dogId))
        }

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "Dog deleted completely", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error deleting dog", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeMyselfFromDog() {
        if (dogId == null) return
        val myUid = auth.currentUser?.uid ?: return

        val batch = db.batch()
        val dogRef = db.collection("dogs").document(dogId!!)
        val myUserRef = db.collection("users").document(myUid)

        batch.update(dogRef, "owners", FieldValue.arrayRemove(myUid))
        batch.update(myUserRef, "myDogs", FieldValue.arrayRemove(dogId))

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "Dog removed from your list", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error removing dog", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveDogChanges() {
        val name = etDogName.text.toString().trim()
        if (name.isEmpty()) {
            etDogName.error = "Name is required"
            return
        }

        val myUid = auth.currentUser?.uid ?: return

        val dogData = hashMapOf<String, Any>(
            "name" to name,
            "breed" to etBreed.text.toString(),
            "age" to etAge.text.toString(),
            "bio" to etBio.text.toString(),
            "energyLevel" to sliderEnergy.value.toInt(),
            "gender" to if (findViewById<RadioButton>(R.id.rbMale).isChecked) "Male" else "Female"
        )

        if (dogId != null) {
            db.collection("dogs").document(dogId!!).update(dogData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Updated successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
        } else {
            dogData["primaryOwnerId"] = myUid
            dogData["owners"] = arrayListOf(myUid)
            dogData["imageUrl"] = randomDogImages.random()

            val newRef = db.collection("dogs").document()
            val newId = newRef.id
            dogData["id"] = newId

            newRef.set(dogData)
                .addOnSuccessListener {
                    db.collection("users").document(myUid)
                        .update("myDogs", FieldValue.arrayUnion(newId))
                        .addOnSuccessListener {
                            Toast.makeText(this, "New Dog Created!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                }
        }
    }
}