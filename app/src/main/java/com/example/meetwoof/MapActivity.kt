package com.example.meetwoof

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var selectedMarker: Marker? = null
    private var selectedLatLng: LatLng? = null
    private lateinit var btnPinLocation: Button
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            enableMyLocation()
        } else {
            Toast.makeText(this, "Location permission is required to zoom to your area", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        NavigationManager.setupBottomNavigation(this)

        btnPinLocation = findViewById(R.id.btnPinLocation)
        btnPinLocation.visibility = View.VISIBLE

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnPinLocation.setOnClickListener {
            if (selectedLatLng == null) {
                Toast.makeText(this, "Please wait for GPS or tap the map to pin a location first!", Toast.LENGTH_SHORT).show()
            } else {
                saveLocationToFirebase()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        mMap.setOnMapClickListener { latLng ->
            selectedMarker?.remove()
            selectedMarker = mMap.addMarker(MarkerOptions().position(latLng).title("Selected Park"))
            selectedLatLng = latLng
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        }
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                    selectedMarker?.remove()
                    selectedMarker = mMap.addMarker(MarkerOptions().position(currentLatLng).title("My Location"))

                    selectedLatLng = currentLatLng
                } else {
                    val defaultLocation = LatLng(32.0853, 34.7818)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
                }
            }
        }
    }

    private fun saveLocationToFirebase() {
        try {
            val latLng = selectedLatLng
            if (latLng == null) {
                Toast.makeText(this, "Error: Location is null", Toast.LENGTH_SHORT).show()
                return
            }

            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "Error: User not connected", Toast.LENGTH_SHORT).show()
                return
            }

            val hash = GeoFireUtils.getGeoHashForLocation(GeoLocation(latLng.latitude, latLng.longitude))

            val locationData = hashMapOf(
                "latitude" to latLng.latitude,
                "longitude" to latLng.longitude,
                "geohash" to hash
            )

            db.collection("users").document(userId)
                .set(hashMapOf("location" to locationData), SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Location saved successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Firebase Error: ${e.message}", Toast.LENGTH_LONG).show()
                }

        } catch (e: Throwable) {
            e.printStackTrace()
            Toast.makeText(this, "Crash Prevented! Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}