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
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CheckoutSharedViewModel : ViewModel() {

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

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var currentUid: String? = null
    private var addressListenerRegistration: ListenerRegistration? = null
    private var paymentListenerRegistration: ListenerRegistration? = null
    private var orderListenerRegistration: ListenerRegistration? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        bindForUser(firebaseAuth.currentUser?.uid)
    }

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

    init {
        auth.addAuthStateListener(authStateListener)
        bindForUser(auth.currentUser?.uid)
    }

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
                if (isDefault) item.copy(isDefault = false, isSelected = false) else item.copy(isSelected = false)
            } + newItem
        }

        val normalized = normalizeAddresses(updated)
        _addressBook.value = normalized
        currentUid?.let { uid -> persistAddressBook(uid, normalized) }
        return true
    }

    fun selectAddress(addressId: String) {
        val normalized = normalizeAddresses(
            _addressBook.value.orEmpty().map { item ->
                item.copy(isSelected = item.id == addressId)
            }
        )
        _addressBook.value = normalized
        currentUid?.let { uid -> persistAddressBook(uid, normalized) }
    }

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
                if (item.id == existing.id) {
                    item.copy(
                        holderName = sanitizedHolder,
                        brand = sanitizedBrand,
                        isSelected = true
                    )
                } else {
                    item.copy(isSelected = false)
                }
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

        val normalized = normalizePaymentMethods(updated)
        _paymentMethods.value = normalized
        currentUid?.let { uid -> persistPaymentMethods(uid, normalized) }
        return true
    }

    fun selectPaymentMethod(cardId: String) {
        val normalized = normalizePaymentMethods(
            _paymentMethods.value.orEmpty().map { item ->
                item.copy(isSelected = item.id == cardId)
            }
        )
        _paymentMethods.value = normalized
        currentUid?.let { uid -> persistPaymentMethods(uid, normalized) }
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
            val quantity = item.quantity.coerceAtLeast(1)
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
        val primaryItem = cartItems.first()

        val newOrder = OrderHistoryItem(
            orderId = "ORD-${UUID.randomUUID().toString().take(8).uppercase()}",
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
            currentLat = startLat,
            currentLng = startLng,
            statusStep = 0
        )

        _orderHistory.value = listOf(newOrder) + _orderHistory.value.orEmpty()
        currentUid?.let { uid -> persistOrder(uid, newOrder) }
        return Result.success(newOrder)
    }

    fun submitOrderReview(orderId: String, rating: Int, comment: String) {
        val sanitizedComment = comment.trim()
        val sanitizedRating = rating.coerceIn(1, 5).toDouble()
        _orderHistory.value = _orderHistory.value.orEmpty().map { order ->
            if (order.orderId == orderId) {
                order.copy(reviewRating = sanitizedRating, reviewComment = sanitizedComment)
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
                                FIELD_REVIEW_RATING to sanitizedRating,
                                FIELD_REVIEW_COMMENT to sanitizedComment,
                                FIELD_UPDATED_AT to com.google.firebase.firestore.FieldValue.serverTimestamp()
                            ),
                            SetOptions.merge()
                        )
                        .await()
                }
            }
        }
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
        val resolvedStatusStep = resolveOrderStatusStep(status, statusStep)
        val normalizedStatus = normalizeOrderStatusLabel(status, resolvedStatusStep)
        _orderHistory.value = _orderHistory.value.orEmpty().map { order ->
            if (order.orderId == orderId) {
                order.copy(
                    currentLat = currentLat,
                    currentLng = currentLng,
                    status = normalizedStatus,
                    statusStep = resolvedStatusStep
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
                                FIELD_STATUS to normalizedStatus,
                                FIELD_STATUS_STEP to resolvedStatusStep,
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
            _addressBook.value = initialAddresses
            _paymentMethods.value = initialPaymentMethods
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
                _addressBook.postValue(normalizeAddresses(mapped))
            }
    }

    private fun observePaymentMethods(uid: String) {
        paymentListenerRegistration = userDocument(uid)
            .collection(PAYMENT_METHODS_COLLECTION)
            .addSnapshotListener { snapshot, _ ->
                val mapped = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toPaymentMethodItem() }
                    .filter { isPaymentMethodValid(it) }
                _paymentMethods.postValue(normalizePaymentMethods(mapped))
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

        resetCollectionIfNeeded(
            uid = uid,
            collectionName = ADDRESS_BOOK_COLLECTION,
            legacyIds = setOf("address_default"),
            existingItems = userRef.collection(ADDRESS_BOOK_COLLECTION).get().await().documents,
            seedData = initialAddresses.associateBy { it.id }.mapValues { (_, item) -> item.toFirestoreMap() }
        )

        resetCollectionIfNeeded(
            uid = uid,
            collectionName = PAYMENT_METHODS_COLLECTION,
            legacyIds = setOf("card_default"),
            existingItems = userRef.collection(PAYMENT_METHODS_COLLECTION).get().await().documents,
            seedData = initialPaymentMethods.associateBy { it.id }.mapValues { (_, item) -> item.toFirestoreMap() }
        )
    }

    private suspend fun resetCollectionIfNeeded(
        uid: String,
        collectionName: String,
        legacyIds: Set<String>,
        existingItems: List<DocumentSnapshot>,
        seedData: Map<String, Map<String, Any?>>
    ) {
        val shouldReset = existingItems.isEmpty() || existingItems.all { snapshot ->
            val itemId = snapshot.getString(FIELD_ID).orEmpty().ifBlank { snapshot.id }
            itemId in legacyIds
        }
        if (!shouldReset) return

        val collection = userDocument(uid).collection(collectionName)
        val batch = firestore.batch()
        existingItems.forEach { snapshot -> batch.delete(collection.document(snapshot.id)) }
        seedData.forEach { (id, value) ->
            batch.set(collection.document(id), value, SetOptions.merge())
        }
        batch.commit().await()
    }

    private fun normalizeAddresses(items: List<AddressBookItem>): List<AddressBookItem> {
        val validItems = items.filter { isAddressValid(it) }
        if (validItems.isEmpty()) return initialAddresses

        val defaultId = validItems.firstOrNull { it.isDefault }?.id ?: validItems.first().id
        val selectedId = validItems.firstOrNull { it.isSelected }?.id ?: defaultId

        return validItems.map { item ->
            item.copy(
                isDefault = item.id == defaultId,
                isSelected = item.id == selectedId
            )
        }
    }

    private fun normalizePaymentMethods(items: List<PaymentMethodItem>): List<PaymentMethodItem> {
        val validItems = items.filter { isPaymentMethodValid(it) }
        if (validItems.isEmpty()) return initialPaymentMethods

        val defaultId = validItems.firstOrNull { it.isDefault }?.id ?: validItems.first().id
        val selectedId = validItems.firstOrNull { it.isSelected }?.id ?: defaultId

        return validItems.map { item ->
            item.copy(
                isDefault = item.id == defaultId,
                isSelected = item.id == selectedId
            )
        }
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
                normalizeAddresses(addresses).forEach { item ->
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
                normalizePaymentMethods(methods).forEach { item ->
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
        FIELD_ITEM_TITLE to itemTitle,
        FIELD_ITEM_SIZE to itemSize,
        FIELD_ITEM_IMAGE_URL to itemImageUrl,
        FIELD_ITEM_PRICE to itemPrice,
        FIELD_REVIEW_RATING to reviewRating,
        FIELD_REVIEW_COMMENT to reviewComment,
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
        val rawStatus = getString(FIELD_STATUS).orEmpty().ifBlank { "Packing" }
        val resolvedStatusStep = resolveOrderStatusStep(
            status = rawStatus,
            storedStep = getLong(FIELD_STATUS_STEP)?.toInt()
        )
        return OrderHistoryItem(
            orderId = orderId,
            date = getString(FIELD_DATE).orEmpty().ifBlank {
                SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH).format(Date())
            },
            status = normalizeOrderStatusLabel(rawStatus, resolvedStatusStep),
            total = getString(FIELD_TOTAL).orEmpty().ifBlank { "EGP 0.00" },
            itemTitle = getString(FIELD_ITEM_TITLE).orEmpty(),
            itemSize = getString(FIELD_ITEM_SIZE).orEmpty().trim().uppercase(),
            itemImageUrl = getString(FIELD_ITEM_IMAGE_URL).orEmpty(),
            itemPrice = getString(FIELD_ITEM_PRICE).orEmpty().ifBlank {
                getString(FIELD_TOTAL).orEmpty().ifBlank { "EGP 0.00" }
            },
            reviewRating = getDouble(FIELD_REVIEW_RATING)
                ?: getLong(FIELD_REVIEW_RATING)?.toDouble()
                ?: 0.0,
            reviewComment = getString(FIELD_REVIEW_COMMENT).orEmpty(),
            address = getString(FIELD_ADDRESS).orEmpty(),
            customerName = getString(FIELD_CUSTOMER_NAME).orEmpty(),
            phone = getString(FIELD_PHONE).orEmpty(),
            destinationLat = getDouble(FIELD_DEST_LAT) ?: getLong(FIELD_DEST_LAT)?.toDouble(),
            destinationLng = getDouble(FIELD_DEST_LNG) ?: getLong(FIELD_DEST_LNG)?.toDouble(),
            currentLat = getDouble(FIELD_CURRENT_LAT) ?: getLong(FIELD_CURRENT_LAT)?.toDouble(),
            currentLng = getDouble(FIELD_CURRENT_LNG) ?: getLong(FIELD_CURRENT_LNG)?.toDouble(),
            statusStep = resolvedStatusStep
        )
    }

    private fun resolveOrderStatusStep(status: String, storedStep: Int?): Int {
        val normalizedStatus = status
            .trim()
            .lowercase(Locale.ENGLISH)
            .replace("_", " ")
            .replace("-", " ")

        val derivedStep = when {
            normalizedStatus.contains("deliver") || normalizedStatus.contains("complete") -> 3
            normalizedStatus.contains("transit") ||
                normalizedStatus.contains("shipping") ||
                normalizedStatus.contains("shipped") ||
                normalizedStatus.contains("out for delivery") -> 2
            normalizedStatus.contains("pick") || normalizedStatus.contains("dispatch") -> 1
            else -> 0
        }

        return maxOf(storedStep?.coerceIn(0, 3) ?: 0, derivedStep)
    }

    private fun normalizeOrderStatusLabel(status: String, resolvedStep: Int): String {
        return when (resolvedStep.coerceIn(0, 3)) {
            3 -> "Delivered"
            2 -> "In Transit"
            1 -> "Picked"
            else -> "Packing"
        }
    }

    private fun userDocument(uid: String) = firestore.collection(USERS_COLLECTION).document(uid)

    private fun formatPrice(price: Double): String {
        val formatter = DecimalFormat("#,##0.##")
        return "EGP ${formatter.format(price)}"
    }

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
        private const val FIELD_ITEM_TITLE = "itemTitle"
        private const val FIELD_ITEM_SIZE = "itemSize"
        private const val FIELD_ITEM_IMAGE_URL = "itemImageUrl"
        private const val FIELD_ITEM_PRICE = "itemPrice"
        private const val FIELD_REVIEW_RATING = "reviewRating"
        private const val FIELD_REVIEW_COMMENT = "reviewComment"
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
