package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.data.local.notifications.NotificationsRepository
import com.example.shopsphere.CleanArchitecture.domain.IRepository
import com.example.shopsphere.CleanArchitecture.ui.models.AddressBookItem
import com.example.shopsphere.CleanArchitecture.ui.models.OrderHistoryItem
import com.example.shopsphere.CleanArchitecture.ui.models.PaymentMethodItem
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.CleanArchitecture.utils.formatEgpPrice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CheckoutSharedViewModel @Inject constructor(
    private val repository: IRepository,
    private val sharedPreference: SharedPreference,
    private val notificationsRepository: NotificationsRepository
) : ViewModel() {

    /**
     * Tracks the last-seen status step per order so `updateOrderTracking`
     * only fires a notification when the step actually advances.
     */
    private val lastNotifiedStatusStep = mutableMapOf<String, Int>()

    // ─── Mock payment methods (no backend endpoint yet) ───────────────────────
    private val initialPaymentMethods = listOf(
        PaymentMethodItem(
            id = "card_visa_2512_default",
            brand = "VISA",
            holderName = "YallaShop User",
            lastFour = "2512",
            isDefault = true,
            isSelected = true
        ),
        PaymentMethodItem(
            id = "card_mastercard_5421",
            brand = "MASTERCARD",
            holderName = "YallaShop User",
            lastFour = "5421"
        ),
        PaymentMethodItem(
            id = "card_visa_2512_alt",
            brand = "VISA",
            holderName = "YallaShop User",
            lastFour = "2512"
        )
    )

    // Address book starts empty; populated from SharedPreference or user input.
    // No mock/hardcoded addresses — the user must add their own.
    private val _addressBook = MutableLiveData<List<AddressBookItem>>(emptyList())
    val addressBook: LiveData<List<AddressBookItem>> = _addressBook

    private val _paymentMethods = MutableLiveData<List<PaymentMethodItem>>(initialPaymentMethods)
    val paymentMethods: LiveData<List<PaymentMethodItem>> = _paymentMethods

    val selectedAddress: LiveData<AddressBookItem?> =
        _addressBook.map { list -> list.firstOrNull { it.isSelected } }

    val selectedPaymentMethod: LiveData<PaymentMethodItem?> =
        _paymentMethods.map { list -> list.firstOrNull { it.isSelected } }

    val address: LiveData<Pair<String, String>?> =
        selectedAddress.map { item -> item?.let { it.title to it.address } }

    val cardLastFour: LiveData<String?> =
        selectedPaymentMethod.map { item -> item?.lastFour }

    private val _orderHistory = MutableLiveData<List<OrderHistoryItem>>(emptyList())
    val orderHistory: LiveData<List<OrderHistoryItem>> = _orderHistory

    private val _isLoadingOrders = MutableLiveData(false)
    val isLoadingOrders: LiveData<Boolean> = _isLoadingOrders

    init {
        // Pre-populate address from the last address the user saved. If the user
        // has no saved address (fresh signup), seed a default "N/A" placeholder
        // so they can still place an order without being forced through address
        // entry first. The user can replace it from the Address Book later.
        val savedAddress = sharedPreference.getDeliveryAddress()
        val seededItem = if (savedAddress.isNotBlank()) {
            AddressBookItem(
                id = "address_saved",
                title = "Delivery Address",
                address = savedAddress,
                latitude = null,
                longitude = null,
                isDefault = true,
                isSelected = true,
                phone = sharedPreference.getProfilePhone().ifBlank { DEFAULT_NA }
            )
        } else {
            AddressBookItem(
                id = "address_default_na",
                title = "Delivery Address",
                address = DEFAULT_NA,
                latitude = null,
                longitude = null,
                isDefault = true,
                isSelected = true,
                phone = DEFAULT_NA
            )
        }
        _addressBook.value = listOf(seededItem)
        fetchOrders()
    }

    // ─── Orders (backend API) ────────────────────────────────────────────────

    fun fetchOrders() {
        viewModelScope.launch {
            // Only flip `isLoadingOrders` true on the very first load. Silent
            // background re-fetches (poll, onResume) should not trigger the
            // shimmer placeholder — that would look like the list was wiped.
            val isFirstLoad = _orderHistory.value.isNullOrEmpty()
            if (isFirstLoad) _isLoadingOrders.value = true
            repository.getMyOrders()
                .onSuccess { domainOrders ->
                    val fromApi = domainOrders.map { domain ->
                        val statusStep = resolveOrderStatusStep(domain.orderStatus, null)
                        OrderHistoryItem(
                            orderId = domain.orderId.toString(),
                            date = formatApiDate(domain.date),
                            status = normalizeOrderStatusLabel(domain.orderStatus, statusStep),
                            total = formatApiTotal(domain.totalAmount),
                            statusStep = statusStep,
                            // Real courier location from the backend (null until dispatched)
                            currentLat = domain.currentLat,
                            currentLng = domain.currentLng,
                            // Real driver name from the backend
                            driverName = domain.driverName,
                            paymentStatus = domain.paymentStatus
                                .takeIf { it.isNotBlank() }
                        )
                    }
                    _orderHistory.postValue(fromApi)
                }
                .onFailure {
                    // Keep the current list on failure so the screen doesn't go blank
                }
            if (isFirstLoad) _isLoadingOrders.value = false
        }
    }

    // ─── Place order ─────────────────────────────────────────────────────────

    suspend fun placeOrder(
        total: String,
        customerName: String,
        phone: String,
        cartItems: List<PresentationProductResult>
    ): Result<OrderHistoryItem> {
        val sanitizedName = customerName.trim()
        val address = selectedAddress.value
        val paymentMethod = selectedPaymentMethod.value

        if (cartItems.isEmpty())
            return Result.failure(IllegalStateException("Cart is empty"))
        if (!isAddressValid(address))
            return Result.failure(IllegalStateException("Please select a valid delivery address"))
        if (!isPaymentMethodValid(paymentMethod))
            return Result.failure(IllegalStateException("Please select a valid payment method"))
        if (sanitizedName.isBlank())
            return Result.failure(IllegalStateException("Please enter a valid customer name"))

        // Phone comes from the delivery address (entered in MapsFragment).
        // The passed-in `phone` param is kept as a fallback for callers that
        // still supply it, but the address phone takes priority. If both are
        // missing we fall back to "N/A" so the order can still be placed.
        val rawAddressPhone = address!!.phone.trim()
        val digitsPhone = rawAddressPhone.filter { it.isDigit() }
            .ifBlank { phone.filter { it.isDigit() } }
        val resolvedPhone = digitsPhone.ifBlank { DEFAULT_NA }

        val invalidStockItem = cartItems.firstOrNull { item ->
            val stock = item.stock.coerceAtLeast(0)
            val quantity = item.quantity.coerceAtLeast(1)
            quantity > stock || stock <= 0
        }
        if (invalidStockItem != null) {
            val stock = invalidStockItem.stock.coerceAtLeast(0)
            return Result.failure(
                IllegalStateException(
                    if (stock <= 0) "'${invalidStockItem.title}' is out of stock"
                    else "Only $stock left for '${invalidStockItem.title}'"
                )
            )
        }

        val validAddress = address!!
        // Use real coordinates from the address if available; no fallback to
        // a hardcoded city — the map simply won't show a pin if coords are missing.
        val destinationLat = validAddress.latitude
        val destinationLng = validAddress.longitude
        val primaryItem = cartItems.first()

        // Placeholder used only so CheckoutFragment can navigate to TrackOrder immediately.
        // currentLat/currentLng are left null — no fake offset.
        // The real courier position will come from fetchOrders() once the backend responds.
        val placeholderOrder = OrderHistoryItem(
            orderId = "PENDING",
            date = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH).format(Date()),
            status = "Packing",
            total = total,
            itemTitle = primaryItem.title,
            itemSize = primaryItem.selectedSize.trim().uppercase(),
            itemImageUrl = primaryItem.image,
            itemPrice = formatPrice(primaryItem.price),
            address = validAddress.address,
            customerName = sanitizedName,
            phone = resolvedPhone,
            destinationLat = destinationLat,
            destinationLng = destinationLng,
            currentLat = null,   // real position comes from backend via fetchOrders()
            currentLng = null,
            statusStep = 0,
            driverName = null    // assigned by backend after dispatch
        )

        // Await the real checkout endpoint so callers don't clear the cart
        // before the server confirms. On failure we propagate the backend
        // message so the UI can show it.
        val checkoutResult = repository.checkout()
        if (checkoutResult.isFailure) {
            return Result.failure(
                checkoutResult.exceptionOrNull()
                    ?: IllegalStateException("Checkout failed")
            )
        }

        // Refresh orders + notify in parallel — these are non-blocking.
        viewModelScope.launch {
            fetchOrders()
            val latestId = _orderHistory.value
                ?.firstOrNull()?.orderId ?: placeholderOrder.orderId
            runCatching {
                notificationsRepository.notify(
                    title = "Order placed successfully",
                    body = "Order #$latestId received. We'll keep you posted with tracking updates.",
                    deepLink = "track_order:$latestId",
                    iconName = "ic_bell"
                )
            }
        }

        return Result.success(placeholderOrder)
    }

    // ─── Review (local only — backend has no review endpoint yet) ────────────

    fun submitOrderReview(orderId: String, rating: Int, comment: String) {
        val sanitizedComment = comment.trim()
        val sanitizedRating = rating.coerceIn(1, 5).toDouble()
        _orderHistory.value = _orderHistory.value.orEmpty().map { order ->
            if (order.orderId == orderId)
                order.copy(reviewRating = sanitizedRating, reviewComment = sanitizedComment)
            else order
        }
    }

    // ─── Tracking (local update only) ────────────────────────────────────────

    fun updateOrderTracking(
        orderId: String,
        currentLat: Double,
        currentLng: Double,
        status: String,
        statusStep: Int
    ) {
        val resolvedStatusStep = resolveOrderStatusStep(status, statusStep)
        val normalizedStatus = normalizeOrderStatusLabel(status, resolvedStatusStep)
        _orderHistory.value = _orderHistory.value.orEmpty().map { order ->
            if (order.orderId == orderId)
                order.copy(
                    currentLat = currentLat,
                    currentLng = currentLng,
                    status = normalizedStatus,
                    statusStep = resolvedStatusStep
                )
            else order
        }

        // Seed a notification only when the resolved step advances for this order.
        val previousStep = lastNotifiedStatusStep[orderId] ?: -1
        if (resolvedStatusStep > previousStep) {
            lastNotifiedStatusStep[orderId] = resolvedStatusStep
            val body = when (resolvedStatusStep) {
                STEP_DELIVERED -> "Your order has been delivered."
                STEP_IN_TRANSIT -> "Your order is on the way."
                STEP_PICKED -> "Your order has been picked up."
                else -> "Your order is being packed."
            }
            viewModelScope.launch {
                runCatching {
                    notificationsRepository.notify(
                        title = "Order #$orderId · $normalizedStatus",
                        body = body,
                        deepLink = "track_order:$orderId",
                        iconName = "ic_bell"
                    )
                }
            }
        }
    }

    fun getOrderById(orderId: String): OrderHistoryItem? =
        _orderHistory.value.orEmpty().firstOrNull { it.orderId == orderId }

    // ─── Address book ─────────────────────────────────────────────────────────

    fun setAddress(
        nick: String,
        full: String,
        latitude: Double? = null,
        longitude: Double? = null,
        isDefault: Boolean = false,
        phone: String = ""
    ): Boolean {
        val sanitizedTitle = nick.trim()
        val sanitizedAddress = full.trim()
        val sanitizedPhone = phone.filter { it.isDigit() }
        if (sanitizedTitle.length < 2 || sanitizedAddress.length < 8) return false
        if (sanitizedPhone.length < 8) return false
        // Coordinates are optional — they improve the map but are not required
        if (latitude != null && longitude != null && !isValidCoordinates(latitude, longitude)) return false

        val current = _addressBook.value.orEmpty()
        val existing = current.firstOrNull {
            it.title.equals(sanitizedTitle, ignoreCase = true) &&
                    it.address.equals(sanitizedAddress, ignoreCase = true)
        }

        val updated = if (existing != null) {
            current.map { item ->
                when {
                    item.id == existing.id -> item.copy(
                        title = sanitizedTitle,
                        address = sanitizedAddress,
                        latitude = latitude ?: item.latitude,
                        longitude = longitude ?: item.longitude,
                        isDefault = isDefault || item.isDefault,
                        isSelected = true,
                        phone = sanitizedPhone.ifBlank { item.phone }
                    )
                    isDefault -> item.copy(isDefault = false, isSelected = false)
                    else -> item.copy(isSelected = false)
                }
            }
        } else {
            val newItem = AddressBookItem(
                id = UUID.randomUUID().toString(),
                title = sanitizedTitle,
                address = sanitizedAddress,
                latitude = latitude,
                longitude = longitude,
                isDefault = isDefault || current.none { it.isDefault },
                isSelected = true,
                phone = sanitizedPhone
            )
            current.map { item ->
                if (isDefault) item.copy(isDefault = false, isSelected = false)
                else item.copy(isSelected = false)
            } + newItem
        }

        _addressBook.value = normalizeAddresses(updated)
        sharedPreference.saveDeliveryAddress(sanitizedAddress)
        return true
    }

    fun selectAddress(addressId: String) {
        val normalized = normalizeAddresses(
            _addressBook.value.orEmpty().map { item ->
                item.copy(isSelected = item.id == addressId)
            }
        )
        _addressBook.value = normalized
        normalized.firstOrNull { it.isSelected }?.address
            ?.let { sharedPreference.saveDeliveryAddress(it) }
    }

    // ─── Payment methods ──────────────────────────────────────────────────────

    fun setCardLastFour(
        lastFour: String,
        holderName: String = "YallaShop User",
        brand: String = "VISA"
    ): Boolean {
        val sanitized = lastFour.filter { it.isDigit() }.takeLast(4)
        val sanitizedHolder = holderName.trim()
        val sanitizedBrand = brand.trim().uppercase(Locale.ENGLISH)
        if (sanitized.length != 4) return false
        if (sanitizedHolder.length < 2) return false
        if (sanitizedBrand.isBlank()) return false

        val current = _paymentMethods.value.orEmpty().ifEmpty { initialPaymentMethods }
        val existing = current.firstOrNull {
            it.lastFour == sanitized && it.brand.equals(sanitizedBrand, ignoreCase = true)
        }

        val updated = if (existing != null) {
            current.map { item ->
                if (item.id == existing.id)
                    item.copy(holderName = sanitizedHolder, brand = sanitizedBrand, isSelected = true)
                else
                    item.copy(isSelected = false)
            }
        } else {
            val newItem = PaymentMethodItem(
                id = UUID.randomUUID().toString(),
                brand = sanitizedBrand,
                holderName = sanitizedHolder,
                lastFour = sanitized,
                isDefault = false,
                isSelected = true
            )
            current.map { it.copy(isSelected = false) } + newItem
        }

        _paymentMethods.value = normalizePaymentMethods(updated)
        return true
    }

    fun selectPaymentMethod(cardId: String) {
        _paymentMethods.value = normalizePaymentMethods(
            _paymentMethods.value.orEmpty().map { item ->
                item.copy(isSelected = item.id == cardId)
            }
        )
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    fun isAddressValid(address: AddressBookItem?): Boolean {
        if (address == null) return false
        if (address.title.trim().isBlank()) return false
        // Accept any non-blank address string, including the "N/A" default so
        // users can place an order immediately after signup without filling in
        // address details. They can update it later from the Address Book.
        if (address.address.trim().isBlank()) return false
        return true
    }

    fun isPaymentMethodValid(paymentMethod: PaymentMethodItem?): Boolean {
        if (paymentMethod == null) return false
        if (paymentMethod.brand.trim().isBlank()) return false
        if (paymentMethod.holderName.trim().length < 2) return false
        return paymentMethod.lastFour.length == 4 && paymentMethod.lastFour.all { it.isDigit() }
    }

    // ─── Shared status resolution (single source of truth used by both
    //     CheckoutSharedViewModel and TrackOrderFragment) ────────────────────

    /**
     * Converts a raw status string from the backend into a 0–3 step index.
     * [storedStep] is the previously resolved step (may be null or from the DTO);
     * we always take the maximum so the timeline never goes backwards.
     */
    fun resolveOrderStatusStep(status: String, storedStep: Int?): Int {
        val normalized = status.trim().lowercase(Locale.ENGLISH)
            .replace("_", " ").replace("-", " ")
        val derived = when {
            normalized.contains("deliver") || normalized.contains("complete") -> STEP_DELIVERED
            normalized.contains("transit") || normalized.contains("shipping") ||
                    normalized.contains("shipped") || normalized.contains("out for delivery") -> STEP_IN_TRANSIT
            normalized.contains("pick") || normalized.contains("dispatch") -> STEP_PICKED
            else -> STEP_PACKING
        }
        return maxOf(storedStep?.coerceIn(STEP_PACKING, STEP_DELIVERED) ?: STEP_PACKING, derived)
    }

    /**
     * Returns a human-readable status label for a resolved step index.
     */
    fun normalizeOrderStatusLabel(status: String, resolvedStep: Int): String {
        return when (resolvedStep.coerceIn(STEP_PACKING, STEP_DELIVERED)) {
            STEP_DELIVERED -> "Delivered"
            STEP_IN_TRANSIT -> "In Transit"
            STEP_PICKED -> "Picked"
            else -> "Packing"
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun formatApiDate(raw: String): String {
        return runCatching {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(raw)!!
            SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH).format(parsed)
        }.getOrDefault(raw)
    }

    private fun formatApiTotal(amount: Double): String = formatEgpPrice(amount)

    private fun formatPrice(price: Double): String = formatEgpPrice(price)

    private fun normalizeAddresses(items: List<AddressBookItem>): List<AddressBookItem> {
        val valid = items.filter { isAddressValid(it) }
        if (valid.isEmpty()) return emptyList()
        val defaultId = valid.firstOrNull { it.isDefault }?.id ?: valid.first().id
        val selectedId = valid.firstOrNull { it.isSelected }?.id ?: defaultId
        return valid.map { it.copy(isDefault = it.id == defaultId, isSelected = it.id == selectedId) }
    }

    private fun normalizePaymentMethods(items: List<PaymentMethodItem>): List<PaymentMethodItem> {
        val valid = items.filter { isPaymentMethodValid(it) }
        if (valid.isEmpty()) return initialPaymentMethods
        val defaultId = valid.firstOrNull { it.isDefault }?.id ?: valid.first().id
        val selectedId = valid.firstOrNull { it.isSelected }?.id ?: defaultId
        return valid.map { it.copy(isDefault = it.id == defaultId, isSelected = it.id == selectedId) }
    }

    private fun isValidCoordinates(latitude: Double?, longitude: Double?): Boolean {
        if (latitude == null || longitude == null) return false
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    companion object {
        const val STEP_PACKING = 0
        const val STEP_PICKED = 1
        const val STEP_IN_TRANSIT = 2
        const val STEP_DELIVERED = 3

        /**
         * Default placeholder shown when the user has not entered an address
         * or phone yet. Lets a fresh signup place an order without first
         * filling in profile details.
         */
        const val DEFAULT_NA = "N/A"
    }
}
