import SwiftUI

struct CartView: View {
    @State private var viewModel = CartViewModel()
    @Environment(\.container) private var container
    @Environment(NavigationRouter.self) private var router

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Text("My Cart")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(AppTheme.textPrimary)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            if viewModel.isLoading {
                Spacer()
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: AppTheme.primaryGreen))
                Spacer()
            } else if viewModel.isEmpty {
                emptyState
            } else {
                // Cart Items
                List {
                    ForEach(viewModel.cartItems) { item in
                        CartItemRow(
                            item: item,
                            onQuantityChange: { qty in
                                viewModel.updateQuantity(item.product.id, newQuantity: qty)
                            },
                            onRemove: {
                                viewModel.removeProduct(item.product.id)
                            }
                        )
                        .listRowSeparator(.hidden)
                        .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                        .onTapGesture {
                            router.push(.productDetail(productId: item.product.id))
                        }
                    }
                }
                .listStyle(.plain)

                // Total + Checkout
                VStack(spacing: 12) {
                    Divider()

                    HStack {
                        Text("Total")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(AppTheme.textSecondary)
                        Spacer()
                        Text(viewModel.formattedTotal)
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(AppTheme.primaryGreen)
                    }
                    .padding(.horizontal, 16)

                    Button {
                        router.push(.checkout)
                    } label: {
                        Text("Checkout")
                            .primaryButton()
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 8)
                }
                .background(Color.white)
            }
        }
        .background(AppTheme.background)
        .navigationBarHidden(true)
        .task {
            viewModel.configure(repository: container.repository, localStorage: container.localStorage)
            await viewModel.loadCart()
        }
        .onReceive(container.localStorage.changes) { _ in
            Task { await viewModel.loadCart() }
        }
        .alert("Notice", isPresented: Binding(
            get: { viewModel.errorMessage != nil },
            set: { if !$0 { viewModel.errorMessage = nil } }
        )) {
            Button("OK") { viewModel.errorMessage = nil }
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Spacer()
            Image(systemName: "cart")
                .font(.system(size: 56))
                .foregroundColor(AppTheme.textSecondary.opacity(0.5))
            Text("Your Cart is Empty")
                .font(.system(size: 20, weight: .semibold))
                .foregroundColor(AppTheme.textPrimary)
            Text("When you add products, they'll appear here.")
                .font(.system(size: 14))
                .foregroundColor(AppTheme.textSecondary)
            Spacer()
        }
    }
}
