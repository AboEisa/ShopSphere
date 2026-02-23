import SwiftUI

struct CourierInfoSheet: View {
    let order: OrderHistoryItem?
    var effectiveStatusStep: Int = 0
    var liveStatusText: String = ""
    var onCallCourier: (() -> Void)?

    var body: some View {
        VStack(spacing: 16) {
            // Handle
            RoundedRectangle(cornerRadius: 2)
                .fill(AppTheme.divider)
                .frame(width: 40, height: 4)
                .padding(.top, 12)

            // Status Timeline
            if let order {
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text("Order Status")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(AppTheme.textPrimary)
                        Spacer()
                        Text(liveStatusText.isEmpty ? order.status : liveStatusText)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(AppTheme.primaryGreen)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(AppTheme.primaryGreen.opacity(0.12))
                            .cornerRadius(10)
                    }

                    OrderStatusTimeline(currentStep: effectiveStatusStep)
                        .padding(.vertical, 8)
                }
                .padding(.horizontal, 16)

                Divider()
                    .padding(.horizontal, 16)

                // Courier Info
                HStack(spacing: 12) {
                    Circle()
                        .fill(AppTheme.primaryGreen.opacity(0.2))
                        .frame(width: 48, height: 48)
                        .overlay(
                            Image(systemName: "person.fill")
                                .font(.system(size: 20))
                                .foregroundColor(AppTheme.primaryGreen)
                        )

                    VStack(alignment: .leading, spacing: 2) {
                        Text(order.customerName.isEmpty ? "Courier" : order.customerName)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(AppTheme.textPrimary)
                        Text("Delivery Courier")
                            .font(.system(size: 13))
                            .foregroundColor(AppTheme.textSecondary)
                    }

                    Spacer()

                    // Call Button
                    if !order.phone.isEmpty {
                        Button {
                            onCallCourier?()
                        } label: {
                            Image(systemName: "phone.fill")
                                .font(.system(size: 16))
                                .foregroundColor(AppTheme.primaryGreen)
                                .frame(width: 44, height: 44)
                                .background(AppTheme.primaryGreen.opacity(0.12))
                                .clipShape(Circle())
                        }
                    }
                }
                .padding(.horizontal, 16)

                // Delivery Address
                if !order.address.isEmpty {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Delivery Address")
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(AppTheme.textSecondary)
                        Text(order.address)
                            .font(.system(size: 14))
                            .foregroundColor(AppTheme.textPrimary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 16)
                }
            }
        }
        .padding(.bottom, 24)
        .background(Color.white)
        .cornerRadius(20, corners: [.topLeft, .topRight])
        .shadow(color: .black.opacity(0.1), radius: 10, x: 0, y: -5)
    }
}
