package com.shrestha.diabeatit.ui.map

import android.Manifest
import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.shrestha.diabeatit.ApiInterface
import com.shrestha.diabeatit.R
import com.shrestha.diabeatit.models.PlacesApiResponse
import com.shrestha.diabeatit.models.Result
import com.shrestha.diabeatit.utils.ServiceBuilder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.Math.*
import kotlin.math.sqrt


@Suppress("UNREACHABLE_CODE", "DEPRECATION")
class MapFragment : Fragment(), SensorEventListener, LocationListener {

    private lateinit var locationManager: LocationManager
    private var isNewInstance: Boolean = true
    private var clueCount = 0
    private val locationPermissionCode = 2

    private var locationListGlobal: MutableList<LatLng> = arrayListOf()
    private var mMap: GoogleMap? = null
    private val LOCATION_PERMISSION_REQUEST_CODE: Int = 100
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    val polyList: MutableList<LatLng> = arrayListOf()
    private val boundList: MutableList<LatLng> = arrayListOf()
    private var currentLatLng: LatLng? = null
    private var landmarkXY: LatLng? = null
    var locationCallback: LocationCallback? = null
    private var mainMarker: Marker? = null
    private var landmarkMarker: Marker? = null
    private var txtSteps: TextView? = null
    private var imgRefresh: ImageView? = null
    private lateinit var sensorManager: SensorManager
    private lateinit var stepCounterSensor: Sensor
    private var stepCount = 0


    private val callback = OnMapReadyCallback { googleMap ->
        mMap = googleMap
        getCurrentLocation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {


        val v = inflater.inflate(R.layout.fragment_map, container, false)
        locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        txtSteps = v.findViewById(R.id.txtSteps)


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        getLocationWithGPS()


        //24.920801,67.046995
//        requestLocationPermission()

        if (isNewInstance)
            showInitialAlert() //initialWasHere

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        return v
    }

    private fun getLocationWithGPS() {
        locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if ((ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1f, this)
        }
    }


    override fun onPause() {
        super.onPause()
        isNewInstance = false
        mainMarker?.remove()
        mainMarker = null
    }


    private fun displayMetrics(dialog: Dialog): DisplayMetrics {
        val displayMetrics = DisplayMetrics()
        dialog.window!!.windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics
    }


//    override fun onStop() {
//        super.onStop()
//
//        stopGettingLocations()
//    }

    private fun addMarkers(list: List<Result>) {
        val locationList: MutableList<LatLng> = arrayListOf()
        for (l in list) {
            Log.d(
                TAG, "list: " + l.name + "-> " + l.geometry.location.toString()
            )
            //here to add markers
            val location = LatLng(l.geometry.location.lat, l.geometry.location.lng)


            locationList.add(location)

        }
        addDestinationMarkers(locationList)


    }

    private fun addDestinationMarkers(locationList: MutableList<LatLng>) {
        val nearestLtlng: LatLng = findNearestLatLng(locationList, currentLatLng!!)!!

        locationListGlobal = locationList
        landmarkMarker = mMap?.addMarker(
            MarkerOptions().position(nearestLtlng)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ellipse_1))
        )
        landmarkXY = nearestLtlng

        boundList.add(nearestLtlng)
        boundList.add(currentLatLng!!)
        setBounds(boundList)
    }

    private fun findNearestLatLng(latlngList: List<LatLng>, targetLatLng: LatLng): LatLng? {
        var nearestLatLng: LatLng? = null
        var nearestDistance = Double.MAX_VALUE

        for (latLng in latlngList) {
            val distance = distanceBetweenLatLngs(latLng, targetLatLng)

            if (distance < nearestDistance) {
                nearestDistance = distance
                nearestLatLng = latLng
            }
        }

        return nearestLatLng
    }

    private fun distanceBetweenLatLngs(latLng1: LatLng, latLng2: LatLng): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers
        val lat1 = toRadians(latLng1.latitude)
        val lat2 = toRadians(latLng2.latitude)
        val lng1 = toRadians(latLng1.longitude)
        val lng2 = toRadians(latLng2.longitude)

        val dLat = lat2 - lat1
        val dLng = lng2 - lng1

        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(lat1) * kotlin.math.cos(lat2) *
                kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
        val c = 2 * kotlin.math.atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }


    private fun setBounds(boundList: MutableList<LatLng>) {
        val builder = LatLngBounds.Builder()

        for (l in boundList) {
            builder.include(l)
        }

        val bounds = builder.build()
        try {
            val width = resources.displayMetrics.widthPixels
            val height = resources.displayMetrics.heightPixels
            val padding = (width * 0.38).toInt()


            if (mainMarker == null) {
                val m = MarkerOptions().position(currentLatLng!!).title("Current Location")
                    .icon(
                        BitmapDescriptorFactory.fromResource(R.drawable.sherlock_holmes_1)
                    )
                mainMarker = mMap?.addMarker(m)
            }
            val cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding)
            mMap!!.animateCamera(cu)

        } catch (i: java.lang.IllegalStateException) {

        }


    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371 // Radius of the earth in km
        val dLat = deg2rad(lat2 - lat1)
        val dLon = deg2rad(lon2 - lon1)
        val a =
            sin(dLat / 2) * sin(dLat / 2) + cos(deg2rad(lat1)) * kotlin.math.cos(deg2rad(lat2)) * sin(
                dLon / 2
            ) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(sqrt(a), sqrt(1 - a))
        val d = R * c * 1000 // Distance in meters
        return d
    }

    private fun deg2rad(deg: Double): Double {
        return deg * (PI / 180)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)


    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission has already been granted
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            Log.d(TAG, "onLocation:$location")

            if (location != null) {

                if (location.accuracy >= 5) {
                    Toast.makeText(
                        requireContext(),
                        "Launch App in Open Area\nFor Better Accuracy",
                        Toast.LENGTH_LONG
                    ).show()
                }

                currentLatLng = LatLng(location.latitude, location.longitude)


                polyList.add(currentLatLng!!)
                if (mainMarker == null) {
                    val m = MarkerOptions().position(currentLatLng!!).title("Current Location")
                        .icon(
                            BitmapDescriptorFactory.fromResource(R.drawable.sherlock_holmes_1)
                        )
                    mainMarker = mMap?.addMarker(m)
                }

                mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng!!, 18f))
                updateInitialMarkers(currentLatLng!!)
            }

        } else {
            Log.d(TAG, "getCurrentLocation: ")
        }
    }

    private fun updateInitialMarkers(currentLatLng: LatLng) {

        val response = ServiceBuilder.buildService(ApiInterface::class.java)

        Log.d(TAG, "Current LatLng: $currentLatLng")

        val latLng = currentLatLng.latitude.toString() + "," + currentLatLng.longitude.toString()

        response.sendReq(latLng, "200", getString(R.string.EVERY_API_KEY))
            .enqueue(object : Callback<PlacesApiResponse> {
                override fun onResponse(
                    call: Call<PlacesApiResponse>, response: Response<PlacesApiResponse>
                ) {

                    Log.d(TAG, "onResponse: " + response.body()!!.status)

                    if (response.body()!!.status == "OK") {

                        val responseFromApi = response
                        Log.d(TAG, "ResponseJSON: " + responseFromApi.body().toString())

                        val list = responseFromApi.body()!!.results

                        addMarkers(list)


                    } else {
                        Toast.makeText(
                            requireContext(), response.body()!!.status, Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<PlacesApiResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), t.toString(), Toast.LENGTH_LONG).show()
                }

            })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            locationPermissionCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, perform your action here
                    getLocationWithGPS()
                } else {
                    // Permission denied, handle the situation here
                    Toast.makeText(
                        requireContext(),
                        "Location permission denied",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun level2() {

        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.clue_1_layout)
        dialog.setCancelable(false)
        val height: Int = displayMetrics(dialog).heightPixels
        val width: Int = displayMetrics(dialog).widthPixels
        dialog.window!!.setLayout((width), (height))
        val txtNext: TextView = dialog.findViewById(R.id.txtNext)

        txtNext.setOnClickListener {

            val latLng = landmarkXY
            landmarkMarker = mMap?.addMarker(
                MarkerOptions().position(latLng!!)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.tick))
            )

            saveLandMarkInMemory()
            dialog.dismiss()

            showNextLandmark()

        }

        dialog.show()

    }

    private fun level3() {

        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.clue_2_layout)
        dialog.setCancelable(false)
        val height: Int = displayMetrics(dialog).heightPixels
        val width: Int = displayMetrics(dialog).widthPixels
        dialog.window!!.setLayout((width), (height))
        val txtNext: TextView = dialog.findViewById(R.id.txtNext)

        txtNext.setOnClickListener {

            if (landmarkXY != null) {
                val latLng = landmarkXY
                landmarkMarker = mMap?.addMarker(
                    MarkerOptions().position(latLng!!)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.tick))
                )
            }


            saveLandMarkInMemory()
            dialog.dismiss()

            showQAAlert()

        }

        dialog.show()

    }

    private fun showQAAlert() {

        //How much vegetables and fruits should you consume daily?

        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.clue_2_qa_layout)
        dialog.setCancelable(false)
        val height: Int = displayMetrics(dialog).heightPixels
        val width: Int = displayMetrics(dialog).widthPixels
        dialog.window!!.setLayout((width), (height))
        val btn50: Button = dialog.findViewById(R.id.btn50)
        val btn500: Button = dialog.findViewById(R.id.btn500)

        btn500.setOnClickListener {

            showCorrectAlert()
            dialog.cancel()

        }

        btn50.setOnClickListener {
            btn50.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.red)
        }

        dialog.show()

    }

    private fun showCorrectAlert() {

        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.correct_layout)
        dialog.setCancelable(false)
        val height: Int = displayMetrics(dialog).heightPixels
        val width: Int = displayMetrics(dialog).widthPixels
        dialog.window!!.setLayout((width), (height))
        dialog.show()

        Handler().postDelayed({
            showFinalClueAlert()
            dialog.cancel()

        }, 1100)

    }

    private fun showFinalClueAlert() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.final_clue_layout)
        dialog.setCancelable(false)
        val height: Int = displayMetrics(dialog).heightPixels
        val width: Int = displayMetrics(dialog).widthPixels
        dialog.window!!.setLayout((width), (height))
        val btnCancel = dialog.findViewById<ImageView>(R.id.imgCancel)
        btnCancel.setOnClickListener {

            dialog.dismiss()

            showNextLandmark()
            dialog.cancel()
        }
        dialog.show()

    }

    private fun saveLandMarkInMemory() {


    }

    private fun showInitialAlert() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.menu_daily_mission)
        dialog.findViewById<ImageView>(R.id.imgCancel).setOnClickListener {
            dialog.cancel()
        }

        dialog.show()

    }

    private fun updateLandmarkMarker() {

        landmarkMarker?.remove()
        val latLng = landmarkXY
        landmarkMarker = mMap?.addMarker(
            MarkerOptions().position(latLng!!)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.q_mark))
        )
        clueCount++

        when (clueCount) {
            1 -> {
                level2()
            }

            2 -> {
                level3()
                //replacing level3 with initial

            }
            3 -> {
                level4Congrates()
            }
        }
    }

    private fun level4Congrates() {

        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.congratulations_alert)
        dialog.setCancelable(false)

        val txtNext: ImageView = dialog.findViewById(R.id.imgCancel)
        val txtYouFound: TextView = dialog.findViewById(R.id.txtYouFound)
        txtYouFound.text = "You:\nfound Out 1 Clue\nWalked $stepCount steps"

        txtNext.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

    }


    private fun showNextLandmark() {

        removeOldLandMark()
        val nearestLtlng: LatLng = findNearestLatLng(locationListGlobal, currentLatLng!!)!!

        landmarkMarker = mMap?.addMarker(
            MarkerOptions().position(nearestLtlng)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ellipse_1))
        )
        landmarkXY = nearestLtlng

        boundList.add(nearestLtlng)
        setBounds(boundList)

    }

    private fun removeOldLandMark() {
        locationListGlobal.remove(landmarkXY)
    }

    private fun updateCurrentMarkerPosition(currentLatLng: LatLng) {
        if (mainMarker != null)
            mainMarker!!.position = currentLatLng
    }

    fun drawPolyLineOnMap(list: List<LatLng?>?, mMap: GoogleMap) {
        try {
            val polyOptions = PolylineOptions()
            polyOptions.color(resources.getColor(R.color.theme_blue))
            polyOptions.width(8f)
            polyOptions.addAll(list!!)
            mMap.addPolyline(polyOptions)


        } catch (e: java.lang.IllegalStateException) {

        }

    }

    override fun onSensorChanged(p0: SensorEvent?) {

        if (p0?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            stepCount = p0.values[0].toInt()
            txtSteps!!.text = stepCount.toString()
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        TODO("Not yet implemented")
    }

    fun calculateSteps(mutableList: MutableList<LatLng>): String {
        try {
            val currentLat = mutableList[mutableList.size - 1]
            val oldLat = mutableList[mutableList.size - 2]

            val steps = calculateDistance(
                currentLat.latitude,
                currentLat.longitude,
                oldLat.latitude,
                oldLat.longitude,
            )

            Log.d(TAG, "calculateSteps: $steps")
            return (Math.round(steps)).toString()
        } catch (exc: ArrayIndexOutOfBoundsException) {
            return "0"
        }

    }

    override fun onLocationChanged(p0: Location) {


        Log.d(TAG, "onLocationResult: $p0")

//        if (p0.accuracy <= 9) {


        currentLatLng = LatLng(p0.latitude, p0.longitude)


        Log.d(TAG, "onLocationChanged: $currentLatLng")
        polyList.add(currentLatLng!!)

        drawPolyLineOnMap(polyList, mMap!!)

        updateCurrentMarkerPosition(currentLatLng!!)
        val stepsChanged: Int = calculateSteps(polyList).toInt()
        stepCount += stepsChanged
        txtSteps!!.text = stepCount.toString()

        val dis = landmarkXY?.latitude?.let {
            landmarkXY?.longitude?.let { it1 ->
                calculateDistance(
                    currentLatLng!!.latitude,
                    currentLatLng!!.longitude,
                    it,
                    it1
                )
            }
        }

        Log.d(TAG, "onLocationResult Distance: $dis")

        if (dis != null) {
            if (dis < 6) {
                updateLandmarkMarker()
            }
        }

//        }
    }

}