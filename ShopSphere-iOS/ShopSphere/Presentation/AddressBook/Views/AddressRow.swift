import SwiftUI

struct AddressRow: View {
    let address: AddressBookItem
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

            VStack(alignment: .leading, spacing: 4) {
                Text(address.title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(AppTheme.textPrimary)
                Text(address.address)
                    .font(.system(size: 13))
                    .foregroundColor(AppTheme.textSecondary)
                    .lineLimit(2)
            }

            Spacer()

            if address.isDefault {
                Text("Default")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(AppTheme.primaryGreen)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(AppTheme.primaryGreen.opacity(0.1))
                    .cornerRadius(8)
            }
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
