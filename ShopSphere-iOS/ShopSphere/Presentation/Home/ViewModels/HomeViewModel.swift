import SwiftUI

@Observable
final class HomeViewModel {
    var products: [Product] = []
    var categories: [String] = AppConstants.categories
    var selectedCategory = "All"
    var isLoading = false
    var hasLoadedOnce = false
    var errorMessage: String?
    var activePriceRange: ClosedRange<Double>?

    private var allProductsCache: [Product] = []
    private var repository: RepositoryProtocol?

    func configure(repository: RepositoryProtocol) {
        self.repository = repository
    }

    func fetchProducts() async {
        guard let repository else { return }
        isLoading = true
        errorMessage = nil
        do {
            let fetched = try await repository.getProducts()
            allProductsCache = fetched
            applyFilters(to: fetched)
            // Extract unique categories
            let uniqueCategories = Set(fetched.map { $0.category })
            categories = ["All"] + uniqueCategories.sorted()
            hasLoadedOnce = true
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func fetchProductsByCategory(_ category: String) async {
        selectedCategory = category
        if category == "All" {
            applyFilters(to: allProductsCache)
            return
        }
        guard let repository else { return }
        isLoading = true
        do {
            let fetched = try await repository.getProductsByCategory(category)
            applyFilters(to: fetched)
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func filterByPrice(min: Double, max: Double) {
        activePriceRange = min...max
        let baseProducts = selectedCategory == "All" ? allProductsCache :
            allProductsCache.filter { $0.category.lowercased() == selectedCategory.lowercased() }
        products = baseProducts.filter { $0.price >= min && $0.price <= max }
    }

    func clearPriceFilter() {
        activePriceRange = nil
        let baseProducts = selectedCategory == "All" ? allProductsCache :
            allProductsCache.filter { $0.category.lowercased() == selectedCategory.lowercased() }
        products = baseProducts
    }

    private func applyFilters(to fetchedProducts: [Product]) {
        if let range = activePriceRange {
            products = fetchedProducts.filter { $0.price >= range.lowerBound && $0.price <= range.upperBound }
        } else {
            products = fetchedProducts
        }
    }
}
