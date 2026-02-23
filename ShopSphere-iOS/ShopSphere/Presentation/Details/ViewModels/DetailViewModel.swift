import SwiftUI

@Observable
final class DetailViewModel {
    var product: Product?
    var isLoading = false
    var errorMessage: String?

    private var repository: RepositoryProtocol?

    func configure(repository: RepositoryProtocol) {
        self.repository = repository
    }

    func fetchProduct(id: Int) async {
        guard let repository else { return }
        isLoading = true
        do {
            let products = try await repository.getProducts()
            product = products.first { $0.id == id }
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}
