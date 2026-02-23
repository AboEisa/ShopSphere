import SwiftUI

struct OrderRow: View {
    let order: OrderHistoryItem

    var body: some View {
        HStack(spacing: 12) {
            // Order Icon
            Circle()
                .fill(statusColor.opacity(0.15))
                .frame(width: 44, height: 44)
                .overlay(
                    Image(systemName: "shippingbox")
                        .font(.system(size: 18))
                        .foregroundColor(statusColor)
                )

            VStack(alignment: .leading, spacing: 4) {
                Text("Order #\(order.orderId)")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(AppTheme.textPrimary)

                Text(order.date)
                    .font(.system(size: 12))
                    .foregroundColor(AppTheme.textSecondary)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 4) {
                Text(order.total)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(AppTheme.primaryGreen)

                Text(order.status)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(statusColor)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(statusColor.opacity(0.12))
                    .cornerRadius(8)
            }
        }
        .padding(12)
        .background(Color.white)
        .cornerRadius(AppTheme.cornerRadius)
        .shadow(color: .black.opacity(0.04), radius: 2, x: 0, y: 1)
    }

    private var statusColor: Color {
        switch order.status.lowercased() {
        case "delivered": return AppTheme.primaryGreen
        case "in transit": return AppTheme.statusBlue
        case "packing", "picked": return AppTheme.statusOrange
        default: return AppTheme.textSecondary
        }
    }
}
