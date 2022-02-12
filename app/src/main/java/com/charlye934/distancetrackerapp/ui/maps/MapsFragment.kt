package com.charlye934.distancetrackerapp.ui.maps

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Camera
import android.graphics.Color
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.charlye934.distancetrackerapp.R
import com.charlye934.distancetrackerapp.databinding.FragmentMapsBinding
import com.charlye934.distancetrackerapp.model.Result
import com.charlye934.distancetrackerapp.service.TrackerService
import com.charlye934.distancetrackerapp.util.Constants
import com.charlye934.distancetrackerapp.util.ExtensionFunctions.disable
import com.charlye934.distancetrackerapp.util.ExtensionFunctions.enable
import com.charlye934.distancetrackerapp.util.ExtensionFunctions.hide
import com.charlye934.distancetrackerapp.util.ExtensionFunctions.show
import com.charlye934.distancetrackerapp.util.Permissions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*

import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, EasyPermissions.PermissionCallbacks, GoogleMap.OnMarkerClickListener {

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!

    private lateinit var map: GoogleMap

    val started = MutableLiveData(false)

    private var startTime = 0L
    private var stopTime = 0L

    private var locationList = mutableListOf<LatLng>()
    private var polylineList = mutableListOf<Polyline>()
    private var markerList = mutableListOf<Marker>()

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapsBinding.inflate(layoutInflater, container, false)
        binding.lifecycleOwner = this
        binding.tracking = this

        binding.btnStart.setOnClickListener{
            onStartButtonClicekd()
        }

        binding.btnStop.setOnClickListener {
            onStopButtonClicked()
        }

        binding.btnReset.setOnClickListener {
            onResetButtonClicked()
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.isMyLocationEnabled = true
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMarkerClickListener(this)
        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isZoomGesturesEnabled = false
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = false
            isCompassEnabled = false
            isScrollGesturesEnabled = false
        }

        observerTrackerService()
    }

    private fun onStartButtonClicekd() {
        if(Permissions.hasBackGroundLocationPermission(requireContext())){
            startCountDown()
            binding.btnStart.disable()
            binding.btnStart.hide()
            binding.btnStop.show()
        }else{
            Permissions.requestBackGroundPermission(this)
        }
    }

    private fun onStopButtonClicked() {
        stopForegraundService()
        binding.btnStop.hide()
        binding.btnStart.show()
    }

    private fun onResetButtonClicked() {
        mapReset()
    }

    private fun observerTrackerService(){
        TrackerService.locationList.observe(this, {
            if(it != null){
                locationList = it
                if(locationList.size > 1){
                    binding.btnStop.enable()
                }
                drawPolyline()
                followPolyline()
            }
        })
        TrackerService.started.observe(this, {
            started.value = it
        })
        TrackerService.startTime.observe(this, {
            startTime = it
        })
        TrackerService.stopTime.observe(this, {
            stopTime = it
            if(stopTime != 0L){
                showBiggerPicture()
                displayResult()
            }
        })
    }

    private fun drawPolyline(){
        val polyline = map.addPolyline(
            PolylineOptions().apply {
                width(10f)
                color(Color.BLUE)
                jointType(JointType.ROUND)
                startCap(ButtCap())
                endCap(ButtCap())
                addAll(locationList)
            }
        )

        polylineList.add(polyline)
    }

    private fun followPolyline(){
        if(locationList.isNotEmpty()){
            map.animateCamera((
                    CameraUpdateFactory.newCameraPosition(
                        MapUtil.setCameraPosition(locationList.last())
                    )),
                1000,
                null
            )
        }
    }

    private fun startCountDown() {
        binding.timerTextView.show()
        binding.btnStop.disable()
        val timer: CountDownTimer = object : CountDownTimer(4000, 1000){
            override fun onTick(millisunitlFinish: Long) {
                val currentSecond = millisunitlFinish / 1000
                if(currentSecond.toString() == "0"){
                    binding.timerTextView.text = "GO"
                    binding.timerTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                }else{
                    binding.timerTextView.text = currentSecond.toString()
                    binding.timerTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                }
            }

            override fun onFinish() {
                sendActionCommandToService(Constants.ACTION_SERVICE_START)
                binding.timerTextView.hide()
            }
        }
        timer.start()
    }

    private fun stopForegraundService() {
        binding.btnStart.disable()
        sendActionCommandToService(Constants.ACTION_SERVICE_STOP)
    }

    private fun sendActionCommandToService(action: String){
        Intent(
            requireContext(),
            TrackerService::class.java
        ).apply {
            this.action = action
            requireContext().startService(this)
        }
    }

    private fun displayResult(){
        val result = Result(
            MapUtil.calculateTheDistance(locationList),
            MapUtil.calculateElapsedTime(startTime, stopTime)
        )

        lifecycleScope.launch {
            delay(2500)
            val directions = MapsFragmentDirections.actionMapsFragmentToResultFragment(result)
            findNavController().navigate(directions)
            binding.btnStart.apply {
                hide()
                enable()
            }
            binding.btnStop.hide()
            binding.btnReset.show()
        }
    }

    private fun showBiggerPicture() {
        val bounds = LatLngBounds.Builder()
        for(location in locationList){
            bounds.include(location)
        }
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),
                100
            ),
            2000,
            null
        )
        for(marker in markerList){
            marker.remove()
        }
        addMarker(locationList.first())
        addMarker(locationList.last())
    }

    private fun addMarker(position: LatLng){
        val marker = map.addMarker(MarkerOptions().position(position))
        markerList.add(marker!!)
    }

    @SuppressLint("MissingPermission")
    private fun mapReset() {
        fusedLocationProviderClient.lastLocation.addOnCompleteListener {
            val lastKnownLocation = LatLng(
                it.result.latitude,
                it.result.longitude
            )
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    MapUtil.setCameraPosition(lastKnownLocation)
                )
            )
            for(polyline in polylineList){
                polyline.remove()
            }
            for(marker in markerList){
                marker.remove()
            }

            locationList.clear()
            markerList.clear()
            binding.btnReset.hide()
            binding.btnStart.show()
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        binding.hintTextView.animate().alpha(0f).duration = 1500
        lifecycleScope.launch {
            delay(2500)
            binding.hintTextView.hide()
            binding.btnStart.show()
        }

        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this, perms)){
            SettingsDialog.Builder(requireActivity()).build().show()
        }else{
            Permissions.requestBackGroundPermission(this)
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        onStartButtonClicekd()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        return true
    }
}