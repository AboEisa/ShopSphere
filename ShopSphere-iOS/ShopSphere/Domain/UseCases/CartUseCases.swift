import Foundation

struct GetCartProductsUseCase {
    private let repository: RepositoryProtocol

    init(repository: RepositoryProtocol) {
        self.repository = repository
    }

    func execute() async throws -> [Product] {
        try await repository.getCartProducts()
    }
}

struct GetCartItemCountUseCase {
    private let repository: RepositoryProtocol

    init(repository: RepositoryProtocol) {
        self.repository = repository
    }

    func execute() -> Int {
        repository.getCartItemCount()
    }
}
