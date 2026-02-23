import SwiftUI
import FirebaseAuth
import GoogleSignIn
import FacebookLogin

enum AuthState: Equatable {
    case idle
    case loading
    case success
    case error(String)
}

@Observable
final class AuthViewModel {
    var state: AuthState = .idle
    var email = ""
    var password = ""
    var fullName = ""
    var confirmPassword = ""
    var isLoggedIn = false

    private var repository: RepositoryProtocol?
    private var localStorage: LocalStorageService?

    func configure(repository: RepositoryProtocol, localStorage: LocalStorageService) {
        self.repository = repository
        self.localStorage = localStorage
        isLoggedIn = checkLoginState()
    }

    // MARK: - Validation

    var isEmailValid: Bool { email.isValidEmail }
    var isPasswordValid: Bool { password.isValidPassword }
    var isNameValid: Bool { fullName.isValidName }
    var isConfirmPasswordValid: Bool { !confirmPassword.isEmpty && confirmPassword == password }

    var isLoginFormValid: Bool { isEmailValid && isPasswordValid }
    var isRegisterFormValid: Bool { isNameValid && isEmailValid && isPasswordValid && isConfirmPasswordValid }

    // MARK: - Auth State Check

    func checkLoginState() -> Bool {
        // localStorage.isLoggedIn is the source of truth.
        // It is set to true on login and cleared on logout.
        return localStorage?.isLoggedIn ?? false
    }

    func syncAuthState() {
        if let user = Auth.auth().currentUser {
            localStorage?.uid = user.uid
            localStorage?.isLoggedIn = true
            if let displayName = user.displayName, !displayName.isEmpty {
                localStorage?.profileName = displayName
            }
            if let userEmail = user.email, !userEmail.isEmpty {
                localStorage?.profileEmail = userEmail
            }
        }
    }

    // MARK: - Email/Password Login

    func login() async {
        guard isLoginFormValid else {
            state = .error("Please fill in all fields correctly")
            return
        }
        state = .loading
        do {
            try await repository?.login(email: email, password: password)
            markLoggedIn()
            state = .success
        } catch {
            state = .error(error.localizedDescription)
        }
    }

    // MARK: - Register

    func register() async {
        guard isRegisterFormValid else {
            state = .error("Please fill in all fields correctly")
            return
        }
        state = .loading
        do {
            let uid = try await repository?.register(name: fullName, email: email, password: password)
            if let uid = uid {
                localStorage?.uid = uid
            }
            localStorage?.saveProfile(name: fullName, email: email, phone: "")
            markLoggedIn()
            state = .success
        } catch {
            state = .error(error.localizedDescription)
        }
    }

    // MARK: - Google Sign-In

    func loginWithGoogle() async {
        guard let windowScene = await UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootVC = await windowScene.windows.first?.rootViewController else {
            state = .error("Cannot present Google Sign-In")
            return
        }
        state = .loading
        do {
            let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: rootVC)
            guard let idToken = result.user.idToken?.tokenString else {
                state = .error("Failed to get Google ID token")
                return
            }
            try await repository?.loginWithGoogle(idToken: idToken)
            markLoggedIn()
            state = .success
        } catch {
            state = .error(error.localizedDescription)
        }
    }

    // MARK: - Facebook Login

    func loginWithFacebook() async {
        let loginManager = LoginManager()
        state = .loading

        await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
            DispatchQueue.main.async {
                guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                      let rootVC = windowScene.windows.first?.rootViewController else {
                    self.state = .error("Cannot present Facebook Login")
                    continuation.resume()
                    return
                }
                loginManager.logIn(permissions: ["email", "public_profile"], from: rootVC) { result, error in
                    if let error = error {
                        self.state = .error(error.localizedDescription)
                        continuation.resume()
                        return
                    }
                    guard let result = result, !result.isCancelled,
                          let tokenString = AccessToken.current?.tokenString else {
                        self.state = .error("Facebook login cancelled")
                        continuation.resume()
                        return
                    }
                    Task {
                        do {
                            try await self.repository?.loginWithFacebook(accessToken: tokenString)
                            self.markLoggedIn()
                            self.state = .success
                        } catch {
                            self.state = .error(error.localizedDescription)
                        }
                        continuation.resume()
                    }
                }
            }
        }
    }

    // MARK: - Logout

    func logout() {
        // Clear local flag first so even if Firebase signOut fails,
        // the app won't treat the user as logged-in on next launch.
        localStorage?.isLoggedIn = false
        localStorage?.clear()
        repository?.logout()
        GIDSignIn.sharedInstance.signOut()
        LoginManager().logOut()
        isLoggedIn = false
        state = .idle
        clearFields()
    }

    // MARK: - Helpers

    private func markLoggedIn() {
        localStorage?.isLoggedIn = true
        syncAuthState()
        isLoggedIn = true
    }

    func consumeState() {
        state = .idle
    }

    func clearFields() {
        email = ""
        password = ""
        fullName = ""
        confirmPassword = ""
    }
}
