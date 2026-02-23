import Foundation
import FirebaseFirestore

struct AddressBookItem: Identifiable, Hashable {
    let id: String
    let title: String
    let address: String
    let latitude: Double?
    let longitude: Double?
    var isDefault: Bool
    var isSelected: Bool

    init(
        id: String = UUID().uuidString,
        title: String,
        address: String,
        latitude: Double? = nil,
        longitude: Double? = nil,
        isDefault: Bool = false,
        isSelected: Bool = false
    ) {
        self.id = id
        self.title = title
        self.address = address
        self.latitude = latitude
        self.longitude = longitude
        self.isDefault = isDefault
        self.isSelected = isSelected
    }

    func toFirestoreMap() -> [String: Any] {
        var map: [String: Any] = [
            "id": id,
            "title": title,
            "address": address,
            "isDefault": isDefault,
            "isSelected": isSelected,
            "updatedAt": FieldValue.serverTimestamp()
        ]
        if let lat = latitude { map["latitude"] = lat }
        if let lng = longitude { map["longitude"] = lng }
        return map
    }

    static func fromFirestore(_ data: [String: Any]) -> AddressBookItem? {
        guard let id = data["id"] as? String,
              let title = data["title"] as? String,
              let address = data["address"] as? String else { return nil }
        return AddressBookItem(
            id: id,
            title: title,
            address: address,
            latitude: data["latitude"] as? Double ?? (data["latitude"] as? Int).map(Double.init),
            longitude: data["longitude"] as? Double ?? (data["longitude"] as? Int).map(Double.init),
            isDefault: data["isDefault"] as? Bool ?? false,
            isSelected: data["isSelected"] as? Bool ?? false
        )
    }

    static func isValid(_ item: AddressBookItem?) -> Bool {
        guard let item else { return false }
        guard item.title.trimmingCharacters(in: .whitespaces).count >= 2 else { return false }
        guard item.address.trimmingCharacters(in: .whitespaces).count >= 8 else { return false }
        return isValidCoordinates(lat: item.latitude, lng: item.longitude)
    }

    static func isValidCoordinates(lat: Double?, lng: Double?) -> Bool {
        guard let lat, let lng else { return false }
        return (-90.0...90.0).contains(lat) && (-180.0...180.0).contains(lng)
    }
}
