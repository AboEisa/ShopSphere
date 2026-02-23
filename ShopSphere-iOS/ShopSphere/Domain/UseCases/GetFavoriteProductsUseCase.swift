import Foundation

struct GetFavoriteProductsUseCase {
    private let repository: RepositoryProtocol

    init(repository: RepositoryProtocol) {
        self.repository = repository
    }

    func execute() async throws -> [Product] {
        try await repository.getFavoriteProducts()
    }
}
