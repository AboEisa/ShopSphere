import Foundation

struct ProductDTO: Codable {
    let id: Int
    let title: String
    let description: String
    let price: Double
    let image: String
    let category: String
    let rating: RatingDTO
}

struct RatingDTO: Codable {
    let rate: Double
    let count: Int
}

// MARK: - Mapping to Domain
extension ProductDTO {
    func toDomain() -> Product {
        Product(
            id: id,
            title: title,
            description: description,
            price: price,
            image: image,
            category: category,
            rating: ProductRating(rate: rating.rate, count: rating.count)
        )
    }
}

extension Array where Element == ProductDTO {
    func toDomain() -> [Product] {
        map { $0.toDomain() }
    }
}
