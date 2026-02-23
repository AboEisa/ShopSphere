import SwiftUI

struct SavedProductCard: View {
    let product: Product
    let onRemove: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ZStack(alignment: .topTrailing) {
                AsyncImage(url: URL(string: product.image)) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFit()
                    case .failure, .empty:
                        Image(systemName: "photo")
                            .foregroundColor(AppTheme.divider)
                    @unknown default:
                        Color.clear
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 130)
                .background(AppTheme.surface)
                .cornerRadius(AppTheme.cornerRadius)

                Button(action: onRemove) {
                    Image(systemName: "heart.fill")
                        .font(.system(size: 14))
                        .foregroundColor(.red)
                        .frame(width: 32, height: 32)
                        .background(Color.white)
                        .clipShape(Circle())
                        .shadow(color: .black.opacity(0.1), radius: 2)
                }
                .padding(8)
            }

            Text(product.title)
                .font(.system(size: 13, weight: .medium))
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
        }
        .padding(8)
        .background(Color.white)
        .cornerRadius(AppTheme.cardCornerRadius)
        .overlay(
            RoundedRectangle(cornerRadius: AppTheme.cardCornerRadius)
                .stroke(AppTheme.divider.opacity(0.5), lineWidth: 1)
        )
    }
}
