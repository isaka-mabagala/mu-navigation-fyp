package com.isaka.munavigation.activity

import android.Manifest
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.isaka.munavigation.R
import com.isaka.munavigation.databinding.ActivityDirectionBinding
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
import com.mapbox.maps.plugin.animation.CameraAnimatorOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location

class DirectionActivity : BaseActivity() {
    private val context = this
    private lateinit var binding: ActivityDirectionBinding
    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private var myLocationPoint: Point? = null

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        myLocationPoint = it
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDirectionBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // toolbar setup
        setupActionBar()

        // initialize the mapbox
        mapView = binding.mapView
        mapboxMap = mapView.getMapboxMap()
        onMapReady()

        // set estimation box
        binding.ivDirectionImage.setImageResource(R.drawable.ic_directions_walk)
        binding.tvDirectionEstimate.text = "16.48 km, hr 3:min 13 to arrive"

        // my location
        binding.ivMyLocation.setOnClickListener {
            if (myLocationPoint != null) {
                myLocation(myLocationPoint!!)
            }
        }

        // direction view bottom sheet
        binding.ivDirectionView.setOnClickListener {
            showBottomSheetDirectionView()
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
            val zoom = createZoomAnimator(CameraAnimatorOptions.cameraAnimatorOptions(CAMERA_ZOOM) {
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

    private fun showBottomSheetDirectionView() {
        val bottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetTransparent)
        bottomSheetDialog.setContentView(R.layout.map_view_bottom_sheet)

        bottomSheetDialog.findViewById<View>(R.id.iv_street_view)?.setOnClickListener {
            showInfoSnackBar("Street view clicked")
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.findViewById<View>(R.id.iv_ar_view)?.setOnClickListener {
            showInfoSnackBar("Augmented reality view clicked")
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show();
    }

    // setup action bar
    private fun setupActionBar() {
        setSupportActionBar(binding.tbDirection)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = TOOL_BAR_TITLE

        binding.tbDirection.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
    }

    companion object {
        const val TOOL_BAR_TITLE = "Direction"
        private const val CAMERA_ZOOM = 18.0
        private const val CAMERA_ZOOM_START = 11.0
        private val CAMERA_TARGET = cameraOptions {
            center(Point.fromLngLat(37.5674841029512, -6.926732194474198))
            zoom(CAMERA_ZOOM)
        }
    }
}