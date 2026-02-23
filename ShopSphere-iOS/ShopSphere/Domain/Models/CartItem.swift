import Foundation

struct CartItem: Identifiable, Hashable {
    var id: Int { product.id }
    let product: Product
    var quantity: Int

    var totalPrice: Double {
        product.price * Double(quantity)
    }

    var formattedTotalPrice: String {
        String(format: "EGP %.2f", totalPrice)
    }
}
