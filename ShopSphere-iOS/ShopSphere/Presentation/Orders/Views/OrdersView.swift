import SwiftUI

struct OrdersView: View {
    @Bindable var checkoutViewModel: CheckoutViewModel
    @Environment(NavigationRouter.self) private var router

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                CircleBackButton { router.pop() }
                Spacer()
                Text("My Orders")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(AppTheme.textPrimary)
                Spacer()
                Color.clear.frame(width: 40, height: 40)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            if checkoutViewModel.orderHistory.isEmpty {
                VStack(spacing: 12) {
                    Spacer()
                    Image(systemName: "shippingbox")
                        .font(.system(size: 48))
                        .foregroundColor(AppTheme.textSecondary.opacity(0.5))
                    Text("No Orders Yet")
                        .font(.system(size: 18, weight: .semibold))
                    Text("Your order history will appear here")
                        .font(.system(size: 14))
                        .foregroundColor(AppTheme.textSecondary)
                    Spacer()
                }
            } else {
                List(checkoutViewModel.orderHistory) { order in
                    OrderRow(order: order)
                        .onTapGesture {
                            router.push(.trackOrder(orderId: order.orderId))
                        }
                        .listRowSeparator(.hidden)
                        .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                }
                .listStyle(.plain)
            }
        }
        .background(AppTheme.background)
        .navigationBarHidden(true)
    }
}
