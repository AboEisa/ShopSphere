import SwiftUI

struct ShimmerProductCard: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            RoundedRectangle(cornerRadius: AppTheme.cornerRadius)
                .fill(AppTheme.shimmerColor)
                .frame(height: 150)

            RoundedRectangle(cornerRadius: 4)
                .fill(AppTheme.shimmerColor)
                .frame(height: 14)

            RoundedRectangle(cornerRadius: 4)
                .fill(AppTheme.shimmerColor)
                .frame(width: 80, height: 14)
        }
        .padding(8)
        .background(Color.white)
        .cornerRadius(AppTheme.cardCornerRadius)
        .overlay(
            RoundedRectangle(cornerRadius: AppTheme.cardCornerRadius)
                .stroke(AppTheme.divider.opacity(0.5), lineWidth: 1)
        )
        .shimmering()
    }
}
