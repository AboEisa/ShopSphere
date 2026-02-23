import SwiftUI
import Lottie

struct OnboardView: View {
    @Binding var appState: AppState

    var body: some View {
        VStack(spacing: 32) {
            Spacer()

            // Lottie Animation
            LottieView(animation: .named("onboard_animation"))
                .playbackMode(.playing(.toProgress(1, loopMode: .loop)))
                .frame(height: 300)

            VStack(spacing: 12) {
                Text("Welcome to ShopSphere")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(AppTheme.textPrimary)

                Text("Discover amazing products and enjoy seamless shopping")
                    .font(.system(size: 16))
                    .foregroundColor(AppTheme.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }

            Spacer()

            Button {
                withAnimation { appState = .login }
            } label: {
                Text("Get Started")
                    .primaryButton()
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 48)
        }
        .background(AppTheme.background)
    }
}
