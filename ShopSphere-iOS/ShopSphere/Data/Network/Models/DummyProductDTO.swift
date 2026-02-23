import Foundation

struct DummyProductsResponse: Codable {
    let products: [DummyProductItem]
}

struct DummyProductItem: Codable {
    let id: Int
    let title: String
    let description: String
    let category: String
    let price: Double
    let thumbnail: String?
    let images: [String]?
    let rating: Double?
    let stock: Int?
}

// MARK: - Mapping to ProductDTO
extension DummyProductItem {
    func toProductDTO() -> ProductDTO {
        ProductDTO(
            id: id,
            title: title,
            description: description,
            price: price,
            image: thumbnail ?? images?.first ?? "",
            category: category,
            rating: RatingDTO(rate: rating ?? 0.0, count: stock ?? 0)
        )
    }

    func toFirestoreMap() -> [String: Any] {
        [
            "id": id,
            "title": title,
            "description": description,
            "price": price,
            "image": thumbnail ?? images?.first ?? "",
            "category": category,
            "rating": [
                "rate": rating ?? 0.0,
                "count": stock ?? 0
            ]
        ]
    }
}
