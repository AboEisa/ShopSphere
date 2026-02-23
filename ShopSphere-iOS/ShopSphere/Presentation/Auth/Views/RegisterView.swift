import SwiftUI

struct RegisterView: View {
    @Binding var appState: AppState
    @Bindable var viewModel: AuthViewModel
    @State private var showPassword = false
    @State private var showConfirmPassword = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Header
                Text("Create Account")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(AppTheme.textPrimary)
                    .padding(.top, 40)

                // Full Name
                validatedField(
                    icon: "person",
                    placeholder: "Full Name",
                    text: $viewModel.fullName,
                    isValid: viewModel.isNameValid
                )

                // Email
                validatedField(
                    icon: "envelope",
                    placeholder: "Email",
                    text: $viewModel.email,
                    isValid: viewModel.isEmailValid,
                    keyboardType: .emailAddress
                )

                // Password
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

                // Confirm Password
                HStack {
                    Image(systemName: "lock")
                        .foregroundColor(AppTheme.textSecondary)
                    if showConfirmPassword {
                        TextField("Confirm Password", text: $viewModel.confirmPassword)
                            .foregroundColor(AppTheme.textPrimary)
                    } else {
                        SecureField("Confirm Password", text: $viewModel.confirmPassword)
                            .foregroundColor(AppTheme.textPrimary)
                    }
                    Button { showConfirmPassword.toggle() } label: {
                        Image(systemName: showConfirmPassword ? "eye.slash" : "eye")
                            .foregroundColor(AppTheme.textSecondary)
                    }
                    if viewModel.isConfirmPasswordValid && !viewModel.confirmPassword.isEmpty {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundColor(AppTheme.primaryGreen)
                    }
                }
                .padding()
                .background(AppTheme.lightGray)
                .cornerRadius(AppTheme.cornerRadius)

                // Terms
                Text("By creating an account, you agree to our Terms & Conditions")
                    .font(.system(size: 12))
                    .foregroundColor(AppTheme.textSecondary)

                // Create Account Button
                Button {
                    Task { await viewModel.register() }
                } label: {
                    ZStack {
                        Text("Create Account")
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

                // Google Sign-Up
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

                // Facebook Sign-Up
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

                // Login Link
                HStack {
                    Spacer()
                    Text("Already have an account?")
                        .foregroundColor(AppTheme.textSecondary)
                    Button("Login") {
                        appState = .login
                    }
                    .foregroundColor(AppTheme.primaryGreen)
                    .fontWeight(.semibold)
                    Spacer()
                }
                .font(.system(size: 14))
                .padding(.top, 8)
                .padding(.bottom, 24)
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

    @ViewBuilder
    private func validatedField(
        icon: String,
        placeholder: String,
        text: Binding<String>,
        isValid: Bool,
        keyboardType: UIKeyboardType = .default
    ) -> some View {
        HStack {
            Image(systemName: icon)
                .foregroundColor(AppTheme.textSecondary)
            TextField(placeholder, text: text)
                .foregroundColor(AppTheme.textPrimary)
                .keyboardType(keyboardType)
                .autocapitalization(keyboardType == .emailAddress ? .none : .words)
            if isValid && !text.wrappedValue.isEmpty {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(AppTheme.primaryGreen)
            }
        }
        .padding()
        .background(AppTheme.lightGray)
        .cornerRadius(AppTheme.cornerRadius)
    }
}
