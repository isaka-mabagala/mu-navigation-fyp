package com.isaka.munavigation.activity

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.isaka.munavigation.R
import com.isaka.munavigation.adapter.AutocompleteLocationListAdapter
import com.isaka.munavigation.api.MapboxApi
import com.isaka.munavigation.api.MapboxApi.Companion.MAPBOX_MATRIX_WALKING_PROFILE
import com.isaka.munavigation.api.RetrofitBuilder
import com.isaka.munavigation.databinding.ActivityDistanceBinding
import com.isaka.munavigation.model.DirectionsMatrix
import com.isaka.munavigation.model.Location
import com.isaka.munavigation.util.Mapbox.locationDistanceConverter
import com.isaka.munavigation.util.Mapbox.locationDurationConverter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DistanceActivity : BaseActivity(), AutocompleteLocationListAdapter.OnItemClickListener {
    private val context = this
    private lateinit var binding: ActivityDistanceBinding
    private lateinit var startingSearchView: SearchView
    private lateinit var destinationSearchView: SearchView
    private lateinit var adapter: AutocompleteLocationListAdapter
    private var locationList: MutableList<Location> = mutableListOf()
    private var filteredLocationList: MutableList<Location> = mutableListOf()
    private var startingPointLocation: Location? = null
    private var destinationPointLocation: Location? = null
    private var mapboxMatrixProfile: String = MAPBOX_MATRIX_WALKING_PROFILE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDistanceBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // toolbar setup
        setupActionBar()

        // set locations to list
        locationList = getLocationList() as MutableList<Location>

        // location query filter
        locationQueryFilter()

        // matrix profile selection
        matrixProfileChipSelection()

        // distance calculation
        binding.tvDistanceCalculation.setOnClickListener {
            if (startingPointLocation != null && destinationPointLocation != null) {
                startingToDestinationDistance(startingPointLocation!!, destinationPointLocation!!)
            }
        }
    }

    override fun onItemClick(locationId: String) {
        if (startingSearchView.hasFocus()) {
            startingPointLocation = getLocationById(locationId)
            startingSearchView.setQuery(startingPointLocation?.title, false)
            startingSearchView.clearFocus()
        }
        if (destinationSearchView.hasFocus()) {
            destinationPointLocation = getLocationById(locationId)
            destinationSearchView.setQuery(destinationPointLocation?.title, false)
            destinationSearchView.clearFocus()
        }
        filteredLocationList = mutableListOf()
        binding.llAutocompleteLocationList.visibility = View.GONE
    }

    // matrix profile selection
    private fun matrixProfileChipSelection() {
        val profileWalking: Chip = binding.matrixProfileWalking
        val profileDriving: Chip = binding.matrixProfileDriving

        profileWalking.setOnClickListener {
            displayDistanceResults(0.0, 0.0)

            profileWalking.chipBackgroundColor =
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.sandy_brown_800))
            profileDriving.chipBackgroundColor =
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.light_gray))

            val profileText = profileWalking.text.toString().lowercase()
            mapboxMatrixProfile = profileText
        }

        profileDriving.setOnClickListener {
            displayDistanceResults(0.0, 0.0)

            profileDriving.chipBackgroundColor =
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.sandy_brown_800))
            profileWalking.chipBackgroundColor =
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.light_gray))

            val profileText = profileDriving.text.toString().lowercase()
            mapboxMatrixProfile = profileText
        }
    }

    // starting to destination points distance calculation
    private fun startingToDestinationDistance(
        startingPointLocation: Location, destinationPointLocation: Location
    ) {
        binding.tvDistanceCalculation.isClickable = false
        binding.llProgressBar.visibility = View.VISIBLE
        displayDistanceResults(0.0, 0.0)

        val startingLocation =
            "${startingPointLocation.latitude},${startingPointLocation.longitude}"
        val destinationLocation =
            "${destinationPointLocation.latitude},${destinationPointLocation.longitude}"
        val coordinates = "$startingLocation;$destinationLocation"

        val parameters = HashMap<String, String>()
        parameters["sources"] = "0"
        parameters["destinations"] = "all"
        parameters["annotations"] = "distance,duration"
        parameters["access_token"] = getString(R.string.mapbox_access_token)

        val mapboxApi = RetrofitBuilder.buildApi(MapboxApi::class.java)
        val requestCall =
            mapboxApi.getDirectionsMatrix(mapboxMatrixProfile, coordinates, parameters)
        requestCall.enqueue(object : Callback<DirectionsMatrix> {
            override fun onResponse(
                call: Call<DirectionsMatrix>, response: Response<DirectionsMatrix>
            ) {
                binding.tvDistanceCalculation.isClickable = true
                binding.llProgressBar.visibility = View.GONE

                if (response.isSuccessful && response.body()?.code == MapboxApi.MAPBOX_MATRIX_SUCCESS) {
                    val directionsMatrix = response.body()
                    displayDistanceResults(
                        directionsMatrix!!.distances[0][1], directionsMatrix.durations[0][1]
                    )
                } else if (response.isSuccessful && response.body()?.code == MapboxApi.MAPBOX_MATRIX_NO_ROUTE) {
                    showErrorSnackBar("No route found")
                } else {
                    showErrorSnackBar("Something went wrong")
                }
            }

            override fun onFailure(call: Call<DirectionsMatrix>, t: Throwable) {
                binding.tvDistanceCalculation.isClickable = true
                binding.llProgressBar.visibility = View.GONE
                showErrorSnackBar("Something went wrong")
            }

        })
    }

    // display destination calculations
    private fun displayDistanceResults(distance: Double, duration: Double) {
        val directionsDistance = locationDistanceConverter(distance)
        val directionsDuration = locationDurationConverter(duration)
        val dayHourMin = directionsDuration.split(":")

        val distanceNumber = directionsDistance.replace("[^0-9]".toRegex(), "")
        val distanceText = directionsDistance.replace("[^a-zA-Z]".toRegex(), "")
        val day = dayHourMin[0]
        val hour = dayHourMin[1]
        val minute = dayHourMin[2]

        binding.tvDistanceNumber.text = distanceNumber
        binding.tvDistanceText.text = distanceText
        binding.tvDayNumber.text = day
        binding.tvHourNumber.text = hour
        binding.tvMinuteNumber.text = minute
    }

    // location query filter
    private fun locationQueryFilter() {
        startingSearchView = binding.locationSearchSource
        destinationSearchView = binding.locationSearchDestination
        adapter = AutocompleteLocationListAdapter(filteredLocationList, context)

        binding.rvLocationList.adapter = adapter
        binding.rvLocationList.layoutManager = LinearLayoutManager(context)
        binding.rvLocationList.setHasFixedSize(true)

        binding.llAutocompleteSearchBox.setOnClickListener {
            startingSearchView.clearFocus()
            destinationSearchView.clearFocus()
        }

        startingSearchView.setOnQueryTextFocusChangeListener { _, b ->
            if (b) {
                val text = startingSearchView.query.toString()
                filterLocationList(text)
                binding.llAutocompleteLocationList.visibility = View.VISIBLE
            } else {
                binding.llAutocompleteLocationList.visibility = View.GONE
            }
        }
        destinationSearchView.setOnQueryTextFocusChangeListener { _, b ->
            if (b) {
                val text = destinationSearchView.query.toString()
                filterLocationList(text)
                binding.llAutocompleteLocationList.visibility = View.VISIBLE
            } else {
                binding.llAutocompleteLocationList.visibility = View.GONE
            }
        }

        startingSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null && newText.trim().isEmpty()) {
                    startingPointLocation = null
                }
                filterLocationList(newText)
                return true
            }
        })
        destinationSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null && newText.trim().isEmpty()) {
                    destinationPointLocation = null
                }
                filterLocationList(newText)
                return false
            }
        })
    }

    // location list filter
    private fun filterLocationList(text: String?) {
        if (text != null && text.trim().isNotEmpty()) {
            filteredLocationList = locationList.filter {
                it.title.lowercase().contains(text.lowercase().trim())
            } as MutableList<Location>
            filteredLocationList.sortBy { it.title }
        } else {
            filteredLocationList = mutableListOf()
        }
        adapter.filterList(filteredLocationList)
    }

    // setup action bar
    private fun setupActionBar() {
        setSupportActionBar(binding.tbDistance)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = TOOL_BAR_TITLE

        binding.tbDistance.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    companion object {
        const val TOOL_BAR_TITLE = "Distance"
    }
}