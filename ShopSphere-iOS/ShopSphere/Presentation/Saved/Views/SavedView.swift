import SwiftUI

struct SavedView: View {
    @State private var viewModel = SavedViewModel()
    @Environment(\.container) private var container
    @Environment(NavigationRouter.self) private var router

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Text("Saved Items")
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
                ScrollView {
                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(viewModel.favoriteProducts) { product in
                            SavedProductCard(
                                product: product,
                                onRemove: { viewModel.toggleFavorite(product.id) }
                            )
                            .onTapGesture {
                                router.push(.productDetail(productId: product.id))
                            }
                        }
                    }
                    .padding(16)
                }
            }
        }
        .background(AppTheme.background)
        .navigationBarHidden(true)
        .task {
            viewModel.configure(repository: container.repository, localStorage: container.localStorage)
            await viewModel.loadFavorites()
        }
        .onReceive(container.localStorage.changes) { _ in
            Task { await viewModel.loadFavorites() }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Spacer()
            Image(systemName: "heart")
                .font(.system(size: 56))
                .foregroundColor(AppTheme.primaryGreen.opacity(0.5))
            Text("No Saved Items")
                .font(.system(size: 20, weight: .semibold))
                .foregroundColor(AppTheme.textPrimary)
            Text("You don't have any saved items")
                .font(.system(size: 14))
                .foregroundColor(AppTheme.textSecondary)
            Text("Go to home and add some")
                .font(.system(size: 14))
                .foregroundColor(AppTheme.textSecondary)
            Spacer()
        }
    }
}
