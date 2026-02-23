import SwiftUI

struct ReviewsView: View {
    let productId: Int
    @State private var viewModel = DetailViewModel()
    @Environment(\.container) private var container
    @Environment(NavigationRouter.self) private var router

    // Mock reviews (matching Android behavior)
    private let reviews = [
        ReviewData(author: "Ahmed M.", rating: 4.5, comment: "Great quality product! Exactly as described. Fast shipping too.", date: "2 days ago"),
        ReviewData(author: "Sara K.", rating: 5.0, comment: "Absolutely love it! The material is premium and fits perfectly.", date: "1 week ago"),
        ReviewData(author: "Mohamed A.", rating: 3.5, comment: "Good product but the color is slightly different from the picture.", date: "2 weeks ago"),
        ReviewData(author: "Nour H.", rating: 4.0, comment: "Nice product for the price. Would recommend to others.", date: "3 weeks ago"),
        ReviewData(author: "Omar T.", rating: 4.5, comment: "Very satisfied with this purchase. Will buy again.", date: "1 month ago"),
    ]

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                CircleBackButton { router.pop() }
                Spacer()
                Text("Reviews")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(AppTheme.textPrimary)
                Spacer()
                Color.clear.frame(width: 40, height: 40)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            // Product Info Header
            if let product = viewModel.product {
                HStack(spacing: 12) {
                    AsyncImage(url: URL(string: product.image)) { phase in
                        switch phase {
                        case .success(let image):
                            image.resizable().scaledToFit()
                        default:
                            Image(systemName: "photo")
                                .foregroundColor(AppTheme.divider)
                        }
                    }
                    .frame(width: 60, height: 60)
                    .background(AppTheme.surface)
                    .cornerRadius(8)

                    VStack(alignment: .leading, spacing: 4) {
                        Text(product.title)
                            .font(.system(size: 14, weight: .semibold))
                            .lineLimit(1)
                        HStack(spacing: 4) {
                            Image(systemName: "star.fill")
                                .font(.system(size: 12))
                                .foregroundColor(AppTheme.reviewStarActive)
                            Text(String(format: "%.1f (%d reviews)", product.rating.rate, product.rating.count))
                                .font(.system(size: 12))
                                .foregroundColor(AppTheme.textSecondary)
                        }
                    }
                    Spacer()
                }
                .padding(16)
                .background(Color.white)

                Divider()
            }

            // Reviews List
            List(reviews) { review in
                ReviewRow(review: review)
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
            }
            .listStyle(.plain)
        }
        .background(AppTheme.background)
        .navigationBarHidden(true)
        .task {
            viewModel.configure(repository: container.repository)
            await viewModel.fetchProduct(id: productId)
        }
    }
}

struct ReviewData: Identifiable {
    let id = UUID()
    let author: String
    let rating: Double
    let comment: String
    let date: String
}
