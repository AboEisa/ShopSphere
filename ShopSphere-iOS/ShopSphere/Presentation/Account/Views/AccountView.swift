import SwiftUI
import FirebaseAuth

struct AccountView: View {
    @Bindable var authViewModel: AuthViewModel
    @Binding var appState: AppState
    @State private var showLogoutAlert = false
    @Environment(\.container) private var container
    @Environment(NavigationRouter.self) private var router

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Text("Account")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(AppTheme.primaryGreen)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            ScrollView {
                VStack(spacing: 16) {
                    // Profile Card
                    profileCard

                    // General Section
                    sectionCard(title: "GENERAL", items: [
                        MenuItem(icon: "shippingbox", title: "My Orders", route: .orders),
                        MenuItem(icon: "person.text.rectangle", title: "My Details", route: .myDetails),
                        MenuItem(icon: "mappin.and.ellipse", title: "Address Book", route: .addressBook),
                        MenuItem(icon: "creditcard", title: "Payment Methods", route: .paymentMethods),
                    ])

                    // Support Section
                    sectionCard(title: "SUPPORT", items: [
                        MenuItem(icon: "questionmark.circle", title: "FAQs", route: .faqs),
                        MenuItem(icon: "headphones", title: "Help Center", route: .helpCenter),
                    ])

                    // Logout
                    Button {
                        showLogoutAlert = true
                    } label: {
                        HStack {
                            Image(systemName: "rectangle.portrait.and.arrow.right")
                                .foregroundColor(AppTheme.errorRed)
                            Text("Logout")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(AppTheme.errorRed)
                            Spacer()
                        }
                        .padding(16)
                        .background(AppTheme.errorRed.opacity(0.08))
                        .cornerRadius(AppTheme.cornerRadius)
                    }
                }
                .padding(16)
            }
        }
        .background(AppTheme.background)
        .navigationBarHidden(true)
        .alert("Logout", isPresented: $showLogoutAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Logout", role: .destructive) {
                authViewModel.logout()
                withAnimation { appState = .login }
            }
        } message: {
            Text("Are you sure you want to logout?")
        }
    }

    // MARK: - Profile Card

    private var profileCard: some View {
        HStack(spacing: 14) {
            // Profile Image
            Circle()
                .fill(AppTheme.primaryGreen.opacity(0.2))
                .frame(width: 56, height: 56)
                .overlay(
                    Group {
                        if let photoURL = Auth.auth().currentUser?.photoURL {
                            AsyncImage(url: photoURL) { phase in
                                switch phase {
                                case .success(let image):
                                    image.resizable().scaledToFill()
                                default:
                                    initialsView
                                }
                            }
                            .clipShape(Circle())
                        } else {
                            initialsView
                        }
                    }
                )

            VStack(alignment: .leading, spacing: 2) {
                Text(container.localStorage.profileName.isEmpty ?
                     (Auth.auth().currentUser?.displayName ?? "Guest User") :
                     container.localStorage.profileName)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(AppTheme.textPrimary)

                Text(container.localStorage.profileEmail.isEmpty ?
                     (Auth.auth().currentUser?.email ?? "Not signed in") :
                     container.localStorage.profileEmail)
                    .font(.system(size: 13))
                    .foregroundColor(AppTheme.textSecondary)
            }

            Spacer()
        }
        .cardStyle()
    }

    private var initialsView: some View {
        let name = container.localStorage.profileName.isEmpty ?
            (Auth.auth().currentUser?.displayName ?? "?") :
            container.localStorage.profileName
        return Text(String(name.prefix(1)).uppercased())
            .font(.system(size: 22, weight: .bold))
            .foregroundColor(AppTheme.primaryGreen)
    }

    // MARK: - Section Card

    private func sectionCard(title: String, items: [MenuItem]) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(title)
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(AppTheme.textSecondary)
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 4)

            VStack(spacing: 0) {
                ForEach(items) { item in
                    Button {
                        router.push(item.route)
                    } label: {
                        HStack(spacing: 12) {
                            Image(systemName: item.icon)
                                .font(.system(size: 16))
                                .foregroundColor(AppTheme.textPrimary)
                                .frame(width: 24)

                            Text(item.title)
                                .font(.system(size: 15))
                                .foregroundColor(AppTheme.textPrimary)

                            Spacer()

                            Image(systemName: "chevron.right")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(AppTheme.textSecondary)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 14)
                    }

                    if item.id != items.last?.id {
                        Divider().padding(.leading, 52)
                    }
                }
            }
            .padding(.bottom, 8)
        }
        .background(Color.white)
        .cornerRadius(AppTheme.cardCornerRadius)
        .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
    }
}

struct MenuItem: Identifiable {
    let id = UUID()
    let icon: String
    let title: String
    let route: AppRoute
}
