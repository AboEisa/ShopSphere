import Foundation

final class FakeStoreAPIService {
    private let networkService: NetworkService
    private let baseURL = APIConstants.fakeStoreBaseURL

    init(networkService: NetworkService) {
        self.networkService = networkService
    }

    func getProducts() async throws -> [ProductDTO] {
        try await networkService.request(url: "\(baseURL)products")
    }

    func getProductsByCategory(_ category: String) async throws -> [ProductDTO] {
        let encodedCategory = category.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? category
        return try await networkService.request(url: "\(baseURL)products/category/\(encodedCategory)")
    }
}
