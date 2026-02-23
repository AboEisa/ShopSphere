import Foundation

enum APIConstants {
    static let fakeStoreBaseURL = "https://fakestoreapi.com/"
    static let dummyJSONBaseURL = "https://dummyjson.com/"
    static let productsCollection = "products"
    static let usersCollection = "users"
}

enum AppConstants {
    static let categories = ["All", "electronics", "jewelery", "men's clothing", "women's clothing"]
    static let searchDebounceMs: UInt64 = 300_000_000 // 300ms in nanoseconds
    static let splashDelaySeconds: Double = 1.5
    static let maxBadgeCount = 99
    static let shippingFee: Double = 80.0

    // Support Info
    static let supportEmail = "omarncc00@gmail.com"
    static let supportPhone = "01018613426"
    static let supportHours = "Daily from 9:00 AM to 11:00 PM"

    // FAQ Data
    static let faqs: [(question: String, answer: String)] = [
        (
            "How do I place an order?",
            "Browse products, add items to your cart, then proceed to checkout. Select your delivery address and payment method, then confirm your order."
        ),
        (
            "What payment methods are accepted?",
            "We accept Visa, Mastercard, and other major credit/debit cards through our secure Stripe payment gateway."
        ),
        (
            "How can I track my order?",
            "Go to Account > My Orders, then tap on any order to see real-time tracking with live courier location on the map."
        ),
        (
            "What is the return policy?",
            "You can return any item within 14 days of delivery. Items must be in original condition with tags attached. Contact our help center to initiate a return."
        )
    ]
}

enum StorageKeys {
    static let uid = "uid"
    static let profileName = "profile_name"
    static let profileEmail = "profile_email"
    static let profilePhone = "profile_phone"
    static let isLoggedIn = "is_logged_in"
    static let favoriteProducts = "favorite_products"
    static let cartProducts = "cart_products"
}
