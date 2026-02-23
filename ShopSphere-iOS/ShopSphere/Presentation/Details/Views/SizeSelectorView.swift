import SwiftUI

struct SizeSelectorView: View {
    let sizes: [String]
    @Binding var selected: String

    var body: some View {
        HStack(spacing: 8) {
            ForEach(sizes, id: \.self) { size in
                Button {
                    selected = size
                } label: {
                    Text(size)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(selected == size ? .white : AppTheme.textPrimary)
                        .frame(width: 44, height: 44)
                        .background(selected == size ? AppTheme.primaryGreen : Color.clear)
                        .cornerRadius(8)
                        .overlay(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(
                                    selected == size ? Color.clear : AppTheme.divider,
                                    lineWidth: 1
                                )
                        )
                }
            }
        }
    }
}
