import SwiftUI

struct ProductDetailView: View {
    let productId: Int
    @State private var viewModel = DetailViewModel()
    @State private var selectedSize = "M"
    @State private var showAddedToast = false
    @Environment(\.container) private var container
    @Environment(NavigationRouter.self) private var router

    private let sizes = ["S", "M", "L", "XL"]

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                CircleBackButton { router.pop() }
                Spacer()
                Text("Details")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(AppTheme.textPrimary)
                Spacer()
                Color.clear.frame(width: 40, height: 40) // Balance
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            if viewModel.isLoading {
                Spacer()
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: AppTheme.primaryGreen))
                Spacer()
            } else if let product = viewModel.product {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        // Product Image
                        ZStack(alignment: .topTrailing) {
                            AsyncImage(url: URL(string: product.image)) { phase in
                                switch phase {
                                case .success(let image):
                                    image.resizable().scaledToFit()
                                case .failure, .empty:
                                    Image(systemName: "photo")
                                        .font(.system(size: 60))
                                        .foregroundColor(AppTheme.divider)
                                @unknown default:
                                    Color.clear
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 250)
                            .background(AppTheme.surface)
                            .cornerRadius(AppTheme.cardCornerRadius)

                            // Favorite Button
                            Button {
                                container.localStorage.toggleFavorite(product.id)
                            } label: {
                                Image(systemName: container.localStorage.isFavorite(product.id) ? "heart.fill" : "heart")
                                    .font(.system(size: 18))
                                    .foregroundColor(container.localStorage.isFavorite(product.id) ? .red : AppTheme.textSecondary)
                                    .frame(width: 40, height: 40)
                                    .background(Color.white)
                                    .clipShape(Circle())
                                    .shadow(color: .black.opacity(0.1), radius: 3)
                            }
                            .padding(12)
                        }

                        // Title
                        Text(product.title)
                            .font(.system(size: 22, weight: .bold))
                            .foregroundColor(AppTheme.textPrimary)

                        // Rating Row
                        HStack(spacing: 8) {
                            HStack(spacing: 4) {
                                Image(systemName: "star.fill")
                                    .font(.system(size: 14))
                                    .foregroundColor(AppTheme.reviewStarActive)
                                Text(String(format: "%.1f/5", product.rating.rate))
                                    .font(.system(size: 14, weight: .medium))
                                    .foregroundColor(AppTheme.textPrimary)
                            }

                            Text("Stock: \(product.stockCount)")
                                .font(.system(size: 14))
                                .foregroundColor(product.isInStock ? AppTheme.textSecondary : AppTheme.errorRed)
                        }

                        // View Reviews
                        Button {
                            router.push(.reviews(productId: product.id))
                        } label: {
                            Text("View Reviews (\(product.rating.count))")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(AppTheme.primaryGreen)
                        }

                        // Description
                        Text(product.description)
                            .font(.system(size: 14))
                            .foregroundColor(AppTheme.textSecondary)
                            .lineSpacing(4)

                        // Size Selector
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Size")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(AppTheme.textPrimary)

                            SizeSelectorView(sizes: sizes, selected: $selectedSize)
                        }

                        // Price + Add to Cart
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Price")
                                    .font(.system(size: 12))
                                    .foregroundColor(AppTheme.textSecondary)
                                Text(product.formattedPrice)
                                    .font(.system(size: 22, weight: .bold))
                                    .foregroundColor(AppTheme.primaryGreen)
                            }

                            Spacer()

                            Button {
                                handleCartAction(product: product)
                            } label: {
                                Text(container.localStorage.isInCart(product.id) ? "Remove from Cart" : "Add to Cart")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 24)
                                    .padding(.vertical, 14)
                                    .background(
                                        container.localStorage.isInCart(product.id) ?
                                        AppTheme.errorRed : AppTheme.primaryGreen
                                    )
                                    .cornerRadius(AppTheme.buttonCornerRadius)
                            }
                            .disabled(!product.isInStock && !container.localStorage.isInCart(product.id))
                        }
                    }
                    .padding(16)
                }
            }
        }
        .background(AppTheme.background)
        .navigationBarHidden(true)
        .task {
            viewModel.configure(repository: container.repository)
            await viewModel.fetchProduct(id: productId)
        }
        .overlay(alignment: .top) {
            if showAddedToast {
                Text("Added to cart")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(.white)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .background(AppTheme.primaryGreen)
                    .cornerRadius(20)
                    .transition(.move(edge: .top).combined(with: .opacity))
                    .padding(.top, 60)
            }
        }
    }

    private func handleCartAction(product: Product) {
        if container.localStorage.isInCart(product.id) {
            container.localStorage.removeCartProduct(product.id)
        } else {
            container.localStorage.addCartProduct(product.id)
            withAnimation { showAddedToast = true }
            Task {
                try? await Task.sleep(for: .seconds(2))
                withAnimation { showAddedToast = false }
            }
        }
    }
}
