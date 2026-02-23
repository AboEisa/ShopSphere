import Foundation

struct GetProductsUseCase {
    private let repository: RepositoryProtocol

    init(repository: RepositoryProtocol) {
        self.repository = repository
    }

    func execute() async throws -> [Product] {
        try await repository.getProducts()
    }
}
