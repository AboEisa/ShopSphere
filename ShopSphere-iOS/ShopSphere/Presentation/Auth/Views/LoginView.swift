import SwiftUI

struct LoginView: View {
    @Binding var appState: AppState
    @Bindable var viewModel: AuthViewModel
    @State private var showPassword = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Header
                Text("Login")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(AppTheme.textPrimary)
                    .padding(.top, 40)

                // Email Field
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Image(systemName: "envelope")
                            .foregroundColor(AppTheme.textSecondary)
                        TextField("Email", text: $viewModel.email)
                            .foregroundColor(AppTheme.textPrimary)
                            .textContentType(.emailAddress)
                            .keyboardType(.emailAddress)
                            .autocapitalization(.none)
                        if viewModel.isEmailValid && !viewModel.email.isEmpty {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(AppTheme.primaryGreen)
                        }
                    }
                    .padding()
                    .background(AppTheme.lightGray)
                    .cornerRadius(AppTheme.cornerRadius)
                }

                // Password Field
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Image(systemName: "lock")
                            .foregroundColor(AppTheme.textSecondary)
                        if showPassword {
                            TextField("Password", text: $viewModel.password)
                                .foregroundColor(AppTheme.textPrimary)
                        } else {
                            SecureField("Password", text: $viewModel.password)
                                .foregroundColor(AppTheme.textPrimary)
                        }
                        Button { showPassword.toggle() } label: {
                            Image(systemName: showPassword ? "eye.slash" : "eye")
                                .foregroundColor(AppTheme.textSecondary)
                        }
                        if viewModel.isPasswordValid && !viewModel.password.isEmpty {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(AppTheme.primaryGreen)
                        }
                    }
                    .padding()
                    .background(AppTheme.lightGray)
                    .cornerRadius(AppTheme.cornerRadius)
                }

                // Forgot Password
                HStack {
                    Spacer()
                    Button("Forgot Password?") {}
                        .font(.system(size: 14))
                        .foregroundColor(AppTheme.primaryGreen)
                }

                // Login Button
                Button {
                    Task { await viewModel.login() }
                } label: {
                    ZStack {
                        Text("Login")
                            .primaryButton()
                            .opacity(viewModel.state == .loading ? 0 : 1)
                        if viewModel.state == .loading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .frame(maxWidth: .infinity)
                                .frame(height: 52)
                                .background(AppTheme.primaryGreen)
                                .cornerRadius(AppTheme.buttonCornerRadius)
                        }
                    }
                }
                .disabled(viewModel.state == .loading)

                // Divider
                HStack {
                    Rectangle().fill(AppTheme.divider).frame(height: 1)
                    Text("Or").foregroundColor(AppTheme.textSecondary).font(.system(size: 14))
                    Rectangle().fill(AppTheme.divider).frame(height: 1)
                }

                // Google Sign-In
                Button {
                    Task { await viewModel.loginWithGoogle() }
                } label: {
                    HStack {
                        Image(systemName: "g.circle.fill")
                            .font(.system(size: 20))
                        Text("Continue with Google")
                    }
                    .outlinedButton()
                }

                // Facebook Login
                Button {
                    Task { await viewModel.loginWithFacebook() }
                } label: {
                    HStack {
                        Image(systemName: "f.circle.fill")
                            .font(.system(size: 20))
                        Text("Continue with Facebook")
                    }
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color(hex: "1877F2"))
                    .cornerRadius(AppTheme.buttonCornerRadius)
                }

                // Register Link
                HStack {
                    Spacer()
                    Text("Don't have an account?")
                        .foregroundColor(AppTheme.textSecondary)
                    Button("Join Now") {
                        appState = .register
                    }
                    .foregroundColor(AppTheme.primaryGreen)
                    .fontWeight(.semibold)
                    Spacer()
                }
                .font(.system(size: 14))
                .padding(.top, 8)
            }
            .padding(.horizontal, 24)
        }
        .background(AppTheme.background)
        .onChange(of: viewModel.state) { _, newValue in
            if newValue == .success {
                viewModel.consumeState()
                withAnimation { appState = .main }
            }
        }
        .alert("Error", isPresented: Binding(
            get: { if case .error = viewModel.state { return true }; return false },
            set: { if !$0 { viewModel.consumeState() } }
        )) {
            Button("OK") { viewModel.consumeState() }
        } message: {
            if case .error(let msg) = viewModel.state {
                Text(msg)
            }
        }
    }
}
