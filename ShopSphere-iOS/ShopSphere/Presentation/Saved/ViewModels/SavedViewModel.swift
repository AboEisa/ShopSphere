import SwiftUI

@Observable
final class SavedViewModel {
    var favoriteProducts: [Product] = []
    var isLoading = false
    var isEmpty: Bool { favoriteProducts.isEmpty && !isLoading }

    private var repository: RepositoryProtocol?
    private var localStorage: LocalStorageService?

    func configure(repository: RepositoryProtocol, localStorage: LocalStorageService) {
        self.repository = repository
        self.localStorage = localStorage
    }

    func loadFavorites() async {
        guard let repository else { return }
        isLoading = true
        do {
            favoriteProducts = try await repository.getFavoriteProducts()
        } catch {
            favoriteProducts = []
        }
        isLoading = false
    }

    func toggleFavorite(_ productId: Int) {
        localStorage?.toggleFavorite(productId)
        favoriteProducts.removeAll { $0.id == productId }
    }

    func isFavorite(_ productId: Int) -> Bool {
        localStorage?.isFavorite(productId) ?? false
    }
}
