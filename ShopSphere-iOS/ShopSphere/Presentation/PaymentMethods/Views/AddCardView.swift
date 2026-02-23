import SwiftUI

struct AddCardView: View {
    @State private var cardNumber = ""
    @State private var holderName = ""
    @State private var expiryMonth = ""
    @State private var expiryYear = ""
    @State private var cvv = ""
    @Bindable var checkoutViewModel: CheckoutViewModel
    @Environment(NavigationRouter.self) private var router

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                CircleBackButton { router.pop() }
                Spacer()
                Text("Add Card")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(AppTheme.textPrimary)
                Spacer()
                Color.clear.frame(width: 40, height: 40)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            ScrollView {
                VStack(spacing: 16) {
                    // Card Number
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Card Number")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(AppTheme.textPrimary)
                        TextField("1234 5678 9012 3456", text: $cardNumber)
                            .foregroundColor(AppTheme.textPrimary)
                            .keyboardType(.numberPad)
                            .padding(12)
                            .background(AppTheme.lightGray)
                            .cornerRadius(AppTheme.cornerRadius)
                    }

                    // Holder Name
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Card Holder Name")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(AppTheme.textPrimary)
                        TextField("John Doe", text: $holderName)
                            .foregroundColor(AppTheme.textPrimary)
                            .padding(12)
                            .background(AppTheme.lightGray)
                            .cornerRadius(AppTheme.cornerRadius)
                    }

                    // Expiry + CVV
                    HStack(spacing: 12) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Expiry Date")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(AppTheme.textPrimary)
                            HStack(spacing: 8) {
                                TextField("MM", text: $expiryMonth)
                                    .foregroundColor(AppTheme.textPrimary)
                                    .keyboardType(.numberPad)
                                    .frame(width: 50)
                                    .multilineTextAlignment(.center)
                                    .padding(12)
                                    .background(AppTheme.lightGray)
                                    .cornerRadius(AppTheme.cornerRadius)
                                Text("/")
                                    .foregroundColor(AppTheme.textSecondary)
                                TextField("YY", text: $expiryYear)
                                    .foregroundColor(AppTheme.textPrimary)
                                    .keyboardType(.numberPad)
                                    .frame(width: 50)
                                    .multilineTextAlignment(.center)
                                    .padding(12)
                                    .background(AppTheme.lightGray)
                                    .cornerRadius(AppTheme.cornerRadius)
                            }
                        }

                        VStack(alignment: .leading, spacing: 6) {
                            Text("CVV")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(AppTheme.textPrimary)
                            SecureField("123", text: $cvv)
                                .foregroundColor(AppTheme.textPrimary)
                                .keyboardType(.numberPad)
                                .padding(12)
                                .background(AppTheme.lightGray)
                                .cornerRadius(AppTheme.cornerRadius)
                        }
                    }
                }
                .padding(16)
            }

            // Save Button
            Button {
                saveCard()
            } label: {
                Text("Save Card")
                    .primaryButton()
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 12)
        }
        .background(AppTheme.background)
        .navigationBarHidden(true)
    }

    private func saveCard() {
        guard !cardNumber.isEmpty, !holderName.isEmpty else { return }
        let lastFour = String(cardNumber.filter(\.isNumber).suffix(4))
        let brand = detectBrand(cardNumber)
        checkoutViewModel.setCardLastFour(
            lastFour: lastFour,
            holderName: holderName,
            brand: brand
        )
        router.pop()
    }

    private func detectBrand(_ number: String) -> String {
        let cleaned = number.replacingOccurrences(of: " ", with: "")
        if cleaned.hasPrefix("4") { return "Visa" }
        if cleaned.hasPrefix("5") { return "Mastercard" }
        return "Card"
    }
}
