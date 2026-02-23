import SwiftUI

@Observable
final class SearchViewModel {
    var query = ""
    var results: [Product] = []
    var isSearching = false

    private var allProducts: [Product] = []
    private var searchTask: Task<Void, Never>?
    private var repository: RepositoryProtocol?

    func configure(repository: RepositoryProtocol) {
        self.repository = repository
    }

    func loadProducts() async {
        guard let repository, allProducts.isEmpty else { return }
        do {
            allProducts = try await repository.getProducts()
        } catch {
            // Silent fallback — products will be empty
        }
    }

    func onQueryChanged(_ newQuery: String) {
        query = newQuery
        searchTask?.cancel()

        guard !newQuery.trimmingCharacters(in: .whitespaces).isEmpty else {
            results = []
            isSearching = false
            return
        }

        isSearching = true
        searchTask = Task {
            try? await Task.sleep(nanoseconds: AppConstants.searchDebounceMs)
            guard !Task.isCancelled else { return }

            let lowered = newQuery.lowercased()
            results = allProducts.filter { $0.title.lowercased().contains(lowered) }
            isSearching = false
        }
    }
}
