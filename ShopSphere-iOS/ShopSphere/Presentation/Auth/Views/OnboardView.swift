import SwiftUI

struct OnboardView: View {
    @Binding var appState: AppState

    var body: some View {
        ZStack {
            // Background
            Color.white.ignoresSafeArea()

            VStack(spacing: 0) {
                // Green top section with image
                ZStack {
                    // Green gradient background
                    LinearGradient(
                        colors: [
                            Color(hex: "2BB87A"),
                            Color(hex: "1E9F6A")
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )

                    // Decorative elements
                    VStack(spacing: 20) {
                        Spacer()

                        // Shopping illustration
                        ZStack {
                            // Phone mockup
                            RoundedRectangle(cornerRadius: 20)
                                .fill(Color.white.opacity(0.15))
                                .frame(width: 160, height: 260)
                                .overlay(
                                    VStack(spacing: 12) {
                                        // Mini product grid inside phone
                                        HStack(spacing: 8) {
                                            RoundedRectangle(cornerRadius: 8)
                                                .fill(Color.white.opacity(0.25))
                                                .frame(width: 55, height: 55)
                                            RoundedRectangle(cornerRadius: 8)
                                                .fill(Color.white.opacity(0.25))
                                                .frame(width: 55, height: 55)
                                        }
                                        HStack(spacing: 8) {
                                            RoundedRectangle(cornerRadius: 8)
                                                .fill(Color.white.opacity(0.25))
                                                .frame(width: 55, height: 55)
                                            RoundedRectangle(cornerRadius: 8)
                                                .fill(Color.white.opacity(0.25))
                                                .frame(width: 55, height: 55)
                                        }
                                        // Cart button
                                        RoundedRectangle(cornerRadius: 12)
                                            .fill(Color.white.opacity(0.3))
                                            .frame(width: 120, height: 32)
                                            .overlay(
                                                HStack(spacing: 6) {
                                                    Image(systemName: "cart.fill")
                                                        .font(.system(size: 12))
                                                    Text("Shop Now")
                                                        .font(.system(size: 11, weight: .semibold))
                                                }
                                                .foregroundColor(.white)
                                            )
                                    }
                                    .padding(.top, 30)
                                )
                                .overlay(
                                    // Phone notch
                                    RoundedRectangle(cornerRadius: 4)
                                        .fill(Color.white.opacity(0.2))
                                        .frame(width: 50, height: 6)
                                        .padding(.top, 8),
                                    alignment: .top
                                )

                            // Floating shopping bag
                            Image(systemName: "bag.fill")
                                .font(.system(size: 28))
                                .foregroundColor(.white.opacity(0.9))
                                .offset(x: -90, y: -40)

                            // Floating heart
                            Image(systemName: "heart.fill")
                                .font(.system(size: 22))
                                .foregroundColor(.white.opacity(0.7))
                                .offset(x: 95, y: -60)

                            // Floating star
                            Image(systemName: "star.fill")
                                .font(.system(size: 18))
                                .foregroundColor(.white.opacity(0.6))
                                .offset(x: 80, y: 50)

                            // Floating tag
                            Image(systemName: "tag.fill")
                                .font(.system(size: 20))
                                .foregroundColor(.white.opacity(0.7))
                                .offset(x: -85, y: 70)
                        }

                        Spacer()
                            .frame(height: 30)
                    }
                }
                .frame(height: UIScreen.main.bounds.height * 0.55)
                .clipShape(
                    RoundedCornerShape(radius: 32, corners: [.bottomLeft, .bottomRight])
                )

                // Bottom section
                VStack(spacing: 20) {
                    Spacer()

                    // Title
                    Text("Define yourself\nin your unique way.")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(Color(hex: "1B1B1B"))
                        .multilineTextAlignment(.center)
                        .lineSpacing(4)

                    // Subtitle
                    Text("Discover the latest trends and express your style")
                        .font(.system(size: 15))
                        .foregroundColor(Color(hex: "888888"))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 40)

                    Spacer()

                    // Get Started Button
                    Button {
                        withAnimation { appState = .login }
                    } label: {
                        HStack(spacing: 8) {
                            Text("Get Started")
                                .font(.system(size: 17, weight: .semibold))
                            Image(systemName: "arrow.right")
                                .font(.system(size: 15, weight: .semibold))
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(Color(hex: "1B1B1B"))
                        .cornerRadius(28)
                    }
                    .padding(.horizontal, 24)
                    .padding(.bottom, 40)
                }
            }
        }
        .ignoresSafeArea(edges: .top)
    }
}

// MARK: - Rounded Corner Shape
private struct RoundedCornerShape: Shape {
    var radius: CGFloat
    var corners: UIRectCorner

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}
