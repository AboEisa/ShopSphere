import Foundation

struct ToggleFavoriteUseCase {
    private let repository: RepositoryProtocol

    init(repository: RepositoryProtocol) {
        self.repository = repository
    }

    func execute(productId: Int) {
        repository.toggleFavorite(productId)
    }

    func isFavorite(productId: Int) -> Bool {
        repository.isFavorite(productId)
    }
}
