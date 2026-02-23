import SwiftUI

struct OrderStatusTimeline: View {
    let currentStep: Int // 0-3 (Packing, Picked, In Transit, Delivered)

    private let steps = OrderHistoryItem.statusLabels

    var body: some View {
        HStack(spacing: 0) {
            ForEach(0..<steps.count, id: \.self) { index in
                VStack(spacing: 6) {
                    // Dot
                    Circle()
                        .fill(index <= currentStep ? AppTheme.primaryGreen : AppTheme.divider)
                        .frame(width: 14, height: 14)
                        .overlay(
                            Circle()
                                .fill(Color.white)
                                .frame(width: 6, height: 6)
                                .opacity(index <= currentStep ? 1 : 0)
                        )

                    // Label
                    Text(steps[index])
                        .font(.system(size: 10, weight: index <= currentStep ? .semibold : .regular))
                        .foregroundColor(index <= currentStep ? AppTheme.primaryGreen : AppTheme.textSecondary)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)

                if index < steps.count - 1 {
                    // Connecting Line
                    Rectangle()
                        .fill(index < currentStep ? AppTheme.primaryGreen : AppTheme.divider)
                        .frame(height: 2)
                        .offset(y: -10)
                }
            }
        }
    }
}
