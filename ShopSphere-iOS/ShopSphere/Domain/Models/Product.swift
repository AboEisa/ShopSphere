import Foundation

struct Product: Identifiable, Hashable {
    let id: Int
    let title: String
    let description: String
    let price: Double
    let image: String
    let category: String
    let rating: ProductRating
    var quantity: Int = 1

    static let placeholder = Product(
        id: 0,
        title: "Loading...",
        description: "Loading description...",
        price: 0.0,
        image: "",
        category: "",
        rating: ProductRating(rate: 0.0, count: 0)
    )
}

struct ProductRating: Hashable {
    let rate: Double
    let count: Int
}

extension Product {
    var formattedPrice: String {
        String(format: "EGP %.2f", price)
    }

    var discountPercentage: Int {
        // Simulate discount based on rating (matching Android behavior)
        let discount = Int((5.0 - rating.rate) * 10)
        return max(0, min(discount, 50))
    }

    var isInStock: Bool {
        rating.count > 0
    }

    var stockCount: Int {
        rating.count
    }
}
