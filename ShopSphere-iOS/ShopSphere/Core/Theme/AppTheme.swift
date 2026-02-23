import SwiftUI

enum AppTheme {
    // MARK: - Brand Colors
    static let primaryGreen = Color(hex: "23a36e")
    static let primaryGreenDark = Color(hex: "0F7A5C")
    static let secondaryGreen = Color(hex: "00A86B")
    static let primaryGreenRipple = Color(hex: "23a36e").opacity(0.12)
    static let primaryGreenIndicator = Color(hex: "23a36e").opacity(0.10)

    // MARK: - UI Colors
    static let background = Color.white
    static let surface = Color(hex: "FAFAFA")
    static let textPrimary = Color(hex: "212121")
    static let textSecondary = Color(hex: "666666")
    static let lightGray = Color(hex: "F6F6F6")
    static let divider = Color(hex: "E0E0E0")
    static let errorRed = Color(hex: "FF3434")

    // MARK: - Status Colors
    static let statusBlue = Color(hex: "1E40AF")
    static let statusOrange = Color(hex: "92400E")
    static let reviewStarActive = Color(hex: "F59E0B")

    // MARK: - Shimmer
    static let shimmerColor = Color(hex: "E0E0E0")
    static let shimmerHighlight = Color(hex: "FAFAFA")

    // MARK: - Constants
    static let shippingCost: Double = 80.0
    static let cornerRadius: CGFloat = 12
    static let cardCornerRadius: CGFloat = 16
    static let buttonCornerRadius: CGFloat = 24
}

// MARK: - Color Extension for Hex
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3:
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6:
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8:
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}
