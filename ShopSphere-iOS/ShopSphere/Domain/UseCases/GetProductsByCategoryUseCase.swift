import Foundation

struct GetProductsByCategoryUseCase {
    private let repository: RepositoryProtocol

    init(repository: RepositoryProtocol) {
        self.repository = repository
    }

    func execute(category: String) async throws -> [Product] {
        try await repository.getProductsByCategory(category)
    }
}
