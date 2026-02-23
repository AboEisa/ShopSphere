import Foundation
import Combine

@Observable
final class LocalStorageService {
    private let defaults = UserDefaults.standard
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    let changes = PassthroughSubject<Void, Never>()

    // MARK: - Authentication / UID

    var uid: String {
        get { defaults.string(forKey: StorageKeys.uid) ?? "" }
        set {
            defaults.set(newValue, forKey: StorageKeys.uid)
            changes.send()
        }
    }

    var isLoggedIn: Bool {
        get { defaults.bool(forKey: StorageKeys.isLoggedIn) }
        set {
            defaults.set(newValue, forKey: StorageKeys.isLoggedIn)
            changes.send()
        }
    }

    // MARK: - Profile

    func saveProfile(name: String, email: String, phone: String) {
        defaults.set(name, forKey: StorageKeys.profileName)
        defaults.set(email, forKey: StorageKeys.profileEmail)
        defaults.set(phone, forKey: StorageKeys.profilePhone)
        changes.send()
    }

    var profileName: String {
        get { defaults.string(forKey: StorageKeys.profileName) ?? "" }
        set { defaults.set(newValue, forKey: StorageKeys.profileName) }
    }

    var profileEmail: String {
        get { defaults.string(forKey: StorageKeys.profileEmail) ?? "" }
        set { defaults.set(newValue, forKey: StorageKeys.profileEmail) }
    }

    var profilePhone: String {
        get { defaults.string(forKey: StorageKeys.profilePhone) ?? "" }
        set { defaults.set(newValue, forKey: StorageKeys.profilePhone) }
    }

    // MARK: - Favorites

    func getFavoriteIds() -> [Int] {
        guard let data = defaults.data(forKey: StorageKeys.favoriteProducts),
              let ids = try? decoder.decode([Int].self, from: data) else {
            return []
        }
        return ids
    }

    func saveFavoriteIds(_ ids: [Int]) {
        if let data = try? encoder.encode(ids) {
            defaults.set(data, forKey: StorageKeys.favoriteProducts)
            changes.send()
        }
    }

    func addFavorite(_ productId: Int) {
        var ids = getFavoriteIds()
        guard !ids.contains(productId) else { return }
        ids.append(productId)
        saveFavoriteIds(ids)
    }

    func removeFavorite(_ productId: Int) {
        var ids = getFavoriteIds()
        ids.removeAll { $0 == productId }
        saveFavoriteIds(ids)
    }

    func isFavorite(_ productId: Int) -> Bool {
        getFavoriteIds().contains(productId)
    }

    func toggleFavorite(_ productId: Int) {
        if isFavorite(productId) {
            removeFavorite(productId)
        } else {
            addFavorite(productId)
        }
    }

    // MARK: - Cart (Map: productId -> quantity)

    func getCartProducts() -> [Int: Int] {
        guard let data = defaults.data(forKey: StorageKeys.cartProducts),
              let dict = try? decoder.decode([String: Int].self, from: data) else {
            return [:]
        }
        return dict.reduce(into: [Int: Int]()) { result, pair in
            if let key = Int(pair.key) {
                result[key] = pair.value
            }
        }
    }

    private func saveCartProducts(_ products: [Int: Int]) {
        let stringDict = products.reduce(into: [String: Int]()) { result, pair in
            result[String(pair.key)] = pair.value
        }
        if let data = try? encoder.encode(stringDict) {
            defaults.set(data, forKey: StorageKeys.cartProducts)
            changes.send()
        }
    }

    func addCartProduct(_ productId: Int) {
        var products = getCartProducts()
        products[productId] = (products[productId] ?? 0) + 1
        saveCartProducts(products)
    }

    func removeCartProduct(_ productId: Int) {
        var products = getCartProducts()
        products.removeValue(forKey: productId)
        saveCartProducts(products)
    }

    func updateCartQuantity(_ productId: Int, _ quantity: Int) {
        var products = getCartProducts()
        if quantity <= 0 {
            products.removeValue(forKey: productId)
        } else {
            products[productId] = quantity
        }
        saveCartProducts(products)
    }

    func isInCart(_ productId: Int) -> Bool {
        getCartProducts().keys.contains(productId)
    }

    func getCartItemCount() -> Int {
        getCartProducts().values.reduce(0, +)
    }

    func clearCart() {
        defaults.removeObject(forKey: StorageKeys.cartProducts)
        changes.send()
    }

    // MARK: - Clear All

    func clear() {
        defaults.removeObject(forKey: StorageKeys.uid)
        defaults.removeObject(forKey: StorageKeys.profileName)
        defaults.removeObject(forKey: StorageKeys.profileEmail)
        defaults.removeObject(forKey: StorageKeys.profilePhone)
        defaults.removeObject(forKey: StorageKeys.isLoggedIn)
        defaults.removeObject(forKey: StorageKeys.favoriteProducts)
        defaults.removeObject(forKey: StorageKeys.cartProducts)
        changes.send()
    }
}
