import SwiftUI
import FirebaseAuth

struct SplashView: View {
    @Binding var appState: AppState
    @Environment(\.container) private var container

    @State private var showProgress = false
    @State private var logoScale: CGFloat = 0.6
    @State private var logoOpacity: Double = 0

    var body: some View {
        ZStack {
            // Dark background
            Color(hex: "1B1B1B")
                .ignoresSafeArea()

            VStack {
                Spacer()

                // Cross/Star Logo
                crossLogo
                    .scaleEffect(logoScale)
                    .opacity(logoOpacity)

                Spacer()

                // Loading Spinner
                if showProgress {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: Color.white.opacity(0.7)))
                        .scaleEffect(1.0)
                        .padding(.bottom, 80)
                }
            }
        }
        .task {
            withAnimation(.easeOut(duration: 0.6)) {
                logoScale = 1.0
                logoOpacity = 1.0
            }
            try? await Task.sleep(for: .seconds(0.3))
            withAnimation { showProgress = true }
            try? await Task.sleep(for: .seconds(AppConstants.splashDelaySeconds))

            // Read directly from UserDefaults to avoid any caching
            let isLoggedIn = UserDefaults.standard.bool(forKey: StorageKeys.isLoggedIn)
            print("🔑 Splash – isLoggedIn=\(isLoggedIn), firebaseUser=\(Auth.auth().currentUser?.uid ?? "nil")")

            if isLoggedIn {
                // Sync UID from Firebase if available
                if let user = Auth.auth().currentUser {
                    container.localStorage.uid = user.uid
                }
                withAnimation { appState = .main }
            } else {
                // Ensure Firebase is also signed out
                try? Auth.auth().signOut()
                withAnimation { appState = .onboard }
            }
        }
    }

    // MARK: - Cross Logo Shape
    private var crossLogo: some View {
        ZStack {
            // Vertical bar
            RoundedRectangle(cornerRadius: 8)
                .fill(Color.white)
                .frame(width: 24, height: 70)

            // Horizontal bar
            RoundedRectangle(cornerRadius: 8)
                .fill(Color.white)
                .frame(width: 70, height: 24)
        }
        .frame(width: 80, height: 80)
    }
}
