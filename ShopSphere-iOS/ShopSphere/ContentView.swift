import SwiftUI

enum AppState {
    case splash
    case onboard
    case login
    case register
    case main
}

struct ContentView: View {
    @State private var appState: AppState = .splash
    @State private var authViewModel = AuthViewModel()
    @Environment(\.container) private var container

    var body: some View {
        Group {
            switch appState {
            case .splash:
                SplashView(appState: $appState)

            case .onboard:
                OnboardView(appState: $appState)

            case .login:
                LoginView(appState: $appState, viewModel: authViewModel)

            case .register:
                RegisterView(appState: $appState, viewModel: authViewModel)

            case .main:
                MainTabView(authViewModel: authViewModel, appState: $appState)
            }
        }
        .animation(.easeInOut(duration: 0.3), value: appState)
        .onAppear {
            // Configure AuthViewModel once with DI container so logout
            // always has valid references to localStorage and repository.
            authViewModel.configure(
                repository: container.repository,
                localStorage: container.localStorage
            )
        }
    }
}
