package com.example.shopsphere.CleanArchitecture.ui.views

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.CleanArchitecture.utils.showSuccessDialog
import com.example.shopsphere.R
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

    private val nicknameOptions by lazy {
        listOf("Home", "Office", "Apartment", "Parent's House")
    }

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

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        binding.editNickname.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, nicknameOptions)
        )

        val watcher = simpleWatcher { updateAddButtonState() }
        binding.editNickname.addTextChangedListener(watcher)
        binding.editAddress.addTextChangedListener(watcher)
        binding.editPhone.addTextChangedListener(watcher)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }
        binding.btnCloseSheet.setOnClickListener { findNavController().navigateUp() }
        binding.btnCheckout.setOnClickListener { saveAddress() }

        updateAddButtonState()
    }

    private fun saveAddress() {
        val nickname = binding.editNickname.text?.toString().orEmpty().trim()
        val fullAddress = binding.editAddress.text?.toString().orEmpty().trim()
        val phone = binding.editPhone.text?.toString().orEmpty().filter { it.isDigit() }
        val latLng = selectedLatLng

        if (latLng == null || nickname.isBlank() || fullAddress.length < 8 || phone.length < 8) {
            updateAddButtonState()
            return
        }

        showSuccessDialog(
            title = getString(R.string.dialog_congratulations_title),
            message = getString(R.string.dialog_address_success_message),
            primaryText = getString(R.string.dialog_thanks)
        ) {
            val result = Bundle().apply {
                putDouble("lat", latLng.latitude)
                putDouble("lng", latLng.longitude)
                putString("nickname", nickname)
                putString("fullname", fullAddress)
                putString("phone", phone)
                putBoolean("is_default", binding.cbDefault.isChecked)
            }
            setFragmentResult("location_result", result)
            findNavController().navigateUp()
        }
    }

    private fun updateAddButtonState() {
        val isValid = selectedLatLng != null &&
            binding.editNickname.text?.toString().orEmpty().trim().length >= 2 &&
            binding.editAddress.text?.toString().orEmpty().trim().length >= 8 &&
            binding.editPhone.text?.toString().orEmpty().filter { it.isDigit() }.length >= 8
        binding.btnCheckout.isEnabled = isValid
        binding.btnCheckout.alpha = if (isValid) 1f else 0.45f
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                val target = location?.let { LatLng(it.latitude, it.longitude) } ?: DEFAULT_MAP_LAT_LNG
                updateSelectedLocation(target)
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 16f))
            }
        } else {
            updateSelectedLocation(DEFAULT_MAP_LAT_LNG)
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_MAP_LAT_LNG, 16f))
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun setupMapTapListener() {
        googleMap?.setOnMapClickListener { latLng ->
            updateSelectedLocation(latLng)
            googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        }
    }

    private fun updateSelectedLocation(latLng: LatLng) {
        selectedLatLng = latLng
        googleMap?.clear()
        googleMap?.addMarker(MarkerOptions().position(latLng))
        updateAddButtonState()
    }

    private fun simpleWatcher(onAfter: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = onAfter.invoke()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        googleMap = null
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private val DEFAULT_MAP_LAT_LNG = LatLng(30.0444, 31.2357) // Cairo
    }
}
