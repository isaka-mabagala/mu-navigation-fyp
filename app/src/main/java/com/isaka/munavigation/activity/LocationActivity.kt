package com.isaka.munavigation.activity

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.SearchView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.isaka.munavigation.R
import com.isaka.munavigation.adapter.LocationListAdapter
import com.isaka.munavigation.api.MapboxApi
import com.isaka.munavigation.api.MapboxApi.Companion.MAPBOX_MATRIX_SUCCESS
import com.isaka.munavigation.api.MapboxApi.Companion.MAPBOX_MATRIX_WALKING_PROFILE
import com.isaka.munavigation.api.RetrofitBuilder
import com.isaka.munavigation.databinding.ActivityLocationBinding
import com.isaka.munavigation.databinding.ItemCallOutViewBinding
import com.isaka.munavigation.model.DirectionsMatrix
import com.isaka.munavigation.model.Location
import com.isaka.munavigation.util.Mapbox.bitmapFromDrawableRes
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.CameraAnimatorOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.viewannotation.ViewAnnotationManager
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.roundToInt

class LocationActivity : BaseActivity(), LocationListAdapter.OnItemClickListener {
    private val context = this
    private lateinit var binding: ActivityLocationBinding
    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var viewAnnotationManager: ViewAnnotationManager
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var mViewAnnotation: View
    private lateinit var searchView: SearchView
    private lateinit var adapter: LocationListAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private var locationList: MutableList<Location> = mutableListOf()
    private var myLocationPoint: Point? = null
    private var locationCategory: String? = null

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        myLocationPoint = it
    }

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            val myLocationBtn = binding.fbMyLocation
            if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                myLocationBtn.show()
                searchView.clearFocus()
            }

            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                myLocationBtn.hide()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            val myLocationBtn = binding.fbMyLocation
            val slideOffsetNum: Double = (slideOffset * 100.0).roundToInt() / 100.0

            // hide my location button
            if (slideOffsetNum > 0.1) {
                myLocationBtn.hide()
            } else {
                myLocationBtn.show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        searchView = binding.locationSearch

        // toolbar setup
        if (intent.hasExtra(MainActivity.EXTRA_ACTION_BAR_TITLE)) {
            val title = intent.getStringExtra(MainActivity.EXTRA_ACTION_BAR_TITLE)
            setupActionBar(title!!)
            locationCategory = title.lowercase()
        }

        // initialize the mapbox
        mapView = binding.mapView
        mapboxMap = mapView.getMapboxMap()
        viewAnnotationManager = mapView.viewAnnotationManager
        pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
        onMapReady()

        // my location
        binding.fbMyLocation.setOnClickListener {
            if (myLocationPoint != null) {
                setCameraLocation(myLocationPoint!!)
            }
        }

        // location query filter
        searchView.setOnQueryTextFocusChangeListener { _, b ->
            if (b) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    filterLocationList(newText)
                }
                return true
            }
        })

        // bottom sheet behavior
        val locationBottomSheet = findViewById<View>(R.id.location_bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(locationBottomSheet)
        bottomSheetBehavior.peekHeight = 600
        bottomSheetBehavior.maxHeight = resources.displayMetrics.heightPixels - 400
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)
    }

    override fun onItemClick(locationId: String) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        val location = locationList.filter { location -> location.id == locationId }[0]
        val point = Point.fromLngLat(location.longitude, location.latitude)
        val viewAnnotation =
            viewAnnotationManager.getViewAnnotationByFeatureId(location.featureIdentifier!!)
        viewAnnotation?.toggleViewVisibility()
        setCameraLocation(point)
    }

    private fun addAnnotationMarkers(locations: List<Location>): List<Location> {
        locations.forEach { location ->
            val pointAnnotation = addAnnotationMarker(location)
            location.featureIdentifier = pointAnnotation.featureIdentifier
        }

        return locations
    }

    private fun addViewAnnotation(location: Location, pointAnnotation: PointAnnotation) {
        val point = Point.fromLngLat(location.longitude, location.latitude)

        val viewAnnotation = viewAnnotationManager.addViewAnnotation(
            resId = R.layout.item_call_out_view,
            options = viewAnnotationOptions {
                geometry(point)
                associatedFeatureId(pointAnnotation.featureIdentifier)
                anchor(ViewAnnotationAnchor.BOTTOM)
                offsetY((pointAnnotation.iconImageBitmap?.height!!).toInt())
            })
        mViewAnnotation = viewAnnotation

        ItemCallOutViewBinding.bind(viewAnnotation).apply {
            tvTitle.text = location.title
            tvDescription.text = location.description

            ivClose.setOnClickListener {
                viewAnnotation.visibility = View.GONE
            }

            btnDirection.setOnClickListener {
                // TODO ("pass data to intent")
                val intent = Intent(context, DirectionActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun addAnnotationMarker(location: Location): PointAnnotation {
        val point = Point.fromLngLat(location.longitude, location.latitude)
        val iconBitmap: Bitmap = bitmapFromDrawableRes(this, R.drawable.blue_marker_view)!!

        val pointAnnotationOptions: PointAnnotationOptions =
            PointAnnotationOptions().withPoint(point).withIconImage(iconBitmap)
                .withTextField(location.title).withTextColor(getColor(R.color.gunmetal_800))
                .withTextSize(10.0).withIconAnchor(IconAnchor.BOTTOM).withDraggable(false)

        val pointAnnotation = pointAnnotationManager.create(pointAnnotationOptions)
        addViewAnnotation(location, pointAnnotation)

        return pointAnnotation
    }

    private fun View.toggleViewVisibility() {
        mViewAnnotation.visibility = View.GONE
        visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
        mViewAnnotation = this
    }

    private fun populateLocationListToUi() {
        if (myLocationPoint == null) {
            binding.llProgressBar.visibility = View.GONE
            binding.tvNoLocation.visibility = View.VISIBLE
            showErrorSnackBar("Something went wrong")
            return
        }

        locationList = getLocationListByCategory(locationCategory!!) as MutableList<Location>
        val mapboxApi = RetrofitBuilder.buildApi(MapboxApi::class.java)

        val parameters = HashMap<String, String>()
        parameters["sources"] = "0"
        parameters["destinations"] = "all"
        parameters["annotations"] = "distance,duration"
        parameters["access_token"] = getString(R.string.mapbox_access_token)

        // set location coordinates
        val userLocation =
            "${myLocationPoint?.latitude().toString()},${myLocationPoint?.longitude().toString()}"
        var destinationCoordinates = ""
        locationList.forEachIndexed { index, location ->
            destinationCoordinates += if (index == locationList.size - 1) {
                "${location.latitude},${location.longitude}"
            } else {
                "${location.latitude},${location.longitude};"
            }
        }
        val coordinates = "$userLocation;$destinationCoordinates"

        if (locationList.isNotEmpty()) {
            // walking distance coordinates request
            val requestCall = mapboxApi.getDirectionsMatrix(
                MAPBOX_MATRIX_WALKING_PROFILE,
                coordinates,
                parameters
            )
            requestCall.enqueue(object : Callback<DirectionsMatrix> {
                override fun onResponse(
                    call: Call<DirectionsMatrix>, response: Response<DirectionsMatrix>
                ) {
                    binding.llProgressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.code == MAPBOX_MATRIX_SUCCESS) {
                        // assign distance to locations
                        response.body()!!.distances[0].forEachIndexed { index, distance ->
                            if (index > 0) {
                                locationList[index - 1].distance = distance
                            }
                        }

                        // add markers to map
                        locationList = addAnnotationMarkers(locationList) as MutableList<Location>

                        // set locations to adapter
                        binding.llLocationListContainer.visibility = View.VISIBLE
                        locationList.sortBy { it.title }
                        adapter = LocationListAdapter(locationList, context)
                        binding.rvLocationList.adapter = adapter
                        binding.rvLocationList.layoutManager = LinearLayoutManager(context)
                        binding.rvLocationList.setHasFixedSize(true)

                    } else {
                        binding.tvNoLocation.visibility = View.VISIBLE
                        showErrorSnackBar("Something went wrong")
                    }
                }

                override fun onFailure(call: Call<DirectionsMatrix>, t: Throwable) {
                    binding.llProgressBar.visibility = View.GONE
                    binding.tvNoLocation.visibility = View.VISIBLE
                    showErrorSnackBar("Something went wrong")
                }

            })
        } else {
            binding.llProgressBar.visibility = View.GONE
            binding.tvNoLocation.visibility = View.VISIBLE
        }
    }

    private fun filterLocationList(text: String) {
        val filteredList = locationList.filter { it.title.lowercase().contains(text.lowercase()) }
        adapter.filterList(filteredList)

        if (filteredList.isNotEmpty()) {
            binding.tvNoLocation.visibility = View.GONE
        } else {
            binding.tvNoLocation.visibility = View.VISIBLE
        }
    }

    private fun onMapReady() {
        // get user permissions
        Dexter.withContext(this).withPermissions(
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                // permission granted
                mapboxMap.setCamera(CAMERA_TARGET)
                mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
                    mapView.camera.apply {
                        val zoom = createZoomAnimator(CameraAnimatorOptions.cameraAnimatorOptions(
                            CAMERA_ZOOM
                        ) {
                            startValue(CAMERA_ZOOM_START)
                        }) {
                            duration = 3000
                            interpolator = AccelerateDecelerateInterpolator()
                        }
                        playAnimatorsSequentially(zoom)
                    }

                    // show or hide view annotation based on a marker click
                    pointAnnotationManager.addClickListener { clickedAnnotation ->
                        clickedAnnotation.apply {
                            val viewAnnotation =
                                viewAnnotationManager.getViewAnnotationByFeatureId(this.featureIdentifier)
                            viewAnnotation?.toggleViewVisibility()
                        }
                        true
                    }

                    initLocationComponent()
                }

                // binding location list adapter
                Handler(Looper.getMainLooper()).postDelayed({
                    populateLocationListToUi()
                }, 2000)
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?, token: PermissionToken?
            ) {
                showRationalDialogForPermissions()
            }
        }).onSameThread().check()
    }

    private fun initLocationComponent() {
        val locationComponentPlugin = mapView.location
        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                bearingImage = AppCompatResources.getDrawable(
                    context,
                    R.drawable.mapbox_user_puck_icon,
                ), shadowImage = AppCompatResources.getDrawable(
                    context,
                    R.drawable.mapbox_user_icon_shadow,
                ), scaleExpression = interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(20.0)
                        literal(1.0)
                    }
                }.toJson()
            )
        }
        locationComponentPlugin.addOnIndicatorPositionChangedListener(
            onIndicatorPositionChangedListener
        )
    }

    private fun setCameraLocation(point: Point) {
        val zooLevel = mapboxMap.coordinateBoundsZoomForCamera(
            CameraOptions.Builder().center(point).build()
        ).zoom

        mapView.camera.apply {
            val zoom = createZoomAnimator(CameraAnimatorOptions.cameraAnimatorOptions(
                CAMERA_ZOOM
            ) {
                startValue(zooLevel)
            }) {
                duration = 3000
                interpolator = AccelerateDecelerateInterpolator()
            }

            val move = createCenterAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(point)
            ) {
                duration = 3000
            }

            if (zooLevel.toInt() == CAMERA_ZOOM.toInt()) {
                playAnimatorsSequentially(move, zoom)
            } else {
                playAnimatorsSequentially(zoom, move)
            }
        }

        mapView.gestures.focalPoint = mapboxMap.pixelForCoordinate(point)
    }

    // setup action bar
    private fun setupActionBar(title: String) {
        setSupportActionBar(binding.tbLocation)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = title

        binding.tbLocation.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        bottomSheetBehavior.removeBottomSheetCallback(bottomSheetCallback)
    }

    companion object {
        private const val CAMERA_ZOOM = 18.0
        private const val CAMERA_ZOOM_START = 11.0
        private val CAMERA_TARGET = cameraOptions {
            center(Point.fromLngLat(37.5674841029512, -6.926732194474198))
            zoom(CAMERA_ZOOM)
        }
    }
}