import SwiftUI

struct ProductCardView: View {
    let product: Product
    let isFavorite: Bool
    var onFavoriteTap: (() -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Product Image
            ZStack(alignment: .topTrailing) {
                AsyncImage(url: URL(string: product.image)) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFit()
                            .frame(maxWidth: .infinity)
                            .frame(height: 150)
                    case .failure:
                        imagePlaceholder
                    case .empty:
                        ProgressView()
                            .frame(maxWidth: .infinity)
                            .frame(height: 150)
                    @unknown default:
                        imagePlaceholder
                    }
                }
                .background(AppTheme.surface)
                .cornerRadius(AppTheme.cornerRadius)

                // Favorite Button
                if let onFavoriteTap {
                    Button { onFavoriteTap() } label: {
                        Image(systemName: isFavorite ? "heart.fill" : "heart")
                            .font(.system(size: 14))
                            .foregroundColor(isFavorite ? .red : AppTheme.textSecondary)
                            .frame(width: 32, height: 32)
                            .background(Color.white)
                            .clipShape(Circle())
                            .shadow(color: .black.opacity(0.1), radius: 2)
                    }
                    .padding(8)
                }
            }

            // Title
            Text(product.title)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(AppTheme.textPrimary)
                .lineLimit(2)

            // Price Row
            HStack(spacing: 4) {
                Text(product.formattedPrice)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(AppTheme.primaryGreen)

                if product.discountPercentage > 0 {
                    Text("-\(product.discountPercentage)%")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(AppTheme.errorRed)
                }
            }
        }
        .padding(8)
        .background(Color.white)
        .cornerRadius(AppTheme.cardCornerRadius)
        .overlay(
            RoundedRectangle(cornerRadius: AppTheme.cardCornerRadius)
                .stroke(AppTheme.divider.opacity(0.5), lineWidth: 1)
        )
    }

    private var imagePlaceholder: some View {
        Image(systemName: "photo")
            .font(.system(size: 40))
            .foregroundColor(AppTheme.divider)
            .frame(maxWidth: .infinity)
            .frame(height: 150)
    }
}
