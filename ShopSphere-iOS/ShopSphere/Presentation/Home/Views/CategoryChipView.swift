import SwiftUI

struct CategoryChipView: View {
    let title: String
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(title.capitalized)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(isSelected ? .white : AppTheme.textPrimary)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(isSelected ? AppTheme.primaryGreen : Color.clear)
                .cornerRadius(20)
                .overlay(
                    RoundedRectangle(cornerRadius: 20)
                        .stroke(isSelected ? Color.clear : AppTheme.divider, lineWidth: 1)
                )
        }
    }
}
