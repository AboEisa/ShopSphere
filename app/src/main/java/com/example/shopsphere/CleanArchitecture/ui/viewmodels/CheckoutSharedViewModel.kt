package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.shopsphere.CleanArchitecture.domain.IRepository
import com.example.shopsphere.CleanArchitecture.ui.models.AddressBookItem
import com.example.shopsphere.CleanArchitecture.ui.models.OrderHistoryItem
import com.example.shopsphere.CleanArchitecture.ui.models.PaymentMethodItem
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
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
    private val repository: IRepository
) : ViewModel() {

    private val initialAddresses = listOf(
        AddressBookItem(
            id = "address_home",
            title = "Home",
            address = "925 S Chugach St #APT 10, Alaska 99645",
            latitude = 61.2176,
            longitude = -149.8997,
            isDefault = true,
            isSelected = true
        ),
        AddressBookItem(
            id = "address_office",
            title = "Office",
            address = "2438 6th Ave, Ketchikan, Alaska 99901",
            latitude = 55.3422,
            longitude = -131.6461
        ),
        AddressBookItem(
            id = "address_apartment",
            title = "Apartment",
            address = "2551 Vista Dr #B301, Juneau, Alaska 99801",
            latitude = 58.3019,
            longitude = -134.4197
        ),
        AddressBookItem(
            id = "address_parents",
            title = "Parent's House",
            address = "4821 Ridge Top Cir, Anchorage, Alaska 99502",
            latitude = 61.1349,
            longitude = -149.9594
        )
    )

    private val initialPaymentMethods = listOf(
        PaymentMethodItem(
            id = "card_visa_2512_default",
            brand = "VISA",
            holderName = "ShopSphere User",
            lastFour = "2512",
            isDefault = true,
            isSelected = true
        ),
        PaymentMethodItem(
            id = "card_mastercard_5421",
            brand = "MASTERCARD",
            holderName = "ShopSphere User",
            lastFour = "5421"
        ),
        PaymentMethodItem(
            id = "card_visa_2512_alt",
            brand = "VISA",
            holderName = "ShopSphere User",
            lastFour = "2512"
        )
    )

    private val _addressBook = MutableLiveData<List<AddressBookItem>>(initialAddresses)
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
        fetchOrders()
    }

    // ─── Orders (backend API) ────────────────────────────────────────────────

    fun fetchOrders() {
        viewModelScope.launch {
            _isLoadingOrders.value = true
            repository.getMyOrders()
                .onSuccess { domainOrders ->
                    val fromApi = domainOrders.map { domain ->
                        val statusStep = resolveOrderStatusStep(domain.orderStatus, null)
                        OrderHistoryItem(
                            orderId = domain.orderId.toString(),
                            date = formatApiDate(domain.date),
                            status = normalizeOrderStatusLabel(domain.orderStatus, statusStep),
                            total = formatApiTotal(domain.totalAmount),
                            statusStep = statusStep
                        )
                    }
                    _orderHistory.postValue(fromApi)
                }
                .onFailure {
                    // Keep the current list on failure so the screen doesn't go blank
                }
            _isLoadingOrders.value = false
        }
    }

    // ─── Place order ─────────────────────────────────────────────────────────

    fun placeOrder(
        total: String,
        customerName: String,
        phone: String,
        cartItems: List<PresentationProductResult>
    ): Result<OrderHistoryItem> {
        val sanitizedName = customerName.trim()
        val digitsPhone = phone.filter { it.isDigit() }
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
        if (digitsPhone.length < 8)
            return Result.failure(IllegalStateException("Please enter a valid phone number"))

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
        val destinationLat = validAddress.latitude ?: 30.0444
        val destinationLng = validAddress.longitude ?: 31.2357
        val primaryItem = cartItems.first()

        // Placeholder used only so CheckoutFragment can navigate to TrackOrder immediately.
        // It is never added to _orderHistory — the real order comes from fetchOrders() below.
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
            phone = digitsPhone,
            destinationLat = destinationLat,
            destinationLng = destinationLng,
            currentLat = destinationLat + 0.02,
            currentLng = destinationLng - 0.02,
            statusStep = 0
        )

        // Call the real checkout endpoint, then refresh orders from the backend
        viewModelScope.launch {
            repository.checkout()
                .onSuccess { result ->
                    // Replace placeholder orderId with the real one from the backend if available
                    fetchOrders()
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
    }

    fun getOrderById(orderId: String): OrderHistoryItem? =
        _orderHistory.value.orEmpty().firstOrNull { it.orderId == orderId }

    // ─── Address book ─────────────────────────────────────────────────────────

    fun setAddress(
        nick: String,
        full: String,
        latitude: Double? = null,
        longitude: Double? = null,
        isDefault: Boolean = false
    ): Boolean {
        val sanitizedTitle = nick.trim()
        val sanitizedAddress = full.trim()
        if (sanitizedTitle.length < 2 || sanitizedAddress.length < 8) return false
        if (!isValidCoordinates(latitude, longitude)) return false

        val current = _addressBook.value.orEmpty().ifEmpty { initialAddresses }
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
                        isSelected = true
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
                isSelected = true
            )
            current.map { item ->
                if (isDefault) item.copy(isDefault = false, isSelected = false)
                else item.copy(isSelected = false)
            } + newItem
        }

        _addressBook.value = normalizeAddresses(updated)
        return true
    }

    fun selectAddress(addressId: String) {
        _addressBook.value = normalizeAddresses(
            _addressBook.value.orEmpty().map { item ->
                item.copy(isSelected = item.id == addressId)
            }
        )
    }

    // ─── Payment methods ──────────────────────────────────────────────────────

    fun setCardLastFour(
        lastFour: String,
        holderName: String = "ShopSphere User",
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
        if (address.title.trim().length < 2) return false
        if (address.address.trim().length < 8) return false
        return isValidCoordinates(address.latitude, address.longitude)
    }

    fun isPaymentMethodValid(paymentMethod: PaymentMethodItem?): Boolean {
        if (paymentMethod == null) return false
        if (paymentMethod.brand.trim().isBlank()) return false
        if (paymentMethod.holderName.trim().length < 2) return false
        return paymentMethod.lastFour.length == 4 && paymentMethod.lastFour.all { it.isDigit() }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun resolveOrderStatusStep(status: String, storedStep: Int?): Int {
        val normalized = status.trim().lowercase(Locale.ENGLISH)
            .replace("_", " ").replace("-", " ")
        val derived = when {
            normalized.contains("deliver") || normalized.contains("complete") -> 3
            normalized.contains("transit") || normalized.contains("shipping") ||
                    normalized.contains("shipped") || normalized.contains("out for delivery") -> 2
            normalized.contains("pick") || normalized.contains("dispatch") -> 1
            else -> 0
        }
        return maxOf(storedStep?.coerceIn(0, 3) ?: 0, derived)
    }

    private fun normalizeOrderStatusLabel(status: String, resolvedStep: Int): String {
        return when (resolvedStep.coerceIn(0, 3)) {
            3 -> "Delivered"
            2 -> "In Transit"
            1 -> "Picked"
            else -> "Packing"
        }
    }

    private fun formatApiDate(raw: String): String {
        return runCatching {
            // Backend sends "yyyy-MM-dd"; convert to "MMM dd, yyyy"
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(raw)!!
            SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH).format(parsed)
        }.getOrDefault(raw)
    }

    private fun formatApiTotal(amount: Double): String {
        val formatter = DecimalFormat("#,##0.00")
        return "EGP ${formatter.format(amount)}"
    }

    private fun formatPrice(price: Double): String {
        val formatter = DecimalFormat("#,##0.##")
        return "EGP ${formatter.format(price)}"
    }

    private fun normalizeAddresses(items: List<AddressBookItem>): List<AddressBookItem> {
        val valid = items.filter { isAddressValid(it) }
        if (valid.isEmpty()) return initialAddresses
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
}