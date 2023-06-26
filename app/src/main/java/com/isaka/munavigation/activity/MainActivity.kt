package com.isaka.munavigation.activity

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.isaka.munavigation.R
import com.isaka.munavigation.databinding.ActivityMainBinding
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.CameraAnimatorOptions.Companion.cameraAnimatorOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import kotlin.math.roundToInt

class MainActivity : BaseActivity() {
    private val context = this
    private lateinit var binding: ActivityMainBinding
    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private var myLocationPoint: Point? = null
    private var extraLongLat: String? = null

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        myLocationPoint = it
    }

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {}

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            val myLocationBtn = binding.myLocationBtn
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
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // initialize the mapbox
        mapView = binding.mapView
        mapboxMap = mapView.getMapboxMap()

        // bottom sheet behavior
        val mainBottomSheet = findViewById<View>(R.id.main_bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(mainBottomSheet)
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)

        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val slideBack = ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.TRANSLATION_X,
                0f,
                -splashScreenView.view.width.toFloat()
            ).apply {
                interpolator = DecelerateInterpolator()
                duration = 1000L
                doOnEnd {
                    splashScreenView.remove()
                    // mapbox map ready
                    onMapReady()
                }
            }

            Handler(Looper.getMainLooper()).postDelayed({
                slideBack.start()
            }, 3000)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                doubleBackToExit()
            }
        })

        // set my location LongLat
        binding.myLocationBtn.setOnClickListener {
            if (myLocationPoint != null) {
                extraLongLat = "${myLocationPoint!!.longitude()},${
                    myLocationPoint!!.latitude()
                }"
                myLocation(myLocationPoint!!)
            }
        }

        // move to distance activity
        binding.btnDistance.setOnClickListener {
            val intent = Intent(this, DistanceActivity::class.java)
            startActivity(intent)
        }

        // move to settings activity
        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // move to direction activity
        binding.btnDirection.setOnClickListener {
            val intent = Intent(this, DirectionActivity::class.java)
            startActivity(intent)
        }

        // move to location activity
        val categoryVenue = findViewById<View>(R.id.iv_category_venue)
        val categoryHostel = findViewById<View>(R.id.iv_category_hostel)
        val categoryOffice = findViewById<View>(R.id.iv_category_office)

        categoryVenue.setOnClickListener {
            val intent = Intent(this, LocationActivity::class.java)
            intent.putExtra(EXTRA_ACTION_BAR_TITLE, "Venue")
            startActivity(intent)
        }
        categoryHostel.setOnClickListener {
            val intent = Intent(this, LocationActivity::class.java)
            intent.putExtra(EXTRA_ACTION_BAR_TITLE, "Hostel")
            startActivity(intent)
        }
        categoryOffice.setOnClickListener {
            val intent = Intent(this, LocationActivity::class.java)
            intent.putExtra(EXTRA_ACTION_BAR_TITLE, "Office")
            startActivity(intent)
        }

        // move to category activity
        val categoryMore = findViewById<View>(R.id.iv_category_more)
        categoryMore.setOnClickListener {
            val intent = Intent(this, CategoryActivity::class.java)
            startActivity(intent)
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
                        val zoom = createZoomAnimator(cameraAnimatorOptions(CAMERA_ZOOM) {
                            startValue(CAMERA_ZOOM_START)
                        }) {
                            duration = 4000
                            interpolator = AccelerateDecelerateInterpolator()
                        }
                        playAnimatorsSequentially(zoom)
                    }
                    initLocationComponent()
                }
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

    private fun myLocation(point: Point) {
        val zooLevel = mapboxMap.coordinateBoundsZoomForCamera(
            CameraOptions.Builder().center(point).build()
        ).zoom

        mapView.camera.apply {
            val zoom = createZoomAnimator(cameraAnimatorOptions(CAMERA_ZOOM) {
                startValue(zooLevel)
            }) {
                duration = 3000
                interpolator = AccelerateDecelerateInterpolator()
            }

            val move = createCenterAnimator(
                cameraAnimatorOptions(point)
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

    override fun onDestroy() {
        super.onDestroy()
        mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        bottomSheetBehavior.removeBottomSheetCallback(bottomSheetCallback)
    }

    companion object {
        const val EXTRA_ACTION_BAR_TITLE = "extra_action_bar_title"
        private const val CAMERA_ZOOM = 18.0
        private const val CAMERA_ZOOM_START = 11.0
        private val CAMERA_TARGET = cameraOptions {
            center(Point.fromLngLat(37.5674841029512, -6.926732194474198))
            zoom(CAMERA_ZOOM)
        }
    }
}