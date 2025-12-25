package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.shopsphere.CleanArchitecture.ui.models.AddressBookItem
import com.example.shopsphere.CleanArchitecture.ui.models.OrderHistoryItem
import com.example.shopsphere.CleanArchitecture.ui.models.PaymentMethodItem
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CheckoutSharedViewModel : ViewModel() {

    private val initialAddress = AddressBookItem(
        id = "address_default",
        title = "Home",
        address = "925 S Chugach St #APT 10, Alaska 99645",
        latitude = 61.2176,
        longitude = -149.8997,
        isDefault = true,
        isSelected = true
    )

    private val initialCard = PaymentMethodItem(
        id = "card_default",
        brand = "VISA",
        holderName = "ShopSphere User",
        lastFour = "1234",
        isDefault = true,
        isSelected = true
    )

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var currentUid: String? = null
    private var addressListenerRegistration: ListenerRegistration? = null
    private var paymentListenerRegistration: ListenerRegistration? = null
    private var orderListenerRegistration: ListenerRegistration? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        bindForUser(firebaseAuth.currentUser?.uid)
    }

    private val _addressBook = MutableLiveData<List<AddressBookItem>>(listOf(initialAddress))
    val addressBook: LiveData<List<AddressBookItem>> = _addressBook

    private val _paymentMethods = MutableLiveData<List<PaymentMethodItem>>(listOf(initialCard))
    val paymentMethods: LiveData<List<PaymentMethodItem>> = _paymentMethods

    val selectedAddress: LiveData<AddressBookItem?> =
        _addressBook.map { list -> list.firstOrNull { it.isSelected } }

    val selectedPaymentMethod: LiveData<PaymentMethodItem?> =
        _paymentMethods.map { list -> list.firstOrNull { it.isSelected } }

    // Backward-compatible observers for old Checkout bindings.
    val address: LiveData<Pair<String, String>?> =
        selectedAddress.map { item -> item?.let { it.title to it.address } }

    val cardLastFour: LiveData<String?> =
        selectedPaymentMethod.map { item -> item?.lastFour }

    private val _orderHistory = MutableLiveData<List<OrderHistoryItem>>(emptyList())
    val orderHistory: LiveData<List<OrderHistoryItem>> = _orderHistory

    init {
        auth.addAuthStateListener(authStateListener)
        bindForUser(auth.currentUser?.uid)
    }

    fun setAddress(
        nick: String,
        full: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): Boolean {
        val sanitizedTitle = nick.trim()
        val sanitizedAddress = full.trim()
        if (sanitizedTitle.length < 2 || sanitizedAddress.length < 8) {
            return false
        }
        if (!isValidCoordinates(latitude, longitude)) {
            return false
        }

        val current = _addressBook.value.orEmpty().ifEmpty { listOf(initialAddress) }
        val existing = current.firstOrNull {
            it.title.equals(sanitizedTitle, ignoreCase = true) &&
                it.address.equals(sanitizedAddress, ignoreCase = true)
        }
        val updated = if (existing != null) {
            current.map {
                if (it.id == existing.id) {
                    it.copy(
                        latitude = latitude ?: it.latitude,
                        longitude = longitude ?: it.longitude,
                        isSelected = true
                    )
                } else {
                    it.copy(isSelected = false)
                }
            }
        } else {
            val newItem = AddressBookItem(
                id = UUID.randomUUID().toString(),
                title = sanitizedTitle,
                address = sanitizedAddress,
                latitude = latitude,
                longitude = longitude,
                isDefault = current.isEmpty(),
                isSelected = true
            )
            current.map { it.copy(isSelected = false) } + newItem
        }
        _addressBook.value = updated

        currentUid?.let { uid ->
            persistAddressBook(uid, updated)
        }
        return true
    }

    fun selectAddress(addressId: String) {
        val updated = _addressBook.value.orEmpty().map {
            it.copy(isSelected = it.id == addressId)
        }
        _addressBook.value = updated
        currentUid?.let { uid ->
            persistAddressBook(uid, updated)
        }
    }

    fun setCardLastFour(
        lastFour: String,
        holderName: String = "ShopSphere User",
        brand: String = "VISA"
    ): Boolean {
        val sanitized = lastFour.filter { it.isDigit() }.takeLast(4)
        val sanitizedHolder = holderName.trim()
        val sanitizedBrand = brand.trim()
        if (sanitized.length != 4) return false
        if (sanitizedHolder.length < 2) return false
        if (sanitizedBrand.isBlank()) return false
        val current = _paymentMethods.value.orEmpty()
        val existing = current.firstOrNull {
            it.lastFour == sanitized && it.brand.equals(sanitizedBrand, ignoreCase = true)
        }
        val updated = if (existing != null) {
            current.map {
                if (it.id == existing.id) it.copy(isSelected = true) else it.copy(isSelected = false)
            }
        } else {
            val newItem = PaymentMethodItem(
                id = UUID.randomUUID().toString(),
                brand = sanitizedBrand,
                holderName = sanitizedHolder,
                lastFour = sanitized,
                isDefault = current.isEmpty(),
                isSelected = true
            )
            current.map { it.copy(isSelected = false) } + newItem
        }
        _paymentMethods.value = updated

        currentUid?.let { uid ->
            persistPaymentMethods(uid, updated)
        }
        return true
    }

    fun selectPaymentMethod(cardId: String) {
        val updated = _paymentMethods.value.orEmpty().map {
            it.copy(isSelected = it.id == cardId)
        }
        _paymentMethods.value = updated
        currentUid?.let { uid ->
            persistPaymentMethods(uid, updated)
        }
    }

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
        if (cartItems.isEmpty()) {
            return Result.failure(IllegalStateException("Cart is empty"))
        }
        if (!isAddressValid(address)) {
            return Result.failure(IllegalStateException("Please select a valid delivery address"))
        }
        if (!isPaymentMethodValid(paymentMethod)) {
            return Result.failure(IllegalStateException("Please select a valid payment method"))
        }
        if (sanitizedName.isBlank()) {
            return Result.failure(IllegalStateException("Please enter a valid customer name"))
        }
        if (digitsPhone.length < 8) {
            return Result.failure(IllegalStateException("Please enter a valid phone number"))
        }
        val invalidStockItem = cartItems.firstOrNull { item ->
            val stock = item.rating.count.coerceAtLeast(0)
            val quantity = (item.quantity).coerceAtLeast(1)
            quantity > stock || stock <= 0
        }
        if (invalidStockItem != null) {
            val stock = invalidStockItem.rating.count.coerceAtLeast(0)
            return Result.failure(
                IllegalStateException(
                    if (stock <= 0) {
                        "'${invalidStockItem.title}' is out of stock"
                    } else {
                        "Only $stock left for '${invalidStockItem.title}'"
                    }
                )
            )
        }

        val validAddress = address!!

        val destinationLat = validAddress.latitude ?: 30.0444
        val destinationLng = validAddress.longitude ?: 31.2357
        val startLat = destinationLat + 0.02
        val startLng = destinationLng - 0.02

        val newOrder = OrderHistoryItem(
            orderId = "ORD-${UUID.randomUUID().toString().take(8).uppercase()}",
            date = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH).format(Date()),
            status = "Packing",
            total = total,
            address = validAddress.address,
            customerName = sanitizedName,
            phone = digitsPhone,
            destinationLat = destinationLat,
            destinationLng = destinationLng,
            currentLat = startLat,
            currentLng = startLng,
            statusStep = 0
        )

        _orderHistory.value = listOf(newOrder) + _orderHistory.value.orEmpty()

        currentUid?.let { uid ->
            persistOrder(uid, newOrder)
        }
        return Result.success(newOrder)
    }

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

    fun updateOrderTracking(
        orderId: String,
        currentLat: Double,
        currentLng: Double,
        status: String,
        statusStep: Int
    ) {
        _orderHistory.value = _orderHistory.value.orEmpty().map { order ->
            if (order.orderId == orderId) {
                order.copy(
                    currentLat = currentLat,
                    currentLng = currentLng,
                    status = status,
                    statusStep = statusStep
                )
            } else {
                order
            }
        }

        currentUid?.let { uid ->
            viewModelScope.launch {
                runCatching {
                    userDocument(uid)
                        .collection(ORDERS_COLLECTION)
                        .document(orderId)
                        .set(
                            mapOf(
                                FIELD_CURRENT_LAT to currentLat,
                                FIELD_CURRENT_LNG to currentLng,
                                FIELD_STATUS to status,
                                FIELD_STATUS_STEP to statusStep,
                                FIELD_UPDATED_AT to com.google.firebase.firestore.FieldValue.serverTimestamp()
                            ),
                            SetOptions.merge()
                        )
                        .await()
                }
            }
        }
    }

    fun getOrderById(orderId: String): OrderHistoryItem? {
        return _orderHistory.value.orEmpty().firstOrNull { it.orderId == orderId }
    }

    private fun bindForUser(uid: String?) {
        if (uid == currentUid) return
        clearFirestoreListeners()
        currentUid = uid

        if (uid.isNullOrBlank()) {
            _addressBook.value = listOf(initialAddress)
            _paymentMethods.value = listOf(initialCard)
            _orderHistory.value = emptyList()
            return
        }

        observeAddressBook(uid)
        observePaymentMethods(uid)
        observeOrders(uid)

        viewModelScope.launch {
            ensureDefaultEntries(uid)
        }
    }

    private fun observeAddressBook(uid: String) {
        addressListenerRegistration = userDocument(uid)
            .collection(ADDRESS_BOOK_COLLECTION)
            .addSnapshotListener { snapshot, _ ->
                val mapped = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toAddressBookItem() }
                    .filter { isAddressValid(it) }
                _addressBook.postValue(ensureSelectedAddress(mapped))
            }
    }

    private fun observePaymentMethods(uid: String) {
        paymentListenerRegistration = userDocument(uid)
            .collection(PAYMENT_METHODS_COLLECTION)
            .addSnapshotListener { snapshot, _ ->
                val mapped = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toPaymentMethodItem() }
                    .filter { isPaymentMethodValid(it) }
                _paymentMethods.postValue(ensureSelectedPayment(mapped))
            }
    }

    private fun observeOrders(uid: String) {
        orderListenerRegistration = userDocument(uid)
            .collection(ORDERS_COLLECTION)
            .orderBy(FIELD_CREATED_AT_EPOCH, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val mapped = snapshot?.documents.orEmpty().mapNotNull { it.toOrderHistoryItem() }
                _orderHistory.postValue(mapped)
            }
    }

    private suspend fun ensureDefaultEntries(uid: String) {
        val userRef = userDocument(uid)
        userRef.set(
            mapOf(FIELD_UPDATED_AT to com.google.firebase.firestore.FieldValue.serverTimestamp()),
            SetOptions.merge()
        ).await()

        val addresses = userRef.collection(ADDRESS_BOOK_COLLECTION).limit(1).get().await()
        if (addresses.isEmpty) {
            userRef.collection(ADDRESS_BOOK_COLLECTION)
                .document(initialAddress.id)
                .set(initialAddress.toFirestoreMap())
                .await()
        }

        val paymentMethods = userRef.collection(PAYMENT_METHODS_COLLECTION).limit(1).get().await()
        if (paymentMethods.isEmpty) {
            userRef.collection(PAYMENT_METHODS_COLLECTION)
                .document(initialCard.id)
                .set(initialCard.toFirestoreMap())
                .await()
        }
    }

    private fun ensureSelectedAddress(items: List<AddressBookItem>): List<AddressBookItem> {
        val validItems = items.filter { isAddressValid(it) }
        if (validItems.isEmpty()) return listOf(initialAddress)
        if (validItems.any { it.isSelected }) return validItems
        val fallback = validItems.firstOrNull { it.isDefault } ?: validItems.first()
        return validItems.map { it.copy(isSelected = it.id == fallback.id) }
    }

    private fun ensureSelectedPayment(items: List<PaymentMethodItem>): List<PaymentMethodItem> {
        val validItems = items.filter { isPaymentMethodValid(it) }
        if (validItems.isEmpty()) return listOf(initialCard)
        if (validItems.any { it.isSelected }) return validItems
        val fallback = validItems.firstOrNull { it.isDefault } ?: validItems.first()
        return validItems.map { it.copy(isSelected = it.id == fallback.id) }
    }

    private fun isValidCoordinates(latitude: Double?, longitude: Double?): Boolean {
        if (latitude == null || longitude == null) return false
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    private fun persistAddressBook(uid: String, addresses: List<AddressBookItem>) {
        viewModelScope.launch {
            runCatching {
                val collection = userDocument(uid).collection(ADDRESS_BOOK_COLLECTION)
                val batch = firestore.batch()
                ensureSelectedAddress(addresses).forEach { item ->
                    batch.set(collection.document(item.id), item.toFirestoreMap(), SetOptions.merge())
                }
                batch.commit().await()
            }
        }
    }

    private fun persistPaymentMethods(uid: String, methods: List<PaymentMethodItem>) {
        viewModelScope.launch {
            runCatching {
                val collection = userDocument(uid).collection(PAYMENT_METHODS_COLLECTION)
                val batch = firestore.batch()
                ensureSelectedPayment(methods).forEach { item ->
                    batch.set(collection.document(item.id), item.toFirestoreMap(), SetOptions.merge())
                }
                batch.commit().await()
            }
        }
    }

    private fun persistOrder(uid: String, order: OrderHistoryItem) {
        viewModelScope.launch {
            runCatching {
                userDocument(uid)
                    .collection(ORDERS_COLLECTION)
                    .document(order.orderId)
                    .set(order.toFirestoreMap(), SetOptions.merge())
                    .await()
            }
        }
    }

    private fun AddressBookItem.toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_ID to id,
        FIELD_TITLE to title,
        FIELD_ADDRESS to address,
        FIELD_LATITUDE to latitude,
        FIELD_LONGITUDE to longitude,
        FIELD_IS_DEFAULT to isDefault,
        FIELD_IS_SELECTED to isSelected,
        FIELD_UPDATED_AT to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )

    private fun PaymentMethodItem.toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_ID to id,
        FIELD_BRAND to brand,
        FIELD_HOLDER_NAME to holderName,
        FIELD_LAST_FOUR to lastFour,
        FIELD_IS_DEFAULT to isDefault,
        FIELD_IS_SELECTED to isSelected,
        FIELD_UPDATED_AT to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )

    private fun OrderHistoryItem.toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_ORDER_ID to orderId,
        FIELD_DATE to date,
        FIELD_STATUS to status,
        FIELD_TOTAL to total,
        FIELD_ADDRESS to address,
        FIELD_CUSTOMER_NAME to customerName,
        FIELD_PHONE to phone,
        FIELD_DEST_LAT to destinationLat,
        FIELD_DEST_LNG to destinationLng,
        FIELD_CURRENT_LAT to currentLat,
        FIELD_CURRENT_LNG to currentLng,
        FIELD_STATUS_STEP to statusStep,
        FIELD_CREATED_AT to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        FIELD_CREATED_AT_EPOCH to System.currentTimeMillis(),
        FIELD_UPDATED_AT to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )

    private fun DocumentSnapshot.toAddressBookItem(): AddressBookItem? {
        return AddressBookItem(
            id = getString(FIELD_ID).orEmpty().ifBlank { id },
            title = getString(FIELD_TITLE).orEmpty().ifBlank { "Address" },
            address = getString(FIELD_ADDRESS).orEmpty(),
            latitude = getDouble(FIELD_LATITUDE) ?: getLong(FIELD_LATITUDE)?.toDouble(),
            longitude = getDouble(FIELD_LONGITUDE) ?: getLong(FIELD_LONGITUDE)?.toDouble(),
            isDefault = getBoolean(FIELD_IS_DEFAULT) ?: false,
            isSelected = getBoolean(FIELD_IS_SELECTED) ?: false
        )
    }

    private fun DocumentSnapshot.toPaymentMethodItem(): PaymentMethodItem? {
        val lastFour = getString(FIELD_LAST_FOUR).orEmpty()
        if (lastFour.isBlank()) return null
        return PaymentMethodItem(
            id = getString(FIELD_ID).orEmpty().ifBlank { id },
            brand = getString(FIELD_BRAND).orEmpty().ifBlank { "CARD" },
            holderName = getString(FIELD_HOLDER_NAME).orEmpty().ifBlank { "ShopSphere User" },
            lastFour = lastFour,
            isDefault = getBoolean(FIELD_IS_DEFAULT) ?: false,
            isSelected = getBoolean(FIELD_IS_SELECTED) ?: false
        )
    }

    private fun DocumentSnapshot.toOrderHistoryItem(): OrderHistoryItem? {
        val orderId = getString(FIELD_ORDER_ID).orEmpty().ifBlank { id }
        return OrderHistoryItem(
            orderId = orderId,
            date = getString(FIELD_DATE).orEmpty().ifBlank {
                SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH).format(Date())
            },
            status = getString(FIELD_STATUS).orEmpty().ifBlank { "Packing" },
            total = getString(FIELD_TOTAL).orEmpty().ifBlank { "EGP0.00" },
            address = getString(FIELD_ADDRESS).orEmpty(),
            customerName = getString(FIELD_CUSTOMER_NAME).orEmpty(),
            phone = getString(FIELD_PHONE).orEmpty(),
            destinationLat = getDouble(FIELD_DEST_LAT) ?: getLong(FIELD_DEST_LAT)?.toDouble(),
            destinationLng = getDouble(FIELD_DEST_LNG) ?: getLong(FIELD_DEST_LNG)?.toDouble(),
            currentLat = getDouble(FIELD_CURRENT_LAT) ?: getLong(FIELD_CURRENT_LAT)?.toDouble(),
            currentLng = getDouble(FIELD_CURRENT_LNG) ?: getLong(FIELD_CURRENT_LNG)?.toDouble(),
            statusStep = getLong(FIELD_STATUS_STEP)?.toInt() ?: 0
        )
    }

    private fun userDocument(uid: String) = firestore.collection(USERS_COLLECTION).document(uid)

    private fun clearFirestoreListeners() {
        addressListenerRegistration?.remove()
        paymentListenerRegistration?.remove()
        orderListenerRegistration?.remove()
        addressListenerRegistration = null
        paymentListenerRegistration = null
        orderListenerRegistration = null
    }

    override fun onCleared() {
        super.onCleared()
        clearFirestoreListeners()
        auth.removeAuthStateListener(authStateListener)
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val ADDRESS_BOOK_COLLECTION = "address_book"
        private const val PAYMENT_METHODS_COLLECTION = "payment_methods"
        private const val ORDERS_COLLECTION = "orders"

        private const val FIELD_ID = "id"
        private const val FIELD_TITLE = "title"
        private const val FIELD_ADDRESS = "address"
        private const val FIELD_LATITUDE = "latitude"
        private const val FIELD_LONGITUDE = "longitude"
        private const val FIELD_IS_DEFAULT = "isDefault"
        private const val FIELD_IS_SELECTED = "isSelected"
        private const val FIELD_UPDATED_AT = "updatedAt"

        private const val FIELD_BRAND = "brand"
        private const val FIELD_HOLDER_NAME = "holderName"
        private const val FIELD_LAST_FOUR = "lastFour"

        private const val FIELD_ORDER_ID = "orderId"
        private const val FIELD_DATE = "date"
        private const val FIELD_STATUS = "status"
        private const val FIELD_TOTAL = "total"
        private const val FIELD_CUSTOMER_NAME = "customerName"
        private const val FIELD_PHONE = "phone"
        private const val FIELD_DEST_LAT = "destinationLat"
        private const val FIELD_DEST_LNG = "destinationLng"
        private const val FIELD_CURRENT_LAT = "currentLat"
        private const val FIELD_CURRENT_LNG = "currentLng"
        private const val FIELD_STATUS_STEP = "statusStep"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_CREATED_AT_EPOCH = "createdAtEpoch"
    }
}
