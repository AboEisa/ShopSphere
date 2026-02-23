import SwiftUI

struct SearchView: View {
    @State private var viewModel = SearchViewModel()
    @Environment(\.container) private var container
    @Environment(NavigationRouter.self) private var router

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack(spacing: 12) {
                CircleBackButton { router.pop() }

                Text("Search")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(AppTheme.textPrimary)

                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            // Search Input
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(AppTheme.textSecondary)
                TextField("Search for products...", text: Binding(
                    get: { viewModel.query },
                    set: { viewModel.onQueryChanged($0) }
                ))
                .autocapitalization(.none)

                if !viewModel.query.isEmpty {
                    Button {
                        viewModel.onQueryChanged("")
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(AppTheme.textSecondary)
                    }
                }
            }
            .padding(12)
            .background(AppTheme.lightGray)
            .cornerRadius(AppTheme.cornerRadius)
            .padding(.horizontal, 16)

            // Results
            if viewModel.query.isEmpty {
                Spacer()
            } else if viewModel.isSearching {
                Spacer()
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: AppTheme.primaryGreen))
                Spacer()
            } else if viewModel.results.isEmpty {
                emptyState
            } else {
                List(viewModel.results) { product in
                    SearchResultRow(product: product)
                        .listRowSeparator(.hidden)
                        .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                        .onTapGesture {
                            router.push(.productDetail(productId: product.id))
                        }
                }
                .listStyle(.plain)
            }
        }
        .background(AppTheme.background)
        .navigationBarHidden(true)
        .task {
            viewModel.configure(repository: container.repository)
            await viewModel.loadProducts()
        }
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Spacer()
            Image(systemName: "magnifyingglass")
                .font(.system(size: 48))
                .foregroundColor(AppTheme.textSecondary.opacity(0.5))
            Text("No Results Found")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(AppTheme.textPrimary)
            Text("Try searching with different keywords")
                .font(.system(size: 14))
                .foregroundColor(AppTheme.textSecondary)
            Spacer()
        }
    }
}
