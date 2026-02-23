import Foundation
import FirebaseFirestore

struct PaymentMethodItem: Identifiable, Hashable {
    let id: String
    let brand: String
    let holderName: String
    let lastFour: String
    var isDefault: Bool
    var isSelected: Bool

    init(
        id: String = UUID().uuidString,
        brand: String,
        holderName: String,
        lastFour: String,
        isDefault: Bool = false,
        isSelected: Bool = false
    ) {
        self.id = id
        self.brand = brand
        self.holderName = holderName
        self.lastFour = lastFour
        self.isDefault = isDefault
        self.isSelected = isSelected
    }

    var maskedNumber: String {
        "**** **** **** \(lastFour)"
    }

    var brandIcon: String {
        switch brand.lowercased() {
        case "visa": return "creditcard"
        case "mastercard": return "creditcard.fill"
        default: return "creditcard"
        }
    }

    func toFirestoreMap() -> [String: Any] {
        [
            "id": id,
            "brand": brand,
            "holderName": holderName,
            "lastFour": lastFour,
            "isDefault": isDefault,
            "isSelected": isSelected,
            "updatedAt": FieldValue.serverTimestamp()
        ]
    }

    static func fromFirestore(_ data: [String: Any]) -> PaymentMethodItem? {
        guard let id = data["id"] as? String,
              let brand = data["brand"] as? String,
              let holderName = data["holderName"] as? String,
              let lastFour = data["lastFour"] as? String else { return nil }
        return PaymentMethodItem(
            id: id,
            brand: brand,
            holderName: holderName,
            lastFour: lastFour,
            isDefault: data["isDefault"] as? Bool ?? false,
            isSelected: data["isSelected"] as? Bool ?? false
        )
    }

    static func isValid(_ item: PaymentMethodItem?) -> Bool {
        guard let item else { return false }
        guard !item.brand.trimmingCharacters(in: .whitespaces).isEmpty else { return false }
        guard item.holderName.trimmingCharacters(in: .whitespaces).count >= 2 else { return false }
        return item.lastFour.count == 4 && item.lastFour.allSatisfy(\.isNumber)
    }
}
