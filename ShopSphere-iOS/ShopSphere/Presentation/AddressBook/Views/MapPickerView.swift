import SwiftUI
import MapKit

struct MapPickerView: View {
    @State private var cameraPosition: MapCameraPosition = .region(
        MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: 30.0444, longitude: 31.2357), // Cairo default
            span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05)
        )
    )
    @State private var selectedCoordinate: CLLocationCoordinate2D?
    @State private var nickname = ""
    @State private var addressText = ""
    @State private var isDefault = false
    @Bindable var checkoutViewModel: CheckoutViewModel
    @Environment(NavigationRouter.self) private var router

    var body: some View {
        ZStack(alignment: .bottom) {
            // Map
            Map(position: $cameraPosition) {
                if let coord = selectedCoordinate {
                    Annotation("", coordinate: coord) {
                        Image(systemName: "mappin.circle.fill")
                            .font(.system(size: 30))
                            .foregroundColor(AppTheme.errorRed)
                    }
                }
            }
            .onTapGesture { location in
                // Convert tap to coordinate using MapProxy would be ideal,
                // but for simplicity, we'll use a search/manual input approach
            }
            .mapControls {
                MapUserLocationButton()
                MapCompass()
            }
            .ignoresSafeArea(edges: .top)

            // Back Button
            VStack {
                HStack {
                    CircleBackButton { router.pop() }
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.top, 60)
                Spacer()
            }

            // Bottom Card
            VStack(spacing: 12) {
                TextField("Address nickname (e.g. Home, Work)", text: $nickname)
                    .padding(12)
                    .background(AppTheme.lightGray)
                    .cornerRadius(AppTheme.cornerRadius)

                TextField("Full address", text: $addressText)
                    .padding(12)
                    .background(AppTheme.lightGray)
                    .cornerRadius(AppTheme.cornerRadius)

                Toggle("Set as default", isOn: $isDefault)
                    .tint(AppTheme.primaryGreen)
                    .font(.system(size: 14))

                Button {
                    saveAddress()
                } label: {
                    Text("Save Address")
                        .primaryButton()
                }
            }
            .padding(16)
            .background(Color.white)
            .cornerRadius(20, corners: [.topLeft, .topRight])
            .shadow(color: .black.opacity(0.1), radius: 10, x: 0, y: -5)
        }
        .navigationBarHidden(true)
    }

    private func saveAddress() {
        guard !nickname.isEmpty, !addressText.isEmpty else { return }
        checkoutViewModel.setAddress(
            nick: nickname,
            full: addressText,
            latitude: selectedCoordinate?.latitude ?? 30.0444,
            longitude: selectedCoordinate?.longitude ?? 31.2357
        )
        router.pop()
    }
}

// MARK: - Corner Radius Extension
extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}
