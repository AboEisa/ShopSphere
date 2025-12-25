package com.example.shopsphere.CleanArchitecture.ui.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.data.network.DirectionsApiServices
import com.example.shopsphere.CleanArchitecture.ui.models.OrderHistoryItem
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.CheckoutSharedViewModel
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentTrackOrderBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class TrackOrderFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentTrackOrderBinding? = null
    private val binding get() = _binding!!

    private val args: TrackOrderFragmentArgs by navArgs()
    private val checkoutSharedViewModel: CheckoutSharedViewModel by activityViewModels()

    @Inject
    lateinit var sharedPreference: SharedPreference

    @Inject
    lateinit var directionsApiServices: DirectionsApiServices

    private var map: GoogleMap? = null
    private var courierMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var routePolyline: Polyline? = null
    private var routeShadowPolyline: Polyline? = null
    private var currentOrder: OrderHistoryItem? = null
    private var courierAnimator: ValueAnimator? = null
    private var bottomSheetBehavior: BottomSheetBehavior<MaterialCardView>? = null
    private var courierMarkerIcon: BitmapDescriptor? = null
    private var destinationMarkerIcon: BitmapDescriptor? = null

    private var mapsApiKey: String = ""
    private var androidPackageName: String = ""
    private var androidCertSha1: String = ""
    private var routeRequestJob: Job? = null
    private var cachedRoutePoints: List<LatLng> = emptyList()
    private var cachedRouteOrigin: LatLng? = null
    private var cachedRouteDestination: LatLng? = null
    private var lastRouteFetchAtMs: Long = 0L
    private var hasShownDirectionsDeniedMessage = false

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            updateMapPaddingWithSheetTop(bottomSheet.top)
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            if (_binding == null) return
            val clamped = slideOffset.coerceIn(0f, 1f)
            binding.topOverlay.alpha = OVERLAY_ALPHA_COLLAPSED +
                ((OVERLAY_ALPHA_EXPANDED - OVERLAY_ALPHA_COLLAPSED) * clamped)
            updateMapPaddingWithSheetTop(bottomSheet.top)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapsApiKey = resolveMapsApiKey()
        androidPackageName = requireContext().packageName
        androidCertSha1 = resolveAndroidCertSha1()

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        setupCallAction()
        setupBottomSheet()
        animateScreenIntro()
        applyTopPanelInsets()

        val mapFragment = childFragmentManager.findFragmentById(R.id.trackMap) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        checkoutSharedViewModel.orderHistory.observe(viewLifecycleOwner) { orders ->
            val order = orders.firstOrNull { it.orderId == args.orderId } ?: return@observe
            val previousOrder = currentOrder
            currentOrder = order
            renderOrder(order, previousOrder)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setupMapUi(googleMap)
        applyUberMapStyle(googleMap)

        courierMarkerIcon = bitmapDescriptorFromVector(R.drawable.ic_courier_car_marker, 44)
        destinationMarkerIcon = bitmapDescriptorFromVector(R.drawable.ic_destination_dot, 18)

        binding.statusSheet.post {
            updateMapPaddingWithSheetTop(binding.statusSheet.top)
        }
        currentOrder?.let { renderMap(order = it, moveCamera = true) }
    }

    private fun setupMapUi(googleMap: GoogleMap) {
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = false
            isMapToolbarEnabled = false
            isCompassEnabled = false
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = true
        }
        googleMap.isBuildingsEnabled = false
        googleMap.isIndoorEnabled = false
        googleMap.isTrafficEnabled = false
    }

    private fun applyUberMapStyle(googleMap: GoogleMap) {
        runCatching {
            googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_uber_like)
            )
        }
    }

    private fun setupBottomSheet() {
        val behavior = BottomSheetBehavior.from(binding.statusSheet)
        bottomSheetBehavior?.removeBottomSheetCallback(bottomSheetCallback)

        bottomSheetBehavior = behavior
        behavior.isHideable = false
        behavior.skipCollapsed = false
        behavior.isFitToContents = true
        behavior.isDraggable = true
        behavior.peekHeight = dpToPx(SHEET_PEEK_HEIGHT_DP)
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        binding.topOverlay.alpha = OVERLAY_ALPHA_COLLAPSED
        behavior.addBottomSheetCallback(bottomSheetCallback)

        binding.sheetHandle.setOnClickListener { toggleBottomSheetState() }
    }

    private fun applyTopPanelInsets() {
        val baseTopPadding = binding.topPanel.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.topPanel) { view, insets ->
            val statusBarsInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = baseTopPadding + statusBarsInset)
            insets
        }
        ViewCompat.requestApplyInsets(binding.topPanel)
    }

    private fun toggleBottomSheetState() {
        val behavior = bottomSheetBehavior ?: return
        behavior.state = when (behavior.state) {
            BottomSheetBehavior.STATE_EXPANDED -> BottomSheetBehavior.STATE_COLLAPSED
            BottomSheetBehavior.STATE_HALF_EXPANDED -> BottomSheetBehavior.STATE_COLLAPSED
            BottomSheetBehavior.STATE_COLLAPSED -> BottomSheetBehavior.STATE_EXPANDED
            else -> BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun setupCallAction() {
        binding.buttonCallCourier.setOnClickListener {
            val phone = currentOrder?.phone?.takeIf { value -> value.isNotBlank() }
                ?: sharedPreference.getProfilePhone().takeIf { value -> value.isNotBlank() }
            if (phone.isNullOrBlank()) return@setOnClickListener
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
        }
    }

    private fun renderOrder(order: OrderHistoryItem, previousOrder: OrderHistoryItem? = null) {
        binding.textTitle.text = getString(R.string.track_order_with_id, order.orderId)
        binding.textLastUpdated.text = getString(R.string.track_last_updated, getCurrentTimeLabel())
        binding.textCourierName.text = order.customerName.ifBlank { getString(R.string.track_courier_name) }
        binding.textPackingAddress.text = getString(R.string.track_default_warehouse)
        binding.textPickedAddress.text = getString(R.string.track_default_pickup)
        binding.textDeliveredAddress.text = order.address.ifBlank { getString(R.string.track_unknown_address) }

        val destination = LatLng(order.destinationLat ?: DEFAULT_LAT, order.destinationLng ?: DEFAULT_LNG)
        val current = LatLng(order.currentLat ?: destination.latitude, order.currentLng ?: destination.longitude)
        val hasArrived = isOrderArrived(order, current, destination)

        binding.textLiveStatus.text = if (hasArrived) {
            getString(R.string.track_status_delivered)
        } else {
            order.status.ifBlank { getString(R.string.track_status_packing) }
        }
        binding.textTransitAddress.text = if (hasArrived) {
            getString(R.string.track_arrived_at_destination)
        } else {
            getString(R.string.track_live_coordinates, order.currentLat ?: 0.0, order.currentLng ?: 0.0)
        }

        showDistanceAndEta(current, destination, hasArrived)
        updateStatusTimeline(if (hasArrived) ORDER_STEP_DELIVERED else order.statusStep)

        renderMap(
            order = order,
            previousPosition = previousOrder?.let {
                LatLng(
                    it.currentLat ?: current.latitude,
                    it.currentLng ?: current.longitude
                )
            }
        )
    }

    private fun renderMap(
        order: OrderHistoryItem,
        moveCamera: Boolean = false,
        previousPosition: LatLng? = null
    ) {
        val googleMap = map ?: return
        val destination = LatLng(order.destinationLat ?: DEFAULT_LAT, order.destinationLng ?: DEFAULT_LNG)
        val current = LatLng(order.currentLat ?: destination.latitude, order.currentLng ?: destination.longitude)
        val hasArrived = isOrderArrived(order, current, destination)

        upsertDestinationMarker(googleMap, destination)
        upsertCourierMarker(googleMap, current)
        val markerPosition = courierMarker?.position
        val start = previousPosition ?: markerPosition ?: current

        if (hasArrived) {
            courierAnimator?.cancel()
            courierMarker?.position = destination
            courierMarker?.rotation = calculateBearing(start, destination)
            clearRoutePolyline()
            updateCameraForOrder(
                current = destination,
                destination = destination,
                statusStep = ORDER_STEP_DELIVERED,
                preferBounds = true,
                animated = moveCamera
            )
            return
        }

        ensureStreetRoute(current, destination, force = moveCamera)

        val shouldAnimateMovement =
            calculateDistanceMeters(start, current) >= MIN_ANIMATABLE_DISTANCE_METERS &&
                order.statusStep < ORDER_STEP_DELIVERED

        if (shouldAnimateMovement) {
            animateCourierMovement(
                start = start,
                end = current,
                destination = destination,
                statusStep = order.statusStep,
                forceBoundsAtEnd = moveCamera
            )
        } else {
            courierAnimator?.cancel()
            courierMarker?.position = current
            courierMarker?.rotation = calculateBearing(start, current)

            updateRoutePolyline(current, destination)
            updateCameraForOrder(
                current = current,
                destination = destination,
                statusStep = order.statusStep,
                preferBounds = moveCamera || order.statusStep == ORDER_STEP_DELIVERED,
                animated = moveCamera
            )
        }
    }

    private fun clearRoutePolyline() {
        routeRequestJob?.cancel()
        routeRequestJob = null
        routeShadowPolyline?.remove()
        routePolyline?.remove()
        routeShadowPolyline = null
        routePolyline = null
        cachedRoutePoints = emptyList()
        cachedRouteOrigin = null
        cachedRouteDestination = null
        lastRouteFetchAtMs = 0L
    }

    private fun upsertDestinationMarker(googleMap: GoogleMap, destination: LatLng) {
        if (destinationMarker == null) {
            destinationMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(destination)
                    .anchor(0.5f, 0.5f)
                    .title(getString(R.string.track_status_delivered))
                    .icon(destinationMarkerIcon ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        } else {
            destinationMarker?.position = destination
        }
    }

    private fun upsertCourierMarker(googleMap: GoogleMap, current: LatLng) {
        if (courierMarker == null) {
            courierMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(current)
                    .anchor(0.5f, 0.5f)
                    .flat(true)
                    .title(getString(R.string.track_courier_role))
                    .icon(courierMarkerIcon ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
        }
    }

    private fun ensureStreetRoute(origin: LatLng, destination: LatLng, force: Boolean) {
        if (mapsApiKey.isBlank()) return

        val now = System.currentTimeMillis()
        val destinationStable = cachedRouteDestination?.let {
            calculateDistanceMeters(it, destination) <= ROUTE_DESTINATION_REFRESH_METERS
        } ?: false
        val originStable = cachedRouteOrigin?.let {
            calculateDistanceMeters(it, origin) <= ROUTE_ORIGIN_REFRESH_METERS
        } ?: false
        val recentlyFetched = now - lastRouteFetchAtMs < ROUTE_REFRESH_MIN_INTERVAL_MS

        if (!force && destinationStable && originStable && recentlyFetched && cachedRoutePoints.size > 1) {
            return
        }

        routeRequestJob?.cancel()
        routeRequestJob = viewLifecycleOwner.lifecycleScope.launch {
            val response = withContext(Dispatchers.IO) {
                runCatching {
                    directionsApiServices.getDrivingDirections(
                        androidPackage = androidPackageName,
                        androidCertSha1 = androidCertSha1,
                        origin = formatLatLng(origin),
                        destination = formatLatLng(destination),
                        apiKey = mapsApiKey
                    )
                }.getOrNull()
            }

            if (_binding == null) return@launch
            if (response == null) return@launch
            if (!response.status.equals(DIRECTIONS_STATUS_OK, ignoreCase = true)) {
                Log.w(
                    TAG,
                    "Directions failed: status=${response.status}, error=${response.errorMessage.orEmpty()}"
                )
                if (
                    !hasShownDirectionsDeniedMessage &&
                    response.status.equals(DIRECTIONS_STATUS_REQUEST_DENIED, ignoreCase = true) &&
                    _binding != null
                ) {
                    hasShownDirectionsDeniedMessage = true
                    binding.textLastUpdated.text = getString(R.string.track_route_unavailable)
                }
                return@launch
            }

            val route = response.routes.firstOrNull()
            val detailedRoute = route?.legs.orEmpty()
                .flatMap { leg -> leg.steps }
                .mapNotNull { step -> step.polyline?.points }
                .flatMap { encoded -> decodePolyline(encoded) }

            val decodedRoute = if (detailedRoute.size > 1) {
                detailedRoute
            } else {
                val encodedPolyline = route?.overviewPolyline?.points.orEmpty()
                decodePolyline(encodedPolyline)
            }
            if (decodedRoute.size < 2) return@launch

            cachedRoutePoints = decodedRoute
            cachedRouteOrigin = origin
            cachedRouteDestination = destination
            lastRouteFetchAtMs = System.currentTimeMillis()

            val current = courierMarker?.position ?: origin
            updateRoutePolyline(current, destination)
        }
    }

    private fun updateRoutePolyline(current: LatLng, destination: LatLng) {
        val googleMap = map ?: return
        val path = buildStreetPath(current, destination)

        if (routeShadowPolyline == null) {
            routeShadowPolyline = googleMap.addPolyline(
                PolylineOptions()
                    .width(11f)
                    .color(Color.argb(90, 17, 24, 39))
                    .startCap(RoundCap())
                    .endCap(RoundCap())
                    .jointType(JointType.ROUND)
                    .zIndex(1f)
            )
        }

        if (routePolyline == null) {
            routePolyline = googleMap.addPolyline(
                PolylineOptions()
                    .width(7f)
                    .color(Color.parseColor("#111111"))
                    .startCap(RoundCap())
                    .endCap(RoundCap())
                    .jointType(JointType.ROUND)
                    .zIndex(2f)
            )
        }

        routeShadowPolyline?.points = path
        routePolyline?.points = path
    }

    private fun buildStreetPath(current: LatLng, destination: LatLng): List<LatLng> {
        val route = cachedRoutePoints
        if (route.size < 2) {
            return listOf(current, destination)
        }

        val nearestIndex = findNearestRouteIndex(current, route)
        if (nearestIndex !in route.indices) {
            return listOf(current, destination)
        }

        val trimmed = route.subList(nearestIndex, route.size).toMutableList()
        if (trimmed.isEmpty()) {
            return listOf(current, destination)
        }

        if (calculateDistanceMeters(current, trimmed.first()) > MIN_ROUTE_POINT_DISTANCE_METERS) {
            trimmed.add(0, current)
        } else {
            trimmed[0] = current
        }

        if (calculateDistanceMeters(trimmed.last(), destination) > MIN_ROUTE_POINT_DISTANCE_METERS) {
            trimmed.add(destination)
        } else {
            trimmed[trimmed.lastIndex] = destination
        }

        return compactNearbyPoints(trimmed)
    }

    private fun compactNearbyPoints(points: List<LatLng>): List<LatLng> {
        if (points.size <= 2) return points
        val compacted = mutableListOf<LatLng>()
        compacted.add(points.first())
        points.drop(1).forEach { point ->
            if (calculateDistanceMeters(compacted.last(), point) >= MIN_ROUTE_POINT_DISTANCE_METERS) {
                compacted.add(point)
            }
        }
        if (compacted.last() != points.last()) {
            compacted.add(points.last())
        }
        return compacted
    }

    private fun findNearestRouteIndex(target: LatLng, route: List<LatLng>): Int {
        var minDistance = Float.MAX_VALUE
        var minIndex = -1

        route.forEachIndexed { index, point ->
            val distance = calculateDistanceMeters(target, point)
            if (distance < minDistance) {
                minDistance = distance
                minIndex = index
            }
        }
        return minIndex
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        if (encoded.isBlank()) return emptyList()
        val polyline = mutableListOf<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20 && index < encoded.length)
            val dLat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dLat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20 && index < encoded.length)
            val dLng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dLng

            polyline.add(LatLng(lat / POLYLINE_PRECISION, lng / POLYLINE_PRECISION))
        }

        return polyline
    }

    private fun animateCourierMovement(
        start: LatLng,
        end: LatLng,
        destination: LatLng,
        statusStep: Int,
        forceBoundsAtEnd: Boolean
    ) {
        val googleMap = map ?: return
        val distanceMeters = calculateDistanceMeters(start, end).toDouble()
        val durationMs = (distanceMeters * ANIMATION_DURATION_PER_METER_MS)
            .toLong()
            .coerceIn(MIN_MARKER_ANIMATION_MS, MAX_MARKER_ANIMATION_MS)

        courierAnimator?.cancel()
        courierAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()

            var lastPosition = start
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                val animatedPosition = interpolateLatLng(start, end, fraction)
                courierMarker?.position = animatedPosition
                courierMarker?.rotation = calculateBearing(lastPosition, animatedPosition)
                lastPosition = animatedPosition

                updateRoutePolyline(animatedPosition, destination)
                if (statusStep < ORDER_STEP_DELIVERED && !forceBoundsAtEnd) {
                    val camera = CameraPosition.Builder()
                        .target(animatedPosition)
                        .zoom(16.6f)
                        .tilt(49f)
                        .bearing(calculateBearing(animatedPosition, destination))
                        .build()
                    googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(camera))
                }
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (forceBoundsAtEnd || statusStep == ORDER_STEP_DELIVERED) {
                        updateCameraForOrder(
                            current = end,
                            destination = destination,
                            statusStep = statusStep,
                            preferBounds = true,
                            animated = true
                        )
                    }
                }
            })
            start()
        }
    }

    private fun updateCameraForOrder(
        current: LatLng,
        destination: LatLng,
        statusStep: Int,
        preferBounds: Boolean,
        animated: Boolean
    ) {
        val googleMap = map ?: return
        if (statusStep == ORDER_STEP_DELIVERED || preferBounds) {
            val bounds = LatLngBounds.builder()
                .include(current)
                .include(destination)
                .build()
            if (animated) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, CAMERA_BOUNDS_PADDING_PX))
            } else {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, CAMERA_BOUNDS_PADDING_PX))
            }
            return
        }

        val camera = CameraPosition.Builder()
            .target(current)
            .zoom(16.6f)
            .tilt(49f)
            .bearing(calculateBearing(current, destination))
            .build()

        if (animated) {
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(camera))
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(camera))
        }
    }

    private fun updateStatusTimeline(statusStep: Int) {
        setDot(binding.statusPackingDot, statusStep >= ORDER_STEP_PACKING)
        setDot(binding.statusPickedDot, statusStep >= ORDER_STEP_PICKED)
        setDot(binding.statusTransitDot, statusStep >= ORDER_STEP_IN_TRANSIT)
        setDot(binding.statusDeliveredDot, statusStep >= ORDER_STEP_DELIVERED)
    }

    private fun setDot(view: View, active: Boolean) {
        view.background = ContextCompat.getDrawable(
            requireContext(),
            if (active) R.drawable.bg_track_status_active else R.drawable.bg_track_status_inactive
        )
    }

    private fun showDistanceAndEta(current: LatLng, destination: LatLng, hasArrived: Boolean) {
        if (hasArrived) {
            binding.textDistanceMinutes.text = getString(R.string.track_arrived_label)
            binding.textDistanceMiles.text = getString(R.string.track_arrived_subtitle)
            return
        }

        val distanceMeters = calculateDistanceMeters(current, destination)
        val miles = distanceMeters / METER_IN_MILE
        val minutes = if (distanceMeters < 15) 0 else max(1, (distanceMeters / METERS_PER_MINUTE).toInt())

        binding.textDistanceMiles.text = getString(R.string.track_distance_miles_away, miles)
        binding.textDistanceMinutes.text = getString(R.string.track_eta_minutes, minutes)
    }

    private fun isOrderArrived(order: OrderHistoryItem, current: LatLng, destination: LatLng): Boolean {
        if (order.statusStep >= ORDER_STEP_DELIVERED) return true
        if (order.status.equals(getString(R.string.track_status_delivered), ignoreCase = true)) return true
        return order.statusStep >= ORDER_STEP_IN_TRANSIT &&
            calculateDistanceMeters(current, destination) <= ARRIVAL_DISTANCE_THRESHOLD_METERS
    }

    private fun calculateDistanceMeters(from: LatLng, to: LatLng): Float {
        val result = FloatArray(1)
        android.location.Location.distanceBetween(
            from.latitude,
            from.longitude,
            to.latitude,
            to.longitude,
            result
        )
        return result[0]
    }

    private fun interpolateLatLng(from: LatLng, to: LatLng, fraction: Float): LatLng {
        val lat = from.latitude + ((to.latitude - from.latitude) * fraction)
        val lng = from.longitude + ((to.longitude - from.longitude) * fraction)
        return LatLng(lat, lng)
    }

    private fun calculateBearing(from: LatLng, to: LatLng): Float {
        val fromLat = Math.toRadians(from.latitude)
        val fromLng = Math.toRadians(from.longitude)
        val toLat = Math.toRadians(to.latitude)
        val toLng = Math.toRadians(to.longitude)
        val deltaLng = toLng - fromLng

        val y = sin(deltaLng) * cos(toLat)
        val x = cos(fromLat) * sin(toLat) - sin(fromLat) * cos(toLat) * cos(deltaLng)
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360.0) % 360.0).toFloat()
    }

    private fun bitmapDescriptorFromVector(
        @DrawableRes drawableResId: Int,
        sizeDp: Int
    ): BitmapDescriptor? {
        val drawable = ContextCompat.getDrawable(requireContext(), drawableResId) ?: return null
        val iconSizePx = dpToPx(sizeDp).coerceAtLeast(1)
        drawable.setBounds(0, 0, iconSizePx, iconSizePx)

        val bitmap = Bitmap.createBitmap(iconSizePx, iconSizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun resolveMapsApiKey(): String {
        return runCatching {
            val appInfo = requireContext().packageManager.getApplicationInfo(
                requireContext().packageName,
                PackageManager.GET_META_DATA
            )
            appInfo.metaData?.getString(GOOGLE_MAPS_META_KEY).orEmpty()
        }.getOrDefault("")
    }

    private fun resolveAndroidCertSha1(): String {
        return runCatching {
            val packageManager = requireContext().packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(
                    requireContext().packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(
                    requireContext().packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo.apkContentsSigners.firstOrNull()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures.firstOrNull()
            } ?: return ""

            val cert = CertificateFactory.getInstance("X.509")
                .generateCertificate(ByteArrayInputStream(signature.toByteArray())) as X509Certificate
            val digest = MessageDigest.getInstance("SHA-1").digest(cert.encoded)
            digest.joinToString(separator = "") { byte -> "%02X".format(byte) }
        }.getOrDefault("")
    }

    private fun formatLatLng(latLng: LatLng): String {
        return "${latLng.latitude},${latLng.longitude}"
    }

    private fun getCurrentTimeLabel(): String {
        val formatter = SimpleDateFormat("h:mm a", Locale.ENGLISH)
        return formatter.format(Date())
    }

    private fun animateScreenIntro() {
        binding.topPanel.apply {
            alpha = 0f
            translationY = -20f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(280L)
                .start()
        }

        binding.statusSheet.apply {
            alpha = 0f
            translationY = 56f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(340L)
                .start()
        }
    }

    private fun updateMapPaddingWithSheetTop(sheetTop: Int) {
        val googleMap = map ?: return
        if (_binding == null) return

        val horizontalPadding = dpToPx(16)
        val topPadding = max(dpToPx(MAP_TOP_PADDING_MIN_DP), binding.topPanel.bottom + dpToPx(10))
        val dynamicBottom = (binding.root.height - sheetTop) + dpToPx(16)
        val minBottom = dpToPx(MIN_MAP_BOTTOM_PADDING_DP)
        val bottomPadding = max(dynamicBottom, minBottom)

        googleMap.setPadding(horizontalPadding, topPadding, horizontalPadding, bottomPadding)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        courierAnimator?.cancel()
        courierAnimator = null

        routeRequestJob?.cancel()
        routeRequestJob = null
        cachedRoutePoints = emptyList()
        cachedRouteOrigin = null
        cachedRouteDestination = null
        lastRouteFetchAtMs = 0L
        hasShownDirectionsDeniedMessage = false

        bottomSheetBehavior?.removeBottomSheetCallback(bottomSheetCallback)
        bottomSheetBehavior = null

        courierMarker?.remove()
        destinationMarker?.remove()
        routePolyline?.remove()
        routeShadowPolyline?.remove()

        map = null
        courierMarker = null
        destinationMarker = null
        routePolyline = null
        routeShadowPolyline = null
        _binding = null
    }

    companion object {
        private const val TAG = "TrackOrderFragment"
        private const val GOOGLE_MAPS_META_KEY = "com.google.android.geo.API_KEY"
        private const val DIRECTIONS_STATUS_OK = "OK"
        private const val DIRECTIONS_STATUS_REQUEST_DENIED = "REQUEST_DENIED"

        private const val ORDER_STEP_PACKING = 0
        private const val ORDER_STEP_PICKED = 1
        private const val ORDER_STEP_IN_TRANSIT = 2
        private const val ORDER_STEP_DELIVERED = 3

        private const val SHEET_PEEK_HEIGHT_DP = 258
        private const val MAP_TOP_PADDING_MIN_DP = 156
        private const val MIN_MAP_BOTTOM_PADDING_DP = 210
        private const val CAMERA_BOUNDS_PADDING_PX = 140
        private const val OVERLAY_ALPHA_COLLAPSED = 0.66f
        private const val OVERLAY_ALPHA_EXPANDED = 0.86f

        private const val MIN_ANIMATABLE_DISTANCE_METERS = 3.5f
        private const val ARRIVAL_DISTANCE_THRESHOLD_METERS = 30f
        private const val ANIMATION_DURATION_PER_METER_MS = 14.0
        private const val MIN_MARKER_ANIMATION_MS = 650L
        private const val MAX_MARKER_ANIMATION_MS = 2600L

        private const val ROUTE_ORIGIN_REFRESH_METERS = 35f
        private const val ROUTE_DESTINATION_REFRESH_METERS = 15f
        private const val ROUTE_REFRESH_MIN_INTERVAL_MS = 5000L
        private const val MIN_ROUTE_POINT_DISTANCE_METERS = 8f
        private const val POLYLINE_PRECISION = 1E5

        private const val DEFAULT_LAT = 30.0444
        private const val DEFAULT_LNG = 31.2357
        private const val METER_IN_MILE = 1609.34
        private const val METERS_PER_MINUTE = 240.0
    }
}
