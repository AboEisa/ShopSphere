import SwiftUI

struct CartItemRow: View {
    let item: CartItem
    let onQuantityChange: (Int) -> Void
    let onRemove: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            // Product Image
            AsyncImage(url: URL(string: item.product.image)) { phase in
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
            .frame(width: 80, height: 80)
            .background(AppTheme.surface)
            .cornerRadius(8)

            // Details
            VStack(alignment: .leading, spacing: 6) {
                Text(item.product.title)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(AppTheme.textPrimary)
                    .lineLimit(2)

                Text(item.product.formattedPrice)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(AppTheme.primaryGreen)

                // Quantity Selector
                HStack(spacing: 0) {
                    Button {
                        onQuantityChange(item.quantity - 1)
                    } label: {
                        Image(systemName: "minus")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(AppTheme.textPrimary)
                            .frame(width: 32, height: 32)
                    }

                    Divider().frame(height: 20)

                    Text("\(item.quantity)")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(AppTheme.textPrimary)
                        .frame(width: 36)

                    Divider().frame(height: 20)

                    Button {
                        onQuantityChange(item.quantity + 1)
                    } label: {
                        Image(systemName: "plus")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(AppTheme.textPrimary)
                            .frame(width: 32, height: 32)
                    }
                }
                .background(AppTheme.lightGray)
                .cornerRadius(8)
            }

            Spacer()

            // Remove Button
            Button(action: onRemove) {
                Image(systemName: "trash")
                    .font(.system(size: 16))
                    .foregroundColor(AppTheme.errorRed)
                    .frame(width: 36, height: 36)
            }
        }
        .padding(12)
        .background(Color.white)
        .cornerRadius(AppTheme.cornerRadius)
        .shadow(color: .black.opacity(0.04), radius: 2, x: 0, y: 1)
    }
}
