import SwiftUI
import FirebaseAuth

struct CheckoutView: View {
    @State private var cartViewModel = CartViewModel()
    @Bindable var checkoutViewModel: CheckoutViewModel
    @State private var confirmDialog: ConfirmationDialogData?
    @State private var successDialog: SuccessDialogData?
    @Environment(\.container) private var container
    @Environment(NavigationRouter.self) private var router

    private var subtotal: Double { cartViewModel.totalPrice }
    private var total: Double { subtotal + AppConstants.shippingFee }

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                CircleBackButton { router.pop() }
                Spacer()
                Text("Checkout")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(AppTheme.textPrimary)
                Spacer()
                Color.clear.frame(width: 40, height: 40)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            ScrollView {
                VStack(spacing: 16) {
                    // Delivery Address
                    addressCard

                    // Payment Method
                    paymentCard

                    // Order Summary
                    summaryCard
                }
                .padding(16)
            }

            // Place Order Button
            Button {
                showConfirmation()
            } label: {
                Text("Place Order")
                    .primaryButton()
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 12)
        }
        .background(AppTheme.surface)
        .navigationBarHidden(true)
        .task {
            cartViewModel.configure(repository: container.repository, localStorage: container.localStorage)
            await cartViewModel.loadCart()
        }
        .dialogs(confirm: $confirmDialog, success: $successDialog)
    }

    // MARK: - Address Card

    private var addressCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Delivery Address")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(AppTheme.textPrimary)
                Spacer()
                Button("Change") {
                    router.push(.addressBook)
                }
                .font(.system(size: 14))
                .foregroundColor(AppTheme.primaryGreen)
            }

            if let address = checkoutViewModel.selectedAddress {
                VStack(alignment: .leading, spacing: 4) {
                    Text(address.title)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(AppTheme.textPrimary)
                    Text(address.address)
                        .font(.system(size: 13))
                        .foregroundColor(AppTheme.textSecondary)
                }
            } else {
                Text("No address selected")
                    .font(.system(size: 14))
                    .foregroundColor(AppTheme.textSecondary)
            }
        }
        .cardStyle()
    }

    // MARK: - Payment Card

    private var paymentCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Payment Method")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(AppTheme.textPrimary)
                Spacer()
                Button("Edit") {
                    router.push(.paymentMethods)
                }
                .font(.system(size: 14))
                .foregroundColor(AppTheme.primaryGreen)
            }

            if let card = checkoutViewModel.selectedPaymentMethod {
                HStack(spacing: 8) {
                    Image(systemName: card.brandIcon)
                        .font(.system(size: 20))
                        .foregroundColor(AppTheme.textPrimary)
                    Text(card.maskedNumber)
                        .font(.system(size: 14))
                        .foregroundColor(AppTheme.textPrimary)
                }
            } else {
                Text("No payment method selected")
                    .font(.system(size: 14))
                    .foregroundColor(AppTheme.textSecondary)
            }
        }
        .cardStyle()
    }

    // MARK: - Summary Card

    private var summaryCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Order Summary")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(AppTheme.textPrimary)

            HStack {
                Text("Sub-Total")
                    .font(.system(size: 14))
                    .foregroundColor(AppTheme.textSecondary)
                Spacer()
                Text(String(format: "EGP %.2f", subtotal))
                    .font(.system(size: 14, weight: .medium))
            }

            HStack {
                Text("Shipping Fee")
                    .font(.system(size: 14))
                    .foregroundColor(AppTheme.textSecondary)
                Spacer()
                Text(String(format: "EGP %.2f", AppConstants.shippingFee))
                    .font(.system(size: 14, weight: .medium))
            }

            Divider()

            HStack {
                Text("Total")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(AppTheme.textPrimary)
                Spacer()
                Text(String(format: "EGP %.2f", total))
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(AppTheme.primaryGreen)
            }
        }
        .cardStyle()
    }

    // MARK: - Actions

    private func showConfirmation() {
        confirmDialog = ConfirmationDialogData(
            title: "Confirm Order",
            message: "Are you sure you want to place this order for \(String(format: "EGP %.2f", total))?",
            confirmTitle: "Place Order",
            onConfirm: { placeOrder() }
        )
    }

    private func placeOrder() {
        // Use profile name, fallback to Firebase displayName, then email prefix
        var customerName = container.localStorage.profileName
        if customerName.trimmingCharacters(in: .whitespaces).isEmpty {
            customerName = Auth.auth().currentUser?.displayName ?? ""
        }
        if customerName.trimmingCharacters(in: .whitespaces).isEmpty {
            customerName = Auth.auth().currentUser?.email?.components(separatedBy: "@").first ?? "Customer"
        }

        // Use profile phone, fallback to Firebase phone, then default
        var phone = container.localStorage.profilePhone
        if phone.filter(\.isNumber).count < 8 {
            phone = Auth.auth().currentUser?.phoneNumber ?? "01000000000"
        }

        let result = checkoutViewModel.placeOrder(
            total: String(format: "EGP %.2f", total),
            customerName: customerName,
            phone: phone,
            cartItems: cartViewModel.cartItems
        )

        switch result {
        case .success(let order):
            cartViewModel.clearCart()
            successDialog = SuccessDialogData(
                title: "Order Placed!",
                message: "Your order #\(order.orderId) has been placed successfully.",
                primaryAction: ("Track Order", {
                    router.popToRoot()
                    router.push(.trackOrder(orderId: order.orderId))
                }),
                secondaryAction: ("Continue Shopping", {
                    router.popToRoot()
                })
            )
        case .failure(let error):
            confirmDialog = ConfirmationDialogData(
                title: "Order Failed",
                message: error.localizedDescription,
                confirmTitle: "OK",
                onConfirm: {}
            )
        }
    }
}
