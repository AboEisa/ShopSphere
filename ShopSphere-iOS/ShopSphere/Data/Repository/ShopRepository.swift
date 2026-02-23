import Foundation
import FirebaseAuth
import FacebookLogin

final class ShopRepository: RepositoryProtocol {
    private let remoteDataSource: FirestoreRemoteDataSource
    private let localStorage: LocalStorageService
    private let firebaseAuth: Auth

    init(
        remoteDataSource: FirestoreRemoteDataSource,
        localStorage: LocalStorageService,
        firebaseAuth: Auth
    ) {
        self.remoteDataSource = remoteDataSource
        self.localStorage = localStorage
        self.firebaseAuth = firebaseAuth
    }

    // MARK: - Products

    func getProducts() async throws -> [Product] {
        try await remoteDataSource.getProducts()
    }

    func getProductsByCategory(_ category: String) async throws -> [Product] {
        try await remoteDataSource.getProductsByCategory(category)
    }

    // MARK: - Favorites

    func getFavoriteProducts() async throws -> [Product] {
        let favoriteIds = localStorage.getFavoriteIds()
        guard !favoriteIds.isEmpty else { return [] }
        let allProducts = try await remoteDataSource.getProducts()
        return allProducts.filter { favoriteIds.contains($0.id) }
    }

    func toggleFavorite(_ productId: Int) {
        localStorage.toggleFavorite(productId)
    }

    func isFavorite(_ productId: Int) -> Bool {
        localStorage.isFavorite(productId)
    }

    func getFavoriteIds() -> [Int] {
        localStorage.getFavoriteIds()
    }

    // MARK: - Cart

    func getCartProducts() async throws -> [Product] {
        let cartMap = localStorage.getCartProducts()
        guard !cartMap.isEmpty else { return [] }
        let allProducts = try await remoteDataSource.getProducts()
        return allProducts.filter { cartMap.keys.contains($0.id) }
    }

    func getCartItemCount() -> Int {
        localStorage.getCartItemCount()
    }

    func getCartMap() -> [Int: Int] {
        localStorage.getCartProducts()
    }

    // MARK: - Auth

    func login(email: String, password: String) async throws {
        try await firebaseAuth.signIn(withEmail: email, password: password)
    }

    func loginWithGoogle(idToken: String) async throws {
        let credential = GoogleAuthProvider.credential(
            withIDToken: idToken,
            accessToken: ""
        )
        try await firebaseAuth.signIn(with: credential)
    }

    func loginWithFacebook(accessToken: String) async throws {
        let credential = FacebookAuthProvider.credential(
            withAccessToken: accessToken
        )
        try await firebaseAuth.signIn(with: credential)
    }

    func register(name: String, email: String, password: String) async throws -> String {
        let result = try await firebaseAuth.createUser(withEmail: email, password: password)
        let changeRequest = result.user.createProfileChangeRequest()
        changeRequest.displayName = name
        try await changeRequest.commitChanges()
        return result.user.uid
    }

    func logout() {
        try? firebaseAuth.signOut()
        localStorage.clear()
    }

    func currentUserId() -> String? {
        firebaseAuth.currentUser?.uid
    }
}
