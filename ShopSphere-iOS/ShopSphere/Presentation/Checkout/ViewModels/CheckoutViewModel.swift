import SwiftUI
import FirebaseAuth
import FirebaseFirestore

@Observable
final class CheckoutViewModel {
    // MARK: - Default Data (matches Android)

    static let initialAddress = AddressBookItem(
        id: "address_default",
        title: "Home",
        address: "925 S Chugach St #APT 10, Alaska 99645",
        latitude: 61.2176,
        longitude: -149.8997,
        isDefault: true,
        isSelected: true
    )

    static let initialCard = PaymentMethodItem(
        id: "card_default",
        brand: "VISA",
        holderName: "ShopSphere User",
        lastFour: "1234",
        isDefault: true,
        isSelected: true
    )

    // MARK: - State

    var addressBook: [AddressBookItem] = [CheckoutViewModel.initialAddress]
    var selectedAddress: AddressBookItem? {
        addressBook.first(where: { $0.isSelected })
    }

    var paymentMethods: [PaymentMethodItem] = [CheckoutViewModel.initialCard]
    var selectedPaymentMethod: PaymentMethodItem? {
        paymentMethods.first(where: { $0.isSelected })
    }

    var orderHistory: [OrderHistoryItem] = []

    var isLoading = false
    var errorMessage: String?

    // MARK: - Private

    private let firestore = Firestore.firestore()
    private var addressListener: ListenerRegistration?
    private var paymentListener: ListenerRegistration?
    private var ordersListener: ListenerRegistration?
    private var authListener: AuthStateDidChangeListenerHandle?
    private var currentUid: String?

    // MARK: - Lifecycle

    init() {
        authListener = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            DispatchQueue.main.async { self?.bindForUser(user?.uid) }
        }
        bindForUser(Auth.auth().currentUser?.uid)
    }

    deinit {
        stopListening()
        if let authListener {
            Auth.auth().removeStateDidChangeListener(authListener)
        }
    }

    func stopListening() {
        addressListener?.remove()
        paymentListener?.remove()
        ordersListener?.remove()
        addressListener = nil
        paymentListener = nil
        ordersListener = nil
    }

    // MARK: - Auth Binding

    private func bindForUser(_ uid: String?) {
        guard uid != currentUid else { return }
        stopListening()
        currentUid = uid

        guard let uid, !uid.isEmpty else {
            addressBook = [Self.initialAddress]
            paymentMethods = [Self.initialCard]
            orderHistory = []
            return
        }

        observeAddressBook(uid: uid)
        observePaymentMethods(uid: uid)
        observeOrders(uid: uid)

        Task { await ensureDefaultEntries(uid: uid) }
    }

    // MARK: - Address Book

    private func observeAddressBook(uid: String) {
        addressListener = userDocument(uid)
            .collection("address_book")
            .addSnapshotListener { [weak self] snapshot, _ in
                let mapped = snapshot?.documents.compactMap { doc in
                    AddressBookItem.fromFirestore(doc.data())
                }.filter { AddressBookItem.isValid($0) } ?? []
                DispatchQueue.main.async {
                    guard let self else { return }
                    self.addressBook = self.ensureSelectedAddress(mapped)
                }
            }
    }

    @discardableResult
    func setAddress(nick: String, full: String, latitude: Double? = nil, longitude: Double? = nil) -> Bool {
        let sanitizedTitle = nick.trimmingCharacters(in: .whitespaces)
        let sanitizedAddress = full.trimmingCharacters(in: .whitespaces)
        guard sanitizedTitle.count >= 2, sanitizedAddress.count >= 8 else { return false }
        guard AddressBookItem.isValidCoordinates(lat: latitude, lng: longitude) else { return false }

        let current = addressBook.isEmpty ? [Self.initialAddress] : addressBook
        let existing = current.first {
            $0.title.caseInsensitiveCompare(sanitizedTitle) == .orderedSame &&
            $0.address.caseInsensitiveCompare(sanitizedAddress) == .orderedSame
        }

        let updated: [AddressBookItem]
        if let existing {
            updated = current.map {
                if $0.id == existing.id {
                    return AddressBookItem(
                        id: $0.id, title: $0.title, address: $0.address,
                        latitude: latitude ?? $0.latitude,
                        longitude: longitude ?? $0.longitude,
                        isDefault: $0.isDefault, isSelected: true
                    )
                } else {
                    var item = $0
                    item.isSelected = false
                    return item
                }
            }
        } else {
            let newItem = AddressBookItem(
                id: UUID().uuidString,
                title: sanitizedTitle,
                address: sanitizedAddress,
                latitude: latitude,
                longitude: longitude,
                isDefault: current.isEmpty,
                isSelected: true
            )
            updated = current.map {
                var item = $0
                item.isSelected = false
                return item
            } + [newItem]
        }

        addressBook = updated
        if let uid = currentUid {
            persistAddressBook(uid: uid, addresses: updated)
        }
        return true
    }

    func addAddress(_ address: AddressBookItem) {
        setAddress(
            nick: address.title,
            full: address.address,
            latitude: address.latitude,
            longitude: address.longitude
        )
    }

    func selectAddress(_ addressId: String) {
        addressBook = addressBook.map {
            var item = $0
            item.isSelected = item.id == addressId
            return item
        }
        if let uid = currentUid {
            persistAddressBook(uid: uid, addresses: addressBook)
        }
    }

    // MARK: - Payment Methods

    private func observePaymentMethods(uid: String) {
        paymentListener = userDocument(uid)
            .collection("payment_methods")
            .addSnapshotListener { [weak self] snapshot, _ in
                let mapped = snapshot?.documents.compactMap { doc in
                    PaymentMethodItem.fromFirestore(doc.data())
                }.filter { PaymentMethodItem.isValid($0) } ?? []
                DispatchQueue.main.async {
                    guard let self else { return }
                    self.paymentMethods = self.ensureSelectedPayment(mapped)
                }
            }
    }

    @discardableResult
    func setCardLastFour(lastFour: String, holderName: String = "ShopSphere User", brand: String = "VISA") -> Bool {
        let sanitized = String(lastFour.filter(\.isNumber).suffix(4))
        let sanitizedHolder = holderName.trimmingCharacters(in: .whitespaces)
        let sanitizedBrand = brand.trimmingCharacters(in: .whitespaces)
        guard sanitized.count == 4, sanitizedHolder.count >= 2, !sanitizedBrand.isEmpty else { return false }

        let current = paymentMethods
        let existing = current.first {
            $0.lastFour == sanitized && $0.brand.caseInsensitiveCompare(sanitizedBrand) == .orderedSame
        }

        let updated: [PaymentMethodItem]
        if let existing {
            updated = current.map {
                var item = $0
                item.isSelected = item.id == existing.id
                return item
            }
        } else {
            let newItem = PaymentMethodItem(
                id: UUID().uuidString,
                brand: sanitizedBrand,
                holderName: sanitizedHolder,
                lastFour: sanitized,
                isDefault: current.isEmpty,
                isSelected: true
            )
            updated = current.map {
                var item = $0
                item.isSelected = false
                return item
            } + [newItem]
        }

        paymentMethods = updated
        if let uid = currentUid {
            persistPaymentMethods(uid: uid, methods: updated)
        }
        return true
    }

    func addPaymentMethod(_ method: PaymentMethodItem) {
        setCardLastFour(
            lastFour: method.lastFour,
            holderName: method.holderName,
            brand: method.brand
        )
    }

    func selectPaymentMethod(_ cardId: String) {
        paymentMethods = paymentMethods.map {
            var item = $0
            item.isSelected = item.id == cardId
            return item
        }
        if let uid = currentUid {
            persistPaymentMethods(uid: uid, methods: paymentMethods)
        }
    }

    // MARK: - Orders

    private func observeOrders(uid: String) {
        ordersListener = userDocument(uid)
            .collection("orders")
            .order(by: "createdAtEpoch", descending: true)
            .addSnapshotListener { [weak self] snapshot, _ in
                let orders = snapshot?.documents.compactMap { doc in
                    OrderHistoryItem.fromFirestore(doc.data())
                } ?? []
                DispatchQueue.main.async {
                    self?.orderHistory = orders
                }
            }
    }

    func placeOrder(
        total: String,
        customerName: String,
        phone: String,
        cartItems: [CartItem]
    ) -> Result<OrderHistoryItem, Error> {
        let sanitizedName = customerName.trimmingCharacters(in: .whitespaces)
        let digitsPhone = phone.filter(\.isNumber)

        guard !cartItems.isEmpty else {
            return .failure(NSError(domain: "", code: 0, userInfo: [NSLocalizedDescriptionKey: "Cart is empty"]))
        }
        guard AddressBookItem.isValid(selectedAddress) else {
            return .failure(NSError(domain: "", code: 0, userInfo: [NSLocalizedDescriptionKey: "Please select a valid delivery address"]))
        }
        guard PaymentMethodItem.isValid(selectedPaymentMethod) else {
            return .failure(NSError(domain: "", code: 0, userInfo: [NSLocalizedDescriptionKey: "Please select a valid payment method"]))
        }
        guard !sanitizedName.isEmpty else {
            return .failure(NSError(domain: "", code: 0, userInfo: [NSLocalizedDescriptionKey: "Please enter a valid customer name"]))
        }
        guard digitsPhone.count >= 8 else {
            return .failure(NSError(domain: "", code: 0, userInfo: [NSLocalizedDescriptionKey: "Please enter a valid phone number"]))
        }

        // Stock validation
        for item in cartItems {
            let stock = max(item.product.stockCount, 0)
            let quantity = max(item.quantity, 1)
            if quantity > stock || stock <= 0 {
                let msg = stock <= 0
                    ? "'\(item.product.title)' is out of stock"
                    : "Only \(stock) left for '\(item.product.title)'"
                return .failure(NSError(domain: "", code: 0, userInfo: [NSLocalizedDescriptionKey: msg]))
            }
        }

        guard let address = selectedAddress else {
            return .failure(NSError(domain: "", code: 0, userInfo: [NSLocalizedDescriptionKey: "No address selected"]))
        }

        let destinationLat = address.latitude ?? 30.0444
        let destinationLng = address.longitude ?? 31.2357
        let startLat = destinationLat + 0.02
        let startLng = destinationLng - 0.02

        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "MMM dd, yyyy"
        dateFormatter.locale = Locale(identifier: "en_US")

        let newOrder = OrderHistoryItem(
            date: dateFormatter.string(from: Date()),
            status: "Packing",
            total: total,
            address: address.address,
            customerName: sanitizedName,
            phone: digitsPhone,
            destinationLat: destinationLat,
            destinationLng: destinationLng,
            currentLat: startLat,
            currentLng: startLng,
            statusStep: 0
        )

        orderHistory.insert(newOrder, at: 0)

        if let uid = currentUid {
            persistOrder(uid: uid, order: newOrder)
        }

        return .success(newOrder)
    }

    func updateOrderTracking(
        orderId: String,
        currentLat: Double,
        currentLng: Double,
        status: String,
        statusStep: Int
    ) {
        orderHistory = orderHistory.map { order in
            guard order.orderId == orderId else { return order }
            var updated = order
            updated.currentLat = currentLat
            updated.currentLng = currentLng
            updated.status = status
            updated.statusStep = statusStep
            return updated
        }

        guard let uid = currentUid else { return }
        userDocument(uid)
            .collection("orders")
            .document(orderId)
            .setData([
                "currentLat": currentLat,
                "currentLng": currentLng,
                "status": status,
                "statusStep": statusStep,
                "updatedAt": FieldValue.serverTimestamp()
            ], merge: true)
    }

    func getOrder(by id: String) -> OrderHistoryItem? {
        orderHistory.first { $0.orderId == id }
    }

    // MARK: - Firestore Helpers

    private func userDocument(_ uid: String) -> DocumentReference {
        firestore.collection(APIConstants.usersCollection).document(uid)
    }

    private func ensureDefaultEntries(uid: String) async {
        let userRef = userDocument(uid)

        try? await userRef.setData([
            "updatedAt": FieldValue.serverTimestamp()
        ], merge: true)

        let addresses = try? await userRef.collection("address_book").limit(to: 1).getDocuments()
        if addresses?.documents.isEmpty ?? true {
            try? await userRef.collection("address_book")
                .document(Self.initialAddress.id)
                .setData(Self.initialAddress.toFirestoreMap())
        }

        let payments = try? await userRef.collection("payment_methods").limit(to: 1).getDocuments()
        if payments?.documents.isEmpty ?? true {
            try? await userRef.collection("payment_methods")
                .document(Self.initialCard.id)
                .setData(Self.initialCard.toFirestoreMap())
        }
    }

    private func ensureSelectedAddress(_ items: [AddressBookItem]) -> [AddressBookItem] {
        let validItems = items.filter { AddressBookItem.isValid($0) }
        if validItems.isEmpty { return [Self.initialAddress] }
        if validItems.contains(where: { $0.isSelected }) { return validItems }
        let fallback = validItems.first(where: { $0.isDefault }) ?? validItems[0]
        return validItems.map {
            var item = $0
            item.isSelected = item.id == fallback.id
            return item
        }
    }

    private func ensureSelectedPayment(_ items: [PaymentMethodItem]) -> [PaymentMethodItem] {
        let validItems = items.filter { PaymentMethodItem.isValid($0) }
        if validItems.isEmpty { return [Self.initialCard] }
        if validItems.contains(where: { $0.isSelected }) { return validItems }
        let fallback = validItems.first(where: { $0.isDefault }) ?? validItems[0]
        return validItems.map {
            var item = $0
            item.isSelected = item.id == fallback.id
            return item
        }
    }

    private func persistAddressBook(uid: String, addresses: [AddressBookItem]) {
        let collection = userDocument(uid).collection("address_book")
        let batch = firestore.batch()
        for item in ensureSelectedAddress(addresses) {
            batch.setData(item.toFirestoreMap(), forDocument: collection.document(item.id), merge: true)
        }
        batch.commit(completion: nil)
    }

    private func persistPaymentMethods(uid: String, methods: [PaymentMethodItem]) {
        let collection = userDocument(uid).collection("payment_methods")
        let batch = firestore.batch()
        for item in ensureSelectedPayment(methods) {
            batch.setData(item.toFirestoreMap(), forDocument: collection.document(item.id), merge: true)
        }
        batch.commit(completion: nil)
    }

    private func persistOrder(uid: String, order: OrderHistoryItem) {
        userDocument(uid)
            .collection("orders")
            .document(order.orderId)
            .setData(order.toFirestoreMap(), merge: true)
    }
}
