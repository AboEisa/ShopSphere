import SwiftUI

struct MainTabView: View {
    @Bindable var authViewModel: AuthViewModel
    @Binding var appState: AppState
    @State private var selectedTab: Tab = .home
    @State private var homeRouter = NavigationRouter()
    @State private var searchRouter = NavigationRouter()
    @State private var savedRouter = NavigationRouter()
    @State private var cartRouter = NavigationRouter()
    @State private var accountRouter = NavigationRouter()
    @State private var checkoutViewModel = CheckoutViewModel()
    @Environment(\.container) private var container

    var body: some View {
        TabView(selection: $selectedTab) {
            NavigationStack(path: $homeRouter.path) {
                HomeView()
                    .navigationDestinations(router: homeRouter, checkoutViewModel: checkoutViewModel)
            }
            .environment(homeRouter)
            .tabItem {
                Label("Home", systemImage: "house")
            }
            .tag(Tab.home)

            NavigationStack(path: $searchRouter.path) {
                SearchView()
                    .navigationDestinations(router: searchRouter, checkoutViewModel: checkoutViewModel)
            }
            .environment(searchRouter)
            .tabItem {
                Label("Search", systemImage: "magnifyingglass")
            }
            .tag(Tab.search)

            NavigationStack(path: $savedRouter.path) {
                SavedView()
                    .navigationDestinations(router: savedRouter, checkoutViewModel: checkoutViewModel)
            }
            .environment(savedRouter)
            .tabItem {
                Label("Saved", systemImage: "heart")
            }
            .tag(Tab.saved)

            NavigationStack(path: $cartRouter.path) {
                CartView()
                    .navigationDestinations(router: cartRouter, checkoutViewModel: checkoutViewModel)
            }
            .environment(cartRouter)
            .tabItem {
                Label("Cart", systemImage: "cart")
            }
            .tag(Tab.cart)

            NavigationStack(path: $accountRouter.path) {
                AccountView(authViewModel: authViewModel, appState: $appState)
                    .navigationDestinations(router: accountRouter, checkoutViewModel: checkoutViewModel)
            }
            .environment(accountRouter)
            .tabItem {
                Label("Account", systemImage: "person")
            }
            .tag(Tab.account)
        }
        .tint(AppTheme.primaryGreen)
    }
}

// MARK: - Navigation Destinations

struct NavigationDestinationsModifier: ViewModifier {
    let router: NavigationRouter
    @Bindable var checkoutViewModel: CheckoutViewModel

    func body(content: Content) -> some View {
        content
            .navigationDestination(for: AppRoute.self) { route in
                switch route {
                case .productDetail(let id):
                    ProductDetailView(productId: id)
                case .reviews(let id):
                    ReviewsView(productId: id)
                case .checkout:
                    CheckoutView(checkoutViewModel: checkoutViewModel)
                case .trackOrder(let orderId):
                    TrackOrderView(orderId: orderId, checkoutViewModel: checkoutViewModel)
                case .addressBook:
                    AddressBookView(checkoutViewModel: checkoutViewModel)
                case .mapPicker:
                    MapPickerView(checkoutViewModel: checkoutViewModel)
                case .paymentMethods:
                    PaymentMethodsView(checkoutViewModel: checkoutViewModel)
                case .addCard:
                    AddCardView(checkoutViewModel: checkoutViewModel)
                case .orders:
                    OrdersView(checkoutViewModel: checkoutViewModel)
                case .myDetails:
                    MyDetailsView()
                case .faqs:
                    FAQsView()
                case .helpCenter:
                    HelpCenterView()
                }
            }
            .environment(router)
    }
}

extension View {
    func navigationDestinations(router: NavigationRouter, checkoutViewModel: CheckoutViewModel) -> some View {
        modifier(NavigationDestinationsModifier(router: router, checkoutViewModel: checkoutViewModel))
    }
}
