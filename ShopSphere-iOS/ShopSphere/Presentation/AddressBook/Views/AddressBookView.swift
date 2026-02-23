import SwiftUI

struct AddressBookView: View {
    @Bindable var checkoutViewModel: CheckoutViewModel
    @Environment(NavigationRouter.self) private var router

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                CircleBackButton { router.pop() }
                Spacer()
                Text("Address Book")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(AppTheme.textPrimary)
                Spacer()
                Color.clear.frame(width: 40, height: 40)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            if checkoutViewModel.addressBook.isEmpty {
                VStack(spacing: 12) {
                    Spacer()
                    Image(systemName: "mappin.circle")
                        .font(.system(size: 48))
                        .foregroundColor(AppTheme.textSecondary.opacity(0.5))
                    Text("No Saved Addresses")
                        .font(.system(size: 18, weight: .semibold))
                    Text("Add a delivery address to get started")
                        .font(.system(size: 14))
                        .foregroundColor(AppTheme.textSecondary)
                    Spacer()
                }
            } else {
                List(checkoutViewModel.addressBook) { address in
                    AddressRow(
                        address: address,
                        isSelected: address.isSelected
                    )
                    .onTapGesture {
                        checkoutViewModel.selectAddress(address.id)
                    }
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                }
                .listStyle(.plain)
            }

            VStack(spacing: 8) {
                Button {
                    router.push(.mapPicker)
                } label: {
                    HStack {
                        Image(systemName: "plus")
                        Text("Add New Address")
                    }
                    .outlinedButton()
                }

                if checkoutViewModel.selectedAddress != nil {
                    Button {
                        router.pop()
                    } label: {
                        Text("Use This Address")
                            .primaryButton()
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 12)
        }
        .background(AppTheme.background)
        .navigationBarHidden(true)
    }
}
