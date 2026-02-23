import SwiftUI

struct HomeView: View {
    @State private var viewModel = HomeViewModel()
    @State private var showFilter = false
    @Environment(\.container) private var container
    @Environment(NavigationRouter.self) private var router

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                // Header
                headerSection

                ScrollView {
                    VStack(spacing: 16) {
                        // Categories
                        categoriesSection

                        // Products Grid
                        if viewModel.isLoading && !viewModel.hasLoadedOnce {
                            shimmerGrid
                        } else if viewModel.products.isEmpty {
                            emptyState
                        } else {
                            productsGrid
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 80)
                }
            }

            // Draggable Cart FAB
            DraggableCartFAB {
                // Navigate to cart tab — handled by parent TabView
            }
        }
        .background(AppTheme.background)
        .navigationBarHidden(true)
        .task {
            if !viewModel.hasLoadedOnce {
                viewModel.configure(repository: container.repository)
                await viewModel.fetchProducts()
            }
        }
        .sheet(isPresented: $showFilter) {
            FilterSheetView(
                currentRange: viewModel.activePriceRange,
                onApply: { min, max in viewModel.filterByPrice(min: min, max: max) },
                onReset: { viewModel.clearPriceFilter() }
            )
            .presentationDetents([.medium])
        }
    }

    // MARK: - Header

    private var headerSection: some View {
        VStack(spacing: 12) {
            HStack {
                Text("Discover")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(AppTheme.textPrimary)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)

            HStack(spacing: 12) {
                // Search Bar (non-editable, navigates to Search)
                Button {
                    router.push(.productDetail(productId: -1)) // SearchView is in its own tab
                } label: {
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(AppTheme.textSecondary)
                        Text("Search for clothes...")
                            .foregroundColor(AppTheme.textSecondary)
                            .font(.system(size: 14))
                        Spacer()
                    }
                    .padding(12)
                    .background(AppTheme.lightGray)
                    .cornerRadius(AppTheme.cornerRadius)
                }

                // Filter Button
                Button { showFilter = true } label: {
                    Image(systemName: "slider.horizontal.3")
                        .font(.system(size: 18))
                        .foregroundColor(AppTheme.textPrimary)
                        .frame(width: 44, height: 44)
                        .background(AppTheme.lightGray)
                        .cornerRadius(AppTheme.cornerRadius)
                }
            }
            .padding(.horizontal, 16)
        }
    }

    // MARK: - Categories

    private var categoriesSection: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(viewModel.categories, id: \.self) { category in
                    CategoryChipView(
                        title: category,
                        isSelected: viewModel.selectedCategory == category
                    ) {
                        Task { await viewModel.fetchProductsByCategory(category) }
                    }
                }
            }
            .padding(.vertical, 4)
        }
    }

    // MARK: - Products Grid

    private var productsGrid: some View {
        LazyVGrid(columns: columns, spacing: 12) {
            ForEach(viewModel.products) { product in
                ProductCardView(
                    product: product,
                    isFavorite: container.localStorage.isFavorite(product.id),
                    onFavoriteTap: {
                        container.localStorage.toggleFavorite(product.id)
                    }
                )
                .onTapGesture {
                    router.push(.productDetail(productId: product.id))
                }
            }
        }
    }

    // MARK: - Shimmer Loading

    private var shimmerGrid: some View {
        LazyVGrid(columns: columns, spacing: 12) {
            ForEach(0..<6, id: \.self) { _ in
                ShimmerProductCard()
            }
        }
    }

    // MARK: - Empty State

    private var emptyState: some View {
        VStack(spacing: 12) {
            Spacer().frame(height: 80)
            Image(systemName: "magnifyingglass")
                .font(.system(size: 48))
                .foregroundColor(AppTheme.textSecondary.opacity(0.5))
            Text("No Result Found")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(AppTheme.textPrimary)
            Text("Try a different category or filter")
                .font(.system(size: 14))
                .foregroundColor(AppTheme.textSecondary)
        }
    }
}
