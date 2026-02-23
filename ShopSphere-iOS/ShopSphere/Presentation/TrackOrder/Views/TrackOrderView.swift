import SwiftUI
import MapKit

struct TrackOrderView: View {
    let orderId: String
    @State private var viewModel = TrackOrderViewModel()
    @State private var cameraPosition: MapCameraPosition = .automatic
    @Bindable var checkoutViewModel: CheckoutViewModel
    @Environment(NavigationRouter.self) private var router

    var body: some View {
        ZStack {
            // Map
            Map(position: $cameraPosition) {
                // Courier Marker
                Annotation("Courier", coordinate: viewModel.courierPosition) {
                    Image(systemName: "car.fill")
                        .font(.system(size: 20))
                        .foregroundColor(.white)
                        .padding(8)
                        .background(viewModel.hasArrived ? AppTheme.textSecondary : AppTheme.primaryGreen)
                        .clipShape(Circle())
                        .rotationEffect(.degrees(viewModel.courierHeading))
                        .shadow(color: .black.opacity(0.3), radius: 4)
                }

                // Destination Marker
                Annotation("Destination", coordinate: viewModel.destinationPosition) {
                    Image(systemName: "mappin.circle.fill")
                        .font(.system(size: 28))
                        .foregroundColor(AppTheme.errorRed)
                        .shadow(color: .black.opacity(0.3), radius: 4)
                }

                // Route Polyline
                if let route = viewModel.route, !viewModel.hasArrived {
                    MapPolyline(route.polyline)
                        .stroke(Color.black.opacity(0.3), lineWidth: 6)
                    MapPolyline(route.polyline)
                        .stroke(Color(hex: "111111"), lineWidth: 4)
                }
            }
            .mapStyle(.standard)
            .ignoresSafeArea()

            // Top scrim
            VStack {
                // Top Header
                HStack(spacing: 12) {
                    CircleBackButton { router.pop() }

                    VStack(alignment: .leading, spacing: 2) {
                        Text("Order #\(orderId)")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(AppTheme.textPrimary)
                        Text(viewModel.lastUpdated)
                            .font(.system(size: 11))
                            .foregroundColor(AppTheme.textSecondary)
                    }

                    Spacer()

                    // Distance/ETA pill
                    HStack(spacing: 8) {
                        Text(viewModel.distanceText)
                            .font(.system(size: 12, weight: .semibold))
                        Text("|")
                            .foregroundColor(AppTheme.divider)
                        Text(viewModel.etaText)
                            .font(.system(size: 12, weight: .semibold))
                    }
                    .foregroundColor(AppTheme.textPrimary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(.ultraThinMaterial)
                    .cornerRadius(16)
                }
                .padding(.horizontal, 16)
                .padding(.top, 60)
                .background(
                    LinearGradient(
                        colors: [Color.white.opacity(0.8), Color.clear],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .ignoresSafeArea()
                )

                Spacer()
            }

            // Bottom Sheet
            VStack {
                Spacer()
                CourierInfoSheet(
                    order: viewModel.order,
                    effectiveStatusStep: viewModel.effectiveStatusStep,
                    liveStatusText: viewModel.liveStatusText,
                    onCallCourier: { viewModel.callCourier() }
                )
            }
        }
        .navigationBarHidden(true)
        .task {
            // Wait briefly for Firestore to load
            try? await Task.sleep(for: .milliseconds(500))
            if let order = checkoutViewModel.getOrder(by: orderId) {
                viewModel.loadOrder(order)
                await viewModel.fetchRoute()

                // Center camera
                let midLat = (viewModel.courierPosition.latitude + viewModel.destinationPosition.latitude) / 2
                let midLng = (viewModel.courierPosition.longitude + viewModel.destinationPosition.longitude) / 2
                cameraPosition = .region(MKCoordinateRegion(
                    center: CLLocationCoordinate2D(latitude: midLat, longitude: midLng),
                    span: MKCoordinateSpan(latitudeDelta: 0.06, longitudeDelta: 0.06)
                ))

                viewModel.startCourierAnimation()
            }
        }
        .onDisappear {
            viewModel.stopAnimation()
        }
    }
}
