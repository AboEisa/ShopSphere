import SwiftUI

struct MyDetailsView: View {
    @State private var fullName = ""
    @State private var email = ""
    @State private var phone = ""
    @State private var showSaved = false
    @Environment(\.container) private var container
    @Environment(NavigationRouter.self) private var router

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                CircleBackButton { router.pop() }
                Spacer()
                Text("My Details")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(AppTheme.textPrimary)
                Spacer()
                Color.clear.frame(width: 40, height: 40)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            ScrollView {
                VStack(spacing: 16) {
                    // Full Name
                    inputField(title: "Full Name", text: $fullName, icon: "person")

                    // Email
                    inputField(title: "Email", text: $email, icon: "envelope", keyboardType: .emailAddress)

                    // Phone
                    inputField(title: "Phone Number", text: $phone, icon: "phone", keyboardType: .phonePad)
                }
                .padding(16)
            }

            // Save Button
            Button {
                container.localStorage.saveProfile(name: fullName, email: email, phone: phone)
                showSaved = true
                Task {
                    try? await Task.sleep(for: .seconds(2))
                    showSaved = false
                }
            } label: {
                Text("Save Changes")
                    .primaryButton()
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 12)
        }
        .background(AppTheme.background)
        .navigationBarHidden(true)
        .onAppear {
            fullName = container.localStorage.profileName
            email = container.localStorage.profileEmail
            phone = container.localStorage.profilePhone
        }
        .overlay(alignment: .top) {
            if showSaved {
                Text("Changes saved")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(.white)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .background(AppTheme.primaryGreen)
                    .cornerRadius(20)
                    .transition(.move(edge: .top).combined(with: .opacity))
                    .padding(.top, 60)
            }
        }
        .animation(.easeInOut, value: showSaved)
    }

    private func inputField(
        title: String,
        text: Binding<String>,
        icon: String,
        keyboardType: UIKeyboardType = .default
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(AppTheme.textPrimary)

            HStack {
                Image(systemName: icon)
                    .foregroundColor(AppTheme.textSecondary)
                TextField(title, text: text)
                    .foregroundColor(AppTheme.textPrimary)
                    .keyboardType(keyboardType)
            }
            .padding(12)
            .background(AppTheme.lightGray)
            .cornerRadius(AppTheme.cornerRadius)
        }
    }
}
