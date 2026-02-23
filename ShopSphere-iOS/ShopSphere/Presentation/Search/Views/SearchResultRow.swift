import SwiftUI

struct SearchResultRow: View {
    let product: Product

    var body: some View {
        HStack(spacing: 12) {
            AsyncImage(url: URL(string: product.image)) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFit()
                case .failure, .empty:
                    Image(systemName: "photo")
                        .foregroundColor(AppTheme.divider)
                @unknown default:
                    Color.clear
                }
            }
            .frame(width: 70, height: 70)
            .background(AppTheme.surface)
            .cornerRadius(8)

            VStack(alignment: .leading, spacing: 4) {
                Text(product.title)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(AppTheme.textPrimary)
                    .lineLimit(2)

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

                HStack(spacing: 4) {
                    Image(systemName: "star.fill")
                        .font(.system(size: 10))
                        .foregroundColor(AppTheme.reviewStarActive)
                    Text(String(format: "%.1f", product.rating.rate))
                        .font(.system(size: 12))
                        .foregroundColor(AppTheme.textSecondary)
                }
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(.system(size: 12))
                .foregroundColor(AppTheme.textSecondary)
        }
        .padding(12)
        .background(Color.white)
        .cornerRadius(AppTheme.cornerRadius)
    }
}
