import SwiftUI

struct PaymentMethodsView: View {
    @Bindable var checkoutViewModel: CheckoutViewModel
    @Environment(NavigationRouter.self) private var router

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                CircleBackButton { router.pop() }
                Spacer()
                Text("Payment Methods")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(AppTheme.textPrimary)
                Spacer()
                Color.clear.frame(width: 40, height: 40)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            if checkoutViewModel.paymentMethods.isEmpty {
                VStack(spacing: 12) {
                    Spacer()
                    Image(systemName: "creditcard")
                        .font(.system(size: 48))
                        .foregroundColor(AppTheme.textSecondary.opacity(0.5))
                    Text("No Payment Methods")
                        .font(.system(size: 18, weight: .semibold))
                    Text("Add a card to get started")
                        .font(.system(size: 14))
                        .foregroundColor(AppTheme.textSecondary)
                    Spacer()
                }
            } else {
                List(checkoutViewModel.paymentMethods) { method in
                    PaymentMethodRow(
                        method: method,
                        isSelected: method.isSelected
                    )
                    .onTapGesture {
                        checkoutViewModel.selectPaymentMethod(method.id)
                    }
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                }
                .listStyle(.plain)
            }

            VStack(spacing: 8) {
                Button {
                    router.push(.addCard)
                } label: {
                    HStack {
                        Image(systemName: "plus")
                        Text("Add New Card")
                    }
                    .outlinedButton()
                }

                if checkoutViewModel.selectedPaymentMethod != nil {
                    Button {
                        router.pop()
                    } label: {
                        Text("Use This Card")
                            .primaryButton()
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 12)
        }
        .background(AppTheme.background)
        .navigationBarHidden(true)
    }
}
