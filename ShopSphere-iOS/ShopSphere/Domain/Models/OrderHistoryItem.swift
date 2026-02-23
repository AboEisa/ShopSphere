import Foundation
import FirebaseFirestore

struct OrderHistoryItem: Identifiable, Hashable {
    let orderId: String
    var date: String
    var status: String
    let total: String
    let address: String
    let customerName: String
    let phone: String
    var destinationLat: Double?
    var destinationLng: Double?
    var currentLat: Double?
    var currentLng: Double?
    var statusStep: Int

    var id: String { orderId }

    init(
        orderId: String = "ORD-\(UUID().uuidString.prefix(8).uppercased())",
        date: String = "",
        status: String = "Packing",
        total: String = "",
        address: String = "",
        customerName: String = "",
        phone: String = "",
        destinationLat: Double? = nil,
        destinationLng: Double? = nil,
        currentLat: Double? = nil,
        currentLng: Double? = nil,
        statusStep: Int = 0
    ) {
        self.orderId = orderId
        self.date = date
        self.status = status
        self.total = total
        self.address = address
        self.customerName = customerName
        self.phone = phone
        self.destinationLat = destinationLat
        self.destinationLng = destinationLng
        self.currentLat = currentLat
        self.currentLng = currentLng
        self.statusStep = statusStep
    }

    static let statusLabels = ["Packing", "Picked", "In Transit", "Delivered"]

    static let stepPacking = 0
    static let stepPicked = 1
    static let stepInTransit = 2
    static let stepDelivered = 3

    func toFirestoreMap() -> [String: Any] {
        var map: [String: Any] = [
            "orderId": orderId,
            "date": date,
            "status": status,
            "total": total,
            "address": address,
            "customerName": customerName,
            "phone": phone,
            "statusStep": statusStep,
            "createdAt": FieldValue.serverTimestamp(),
            "createdAtEpoch": Int(Date().timeIntervalSince1970 * 1000),
            "updatedAt": FieldValue.serverTimestamp()
        ]
        if let lat = destinationLat { map["destinationLat"] = lat }
        if let lng = destinationLng { map["destinationLng"] = lng }
        if let lat = currentLat { map["currentLat"] = lat }
        if let lng = currentLng { map["currentLng"] = lng }
        return map
    }

    static func fromFirestore(_ data: [String: Any]) -> OrderHistoryItem? {
        let orderId = data["orderId"] as? String ?? data["id"] as? String ?? ""
        guard !orderId.isEmpty else { return nil }

        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "MMM dd, yyyy"
        dateFormatter.locale = Locale(identifier: "en_US")

        return OrderHistoryItem(
            orderId: orderId,
            date: data["date"] as? String ?? dateFormatter.string(from: Date()),
            status: data["status"] as? String ?? "Packing",
            total: data["total"] as? String ?? "EGP0.00",
            address: data["address"] as? String ?? "",
            customerName: data["customerName"] as? String ?? "",
            phone: data["phone"] as? String ?? "",
            destinationLat: data["destinationLat"] as? Double ?? (data["destinationLat"] as? Int).map(Double.init),
            destinationLng: data["destinationLng"] as? Double ?? (data["destinationLng"] as? Int).map(Double.init),
            currentLat: data["currentLat"] as? Double ?? (data["currentLat"] as? Int).map(Double.init),
            currentLng: data["currentLng"] as? Double ?? (data["currentLng"] as? Int).map(Double.init),
            statusStep: data["statusStep"] as? Int ?? (data["statusStep"] as? NSNumber)?.intValue ?? 0
        )
    }
}
