import SwiftUI

struct FilterSheetView: View {
    var currentRange: ClosedRange<Double>?
    let onApply: (Double, Double) -> Void
    let onReset: () -> Void

    @State private var minPrice: Double = 0
    @State private var maxPrice: Double = 1000
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 24) {
            // Handle
            RoundedRectangle(cornerRadius: 2)
                .fill(AppTheme.divider)
                .frame(width: 40, height: 4)
                .padding(.top, 12)

            Text("Filter by Price")
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(AppTheme.textPrimary)

            VStack(spacing: 16) {
                // Price Labels
                HStack {
                    Text("EGP \(Int(minPrice))")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(AppTheme.textPrimary)
                    Spacer()
                    Text("EGP \(Int(maxPrice))")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(AppTheme.textPrimary)
                }

                // Min Slider
                VStack(alignment: .leading, spacing: 4) {
                    Text("Minimum Price")
                        .font(.system(size: 12))
                        .foregroundColor(AppTheme.textSecondary)
                    Slider(value: $minPrice, in: 0...1000, step: 10)
                        .tint(AppTheme.primaryGreen)
                }

                // Max Slider
                VStack(alignment: .leading, spacing: 4) {
                    Text("Maximum Price")
                        .font(.system(size: 12))
                        .foregroundColor(AppTheme.textSecondary)
                    Slider(value: $maxPrice, in: 0...1000, step: 10)
                        .tint(AppTheme.primaryGreen)
                }
            }
            .padding(.horizontal, 24)

            Spacer()

            // Buttons
            HStack(spacing: 12) {
                Button {
                    onReset()
                    dismiss()
                } label: {
                    Text("Reset")
                        .outlinedButton()
                }

                Button {
                    onApply(minPrice, maxPrice)
                    dismiss()
                } label: {
                    Text("Apply")
                        .primaryButton()
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
        }
        .onAppear {
            if let range = currentRange {
                minPrice = range.lowerBound
                maxPrice = range.upperBound
            }
        }
    }
}
