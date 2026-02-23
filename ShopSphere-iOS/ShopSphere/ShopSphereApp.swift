import SwiftUI
import FirebaseCore
import StripePaymentSheet
import GoogleSignIn
import FacebookCore

@main
struct ShopSphereApp: App {
    @State private var container: DependencyContainer

    init() {
        FirebaseApp.configure()
        StripeAPI.defaultPublishableKey = "pk_test_51N4k1bLz2Z5Y3X9Q1c7x2zGQeN7m7AtpFg4YkVKqG61KtvYPRx8yL9fThAtHV9ZoASQkQ7BHOcDnMBo2ULGMaJX300C3bBoJx3"
        _container = State(initialValue: DependencyContainer())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(\.container, container)
        }
    }
}
