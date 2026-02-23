import Foundation

enum AppRoute: Hashable {
    case productDetail(productId: Int)
    case reviews(productId: Int)
    case checkout
    case trackOrder(orderId: String)
    case addressBook
    case mapPicker
    case paymentMethods
    case addCard
    case orders
    case myDetails
    case faqs
    case helpCenter
}

enum Tab: Int, CaseIterable {
    case home = 0
    case search
    case saved
    case cart
    case account
}
