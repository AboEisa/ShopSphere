import SwiftUI

struct PaymentMethodRow: View {
    let method: PaymentMethodItem
    let isSelected: Bool

    var body: some View {
        HStack(spacing: 12) {
            // Radio Button
            Circle()
                .strokeBorder(isSelected ? AppTheme.primaryGreen : AppTheme.divider, lineWidth: 2)
                .background(Circle().fill(isSelected ? AppTheme.primaryGreen : Color.clear))
                .frame(width: 22, height: 22)
                .overlay(
                    Circle()
                        .fill(Color.white)
                        .frame(width: 8, height: 8)
                        .opacity(isSelected ? 1 : 0)
                )

            Image(systemName: method.brandIcon)
                .font(.system(size: 24))
                .foregroundColor(AppTheme.textPrimary)
                .frame(width: 40)

            VStack(alignment: .leading, spacing: 2) {
                Text(method.maskedNumber)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(AppTheme.textPrimary)
                Text(method.holderName)
                    .font(.system(size: 12))
                    .foregroundColor(AppTheme.textSecondary)
            }

            Spacer()
        }
        .padding(12)
        .background(isSelected ? AppTheme.primaryGreen.opacity(0.05) : Color.white)
        .cornerRadius(AppTheme.cornerRadius)
        .overlay(
            RoundedRectangle(cornerRadius: AppTheme.cornerRadius)
                .stroke(isSelected ? AppTheme.primaryGreen : AppTheme.divider.opacity(0.5), lineWidth: 1)
        )
    }
}
