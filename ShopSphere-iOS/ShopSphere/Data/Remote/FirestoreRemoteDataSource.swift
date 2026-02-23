import Foundation
import FirebaseFirestore

final class FirestoreRemoteDataSource {
    private let dummyAPIService: DummyJSONAPIService
    private let firestore = Firestore.firestore()
    private let productsCollection = APIConstants.productsCollection

    init(dummyAPIService: DummyJSONAPIService) {
        self.dummyAPIService = dummyAPIService
    }

    // MARK: - Get Products (with Firestore seeding)

    func getProducts() async throws -> [Product] {
        let collection = firestore.collection(productsCollection)

        // Try server first, fallback to cache
        let snapshot: QuerySnapshot
        do {
            snapshot = try await collection.getDocuments(source: .server)
        } catch {
            snapshot = try await collection.getDocuments()
        }

        if snapshot.isEmpty {
            // Seed from DummyJSON API
            let response = try await dummyAPIService.getProducts()
            let batch = firestore.batch()

            for product in response.products {
                let docRef = collection.document(String(product.id))
                batch.setData(product.toFirestoreMap(mappedCategory: mapCategory(product.category)), forDocument: docRef)
            }

            try await batch.commit()
            return response.products.map { item in
                item.toProduct(mappedCategory: mapCategory(item.category))
            }
        } else {
            return snapshot.documents
                .compactMap { documentToProduct($0) }
                .sorted { $0.id < $1.id }
        }
    }

    func getProductsByCategory(_ category: String) async throws -> [Product] {
        let allProducts = try await getProducts()
        return allProducts.filter {
            normalizeCategory($0.category) == normalizeCategory(category)
        }
    }

    // MARK: - Document Parsing (matches Android logic exactly)

    private func documentToProduct(_ document: DocumentSnapshot) -> Product? {
        guard let data = document.data() else { return nil }

        let isActive = data["isActive"] as? Bool ?? true
        guard isActive else { return nil }

        guard let title = data["title"] as? String ?? data["name"] as? String else { return nil }

        let description = data["description"] as? String ?? ""
        let rawCategory = data["category"] as? String ?? "General"
        let category = mapCategory(rawCategory.isEmpty ? "General" : rawCategory)

        let price: Double
        if let p = data["price"] as? Double {
            price = p
        } else if let p = data["price"] as? Int {
            price = Double(p)
        } else {
            price = 0.0
        }

        let image: String
        if let img = data["image"] as? String, !img.isEmpty {
            image = img
        } else if let thumb = data["thumbnail"] as? String, !thumb.isEmpty {
            image = thumb
        } else if let images = data["images"] as? [String], let first = images.first {
            image = first
        } else {
            image = ""
        }

        let ratingRate: Double
        if let r = data["ratingRate"] as? Double {
            ratingRate = r
        } else if let r = data["rating"] as? Double {
            ratingRate = r
        } else {
            ratingRate = 0.0
        }

        let stockCount: Int
        if let s = data["stock"] as? Int {
            stockCount = s
        } else if let s = data["ratingCount"] as? Int {
            stockCount = s
        } else if let s = data["inventory"] as? Int {
            stockCount = s
        } else {
            stockCount = 0
        }

        let productId: Int
        if let id = data["id"] as? Int {
            productId = id
        } else if let id = data["id"] as? Int64 {
            productId = Int(id)
        } else {
            productId = abs(document.documentID.hashValue)
        }

        return Product(
            id: productId,
            title: title,
            description: description,
            price: price,
            image: image,
            category: category,
            rating: ProductRating(
                rate: ratingRate,
                count: max(stockCount, 0)
            )
        )
    }

    // MARK: - Category Mapping (matches Android mapCategory)

    private func mapCategory(_ rawCategory: String) -> String {
        let cleaned = rawCategory.trimmingCharacters(in: .whitespaces)
        guard !cleaned.isEmpty else { return "General" }

        return cleaned
            .replacingOccurrences(of: "-", with: " ")
            .replacingOccurrences(of: "_", with: " ")
            .split(separator: " ")
            .filter { !$0.isEmpty }
            .map { part in
                part.lowercased().prefix(1).uppercased() + part.lowercased().dropFirst()
            }
            .joined(separator: " ")
    }

    private func normalizeCategory(_ category: String) -> String {
        category.trimmingCharacters(in: .whitespaces).lowercased()
    }
}

// MARK: - DummyProductItem Conversion Helpers

private extension DummyProductItem {
    func toProduct(mappedCategory: String) -> Product {
        Product(
            id: id,
            title: title,
            description: description,
            price: price,
            image: thumbnail ?? images?.first ?? "",
            category: mappedCategory,
            rating: ProductRating(
                rate: rating ?? 0.0,
                count: stock ?? 0
            )
        )
    }

    func toFirestoreMap(mappedCategory: String) -> [String: Any] {
        [
            "id": id,
            "title": title,
            "description": description,
            "category": mappedCategory,
            "price": price,
            "thumbnail": thumbnail ?? images?.first ?? "",
            "image": thumbnail ?? images?.first ?? "",
            "images": images ?? [],
            "ratingRate": rating ?? 0.0,
            "ratingCount": stock ?? 0,
            "stock": stock ?? 0,
            "isActive": true,
            "createdAt": FieldValue.serverTimestamp(),
            "updatedAt": FieldValue.serverTimestamp(),
            "seededFromDummyJsonAt": Timestamp()
        ]
    }
}
