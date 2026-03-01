package com.example.meetwoof

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MatchFiltersActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var isCheckingDog = true
    private val myDogsMap = mutableMapOf<String, String>()
    private var selectedMyDogId: String? = null

    private lateinit var etSelectMyDog: MaterialAutoCompleteTextView
    private lateinit var sliderDistance: Slider
    private lateinit var tvDistanceValue: TextView
    private lateinit var sliderAgeRange: RangeSlider
    private lateinit var tvAgeRangeValue: TextView
    private lateinit var sliderEnergyRange: RangeSlider
    private lateinit var tvEnergyRangeValue: TextView
    private lateinit var rgGenderFilter: RadioGroup
    private lateinit var etFilterBreed: MaterialAutoCompleteTextView
    private lateinit var btnLetsWoof: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_match_filters)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        NavigationManager.setupBottomNavigation(this)

        initViews()
        setupBreedDropdown()
        setupListeners()

        fetchMyDogs()
    }

    private fun fetchMyDogs() {
        val myUid = auth.currentUser?.uid ?: return
        isCheckingDog = true

        db.collection("dogs").whereArrayContains("owners", myUid).get()
            .addOnSuccessListener { documents ->
                isCheckingDog = false
                myDogsMap.clear()
                val dogNames = mutableListOf<String>()

                for (doc in documents) {
                    val dogName = doc.getString("name") ?: "Unknown Dog"
                    myDogsMap[dogName] = doc.id
                    dogNames.add(dogName)
                }

                if (dogNames.isEmpty()) {
                    showNoDogDialog()
                } else {
                    val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, dogNames)
                    etSelectMyDog.setAdapter(adapter)

                    // בוחר אוטומטית את הכלב הראשון ברשימה
                    etSelectMyDog.setText(dogNames[0], false)
                    selectedMyDogId = myDogsMap[dogNames[0]]
                }
            }
            .addOnFailureListener {
                isCheckingDog = false
                Toast.makeText(this, "Failed to load your dogs", Toast.LENGTH_SHORT).show()
            }
    }

    private fun initViews() {
        etSelectMyDog = findViewById(R.id.etSelectMyDog)
        sliderDistance = findViewById(R.id.sliderDistance)
        tvDistanceValue = findViewById(R.id.tvDistanceValue)
        sliderAgeRange = findViewById(R.id.sliderAgeRange)
        tvAgeRangeValue = findViewById(R.id.tvAgeRangeValue)
        sliderEnergyRange = findViewById(R.id.sliderEnergyRange)
        tvEnergyRangeValue = findViewById(R.id.tvEnergyRangeValue)
        rgGenderFilter = findViewById(R.id.rgGenderFilter)
        etFilterBreed = findViewById(R.id.etFilterBreed)
        btnLetsWoof = findViewById(R.id.btnLetsWoof)
    }

    private fun setupBreedDropdown() {
        val breeds = try {
            resources.getStringArray(R.array.dog_breeds)
        } catch (e: Exception) {
            arrayOf("Labrador", "Poodle", "Husky", "Bulldog", "Golden Retriever", "German Shepherd")
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, breeds)
        etFilterBreed.setAdapter(adapter)
    }

    private fun setupListeners() {
        etSelectMyDog.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position) as String
            selectedMyDogId = myDogsMap[selectedName]
        }

        sliderDistance.addOnChangeListener { _, value, _ ->
            tvDistanceValue.text = "${value.toInt()} km"
        }

        sliderAgeRange.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            val minAge = values[0].toInt()
            val maxAge = values[1].toInt()
            tvAgeRangeValue.text = "$minAge - $maxAge"
        }

        sliderEnergyRange.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            val minEnergy = values[0].toInt()
            val maxEnergy = values[1].toInt()
            tvEnergyRangeValue.text = "$minEnergy - $maxEnergy"
        }

        btnLetsWoof.setOnClickListener {
            handleStartGameClick()
        }
    }

    private fun handleStartGameClick() {
        if (isCheckingDog) {
            Toast.makeText(this, "Loading profile... please wait", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedMyDogId == null) {
            Toast.makeText(this, "Please select which dog you are playing as.", Toast.LENGTH_SHORT).show()
            return
        }

        applyFiltersAndStartGame()
    }

    private fun showNoDogDialog() {
        AlertDialog.Builder(this)
            .setTitle("Create a Dog Profile")
            .setMessage("You need a dog profile to start matching! Let's create one now.")
            .setPositiveButton("Create Dog") { _, _ ->
                startActivity(Intent(this, DogProfileActivity::class.java))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun applyFiltersAndStartGame() {
        val maxDistance = sliderDistance.value
        val minAge = sliderAgeRange.values[0]
        val maxAge = sliderAgeRange.values[1]
        val minEnergy = sliderEnergyRange.values[0]
        val maxEnergy = sliderEnergyRange.values[1]
        val selectedBreed = etFilterBreed.text.toString()

        val selectedGenderId = rgGenderFilter.checkedRadioButtonId
        val genderPref = when (selectedGenderId) {
            R.id.rbMale -> "Male"
            R.id.rbFemale -> "Female"
            else -> "Both"
        }

        val intent = Intent(this, DogMatchActivity::class.java)

        intent.putExtra("MY_DOG_ID", selectedMyDogId)

        intent.putExtra("FILTER_DISTANCE", maxDistance)
        intent.putExtra("FILTER_MIN_AGE", minAge)
        intent.putExtra("FILTER_MAX_AGE", maxAge)
        intent.putExtra("FILTER_MIN_ENERGY", minEnergy)
        intent.putExtra("FILTER_MAX_ENERGY", maxEnergy)
        intent.putExtra("FILTER_GENDER", genderPref)
        intent.putExtra("FILTER_BREED", selectedBreed)

        startActivity(intent)
    }
}