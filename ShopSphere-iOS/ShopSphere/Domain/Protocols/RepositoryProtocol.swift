import Foundation

protocol RepositoryProtocol {
    // Products
    func getProducts() async throws -> [Product]
    func getProductsByCategory(_ category: String) async throws -> [Product]

    // Favorites
    func getFavoriteProducts() async throws -> [Product]
    func toggleFavorite(_ productId: Int)
    func isFavorite(_ productId: Int) -> Bool
    func getFavoriteIds() -> [Int]

    // Cart
    func getCartProducts() async throws -> [Product]
    func getCartItemCount() -> Int
    func getCartMap() -> [Int: Int]

    // Auth
    func login(email: String, password: String) async throws
    func loginWithGoogle(idToken: String) async throws
    func loginWithFacebook(accessToken: String) async throws
    func register(name: String, email: String, password: String) async throws -> String
    func logout()
    func currentUserId() -> String?
}
