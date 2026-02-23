import SwiftUI

struct SplashView: View {
    @Binding var appState: AppState
    @Environment(\.container) private var container

    @State private var showProgress = false

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            VStack(spacing: 24) {
                Image(systemName: "bag.fill")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 80, height: 80)
                    .foregroundColor(AppTheme.primaryGreen)

                Text("ShopSphere")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(.white)

                if showProgress {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .scaleEffect(1.2)
                }
            }
        }
        .task {
            withAnimation { showProgress = true }
            try? await Task.sleep(for: .seconds(AppConstants.splashDelaySeconds))

            let isLoggedIn = container.localStorage.isLoggedIn ||
                             container.repository.currentUserId() != nil

            if isLoggedIn {
                // Sync auth state
                if let user = FirebaseAuth.Auth.auth().currentUser {
                    container.localStorage.uid = user.uid
                    container.localStorage.isLoggedIn = true
                }
                withAnimation { appState = .main }
            } else {
                withAnimation { appState = .onboard }
            }
        }
    }
}

import FirebaseAuth
