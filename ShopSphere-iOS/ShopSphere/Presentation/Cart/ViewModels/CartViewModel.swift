import SwiftUI

@Observable
final class CartViewModel {
    var cartItems: [CartItem] = []
    var isLoading = false
    var isEmpty: Bool { cartItems.isEmpty && !isLoading }
    var errorMessage: String?

    var totalPrice: Double {
        cartItems.reduce(0) { $0 + $1.totalPrice }
    }

    var formattedTotal: String {
        String(format: "EGP %.2f", totalPrice)
    }

    var cartItemCount: Int {
        cartItems.reduce(0) { $0 + $1.quantity }
    }

    private var repository: RepositoryProtocol?
    private var localStorage: LocalStorageService?

    func configure(repository: RepositoryProtocol, localStorage: LocalStorageService) {
        self.repository = repository
        self.localStorage = localStorage
    }

    func loadCart() async {
        guard let repository, let localStorage else { return }
        isLoading = true
        do {
            let cartMap = localStorage.getCartProducts()
            let products = try await repository.getCartProducts()
            cartItems = products.compactMap { product in
                guard let quantity = cartMap[product.id] else { return nil }
                return CartItem(product: product, quantity: quantity)
            }
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func addProduct(_ productId: Int, stock: Int) -> Bool {
        guard let localStorage else { return false }
        let currentQty = localStorage.getCartProducts()[productId] ?? 0
        if currentQty >= stock {
            errorMessage = "Stock limit reached"
            return false
        }
        localStorage.addCartProduct(productId)
        return true
    }

    func removeProduct(_ productId: Int) {
        localStorage?.removeCartProduct(productId)
        cartItems.removeAll { $0.product.id == productId }
    }

    func updateQuantity(_ productId: Int, newQuantity: Int) {
        guard let localStorage else { return }
        if newQuantity <= 0 {
            removeProduct(productId)
            return
        }
        // Check stock
        if let item = cartItems.first(where: { $0.product.id == productId }),
           newQuantity > item.product.stockCount {
            errorMessage = "\(item.product.title) has only \(item.product.stockCount) in stock"
            return
        }
        localStorage.updateCartQuantity(productId, newQuantity)
        if let idx = cartItems.firstIndex(where: { $0.product.id == productId }) {
            cartItems[idx].quantity = newQuantity
        }
    }

    func isInCart(_ productId: Int) -> Bool {
        localStorage?.isInCart(productId) ?? false
    }

    func clearCart() {
        localStorage?.clearCart()
        cartItems = []
    }
}
