import Foundation

final class DummyJSONAPIService {
    private let networkService: NetworkService
    private let baseURL = APIConstants.dummyJSONBaseURL

    init(networkService: NetworkService) {
        self.networkService = networkService
    }

    func getProducts(limit: Int = 100) async throws -> DummyProductsResponse {
        try await networkService.request(url: "\(baseURL)products?limit=\(limit)")
    }
}
