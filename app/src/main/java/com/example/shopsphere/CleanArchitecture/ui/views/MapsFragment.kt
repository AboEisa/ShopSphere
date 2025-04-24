package com.example.shopsphere.CleanArchitecture.ui.views

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.databinding.FragmentMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapsFragment : Fragment() {

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var googleMap: GoogleMap? = null
    private var selectedLatLng: LatLng? = null

    private val callback = OnMapReadyCallback { map ->
        googleMap = map
        enableMyLocation()
        setupMapTapListener()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val mapFragment = childFragmentManager.findFragmentById(com.example.shopsphere.R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
        mapFragment?.view?.isClickable = true
        onClicks()

    }
    fun onClicks(){
        binding.btnCheckout.setOnClickListener {
            val nickname = binding.editNickname.text.toString()
            val fullname = binding.editAddress.text.toString()
            val latLng = selectedLatLng
            if (latLng != null && nickname.isNotBlank() && fullname.isNotBlank()) {
                returnLocationResult(latLng.latitude, latLng.longitude, nickname, fullname)
            } else {
                binding.editNickname.error = "Please enter a nickname"
                binding.editAddress.error = "Please enter a full address"
            }
        }
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    selectedLatLng = currentLatLng
                    googleMap?.clear()
                    googleMap?.addMarker(MarkerOptions().position(currentLatLng).title("Your Location"))
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
                }
            }
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun setupMapTapListener() {
        googleMap?.setOnMapClickListener { latLng ->
            selectedLatLng = latLng
            googleMap?.clear()
            googleMap?.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
            googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            }
        }
    }

    private fun returnLocationResult(lat: Double, lng: Double, nickname: String, fullname: String) {
        val result = Bundle().apply {
            putDouble("lat", lat)
            putDouble("lng", lng)
            putString("nickname", nickname)
            putString("fullname", fullname)
        }
        setFragmentResult("location_result", result)
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        googleMap = null
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}