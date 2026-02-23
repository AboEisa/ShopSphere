import SwiftUI

struct HelpCenterView: View {
    @Environment(NavigationRouter.self) private var router

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                CircleBackButton { router.pop() }
                Spacer()
                Text("Help Center")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(AppTheme.textPrimary)
                Spacer()
                Color.clear.frame(width: 40, height: 40)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            ScrollView {
                VStack(spacing: 16) {
                    // Contact Options
                    VStack(alignment: .leading, spacing: 16) {
                        Text("Contact Us")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(AppTheme.textPrimary)

                        // Email
                        contactRow(
                            icon: "envelope.fill",
                            title: "Email Support",
                            subtitle: AppConstants.supportEmail,
                            action: {
                                if let url = URL(string: "mailto:\(AppConstants.supportEmail)") {
                                    UIApplication.shared.open(url)
                                }
                            }
                        )

                        // Phone
                        contactRow(
                            icon: "phone.fill",
                            title: "Phone Support",
                            subtitle: AppConstants.supportPhone,
                            action: {
                                if let url = URL(string: "tel://\(AppConstants.supportPhone)") {
                                    UIApplication.shared.open(url)
                                }
                            }
                        )

                        // Hours
                        contactRow(
                            icon: "clock.fill",
                            title: "Working Hours",
                            subtitle: AppConstants.supportHours,
                            action: nil
                        )
                    }
                    .cardStyle()

                    // Help Topics
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Help Topics")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(AppTheme.textPrimary)

                        helpTopicRow(icon: "shippingbox", title: "Shipping & Delivery")
                        helpTopicRow(icon: "arrow.uturn.left", title: "Returns & Refunds")
                        helpTopicRow(icon: "creditcard", title: "Payment Issues")
                        helpTopicRow(icon: "person", title: "Account Management")
                        helpTopicRow(icon: "gift", title: "Promotions & Coupons")
                    }
                    .cardStyle()
                }
                .padding(16)
            }
        }
        .background(AppTheme.surface)
        .navigationBarHidden(true)
    }

    private func contactRow(icon: String, title: String, subtitle: String, action: (() -> Void)?) -> some View {
        Button {
            action?()
        } label: {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 18))
                    .foregroundColor(AppTheme.primaryGreen)
                    .frame(width: 40, height: 40)
                    .background(AppTheme.primaryGreen.opacity(0.1))
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(AppTheme.textPrimary)
                    Text(subtitle)
                        .font(.system(size: 13))
                        .foregroundColor(AppTheme.textSecondary)
                }

                Spacer()

                if action != nil {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12))
                        .foregroundColor(AppTheme.textSecondary)
                }
            }
        }
        .disabled(action == nil)
    }

    private func helpTopicRow(icon: String, title: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 16))
                .foregroundColor(AppTheme.textPrimary)
                .frame(width: 24)

            Text(title)
                .font(.system(size: 15))
                .foregroundColor(AppTheme.textPrimary)

            Spacer()

            Image(systemName: "chevron.right")
                .font(.system(size: 12))
                .foregroundColor(AppTheme.textSecondary)
        }
        .padding(.vertical, 6)
    }
}
