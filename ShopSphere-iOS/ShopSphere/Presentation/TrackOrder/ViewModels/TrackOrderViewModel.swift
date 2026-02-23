import SwiftUI
import MapKit
import CoreLocation

@Observable
final class TrackOrderViewModel {
    var order: OrderHistoryItem?
    var courierPosition: CLLocationCoordinate2D = CLLocationCoordinate2D()
    var destinationPosition: CLLocationCoordinate2D = CLLocationCoordinate2D()
    var route: MKRoute?
    var routePoints: [CLLocationCoordinate2D] = []
    var isAnimating = false
    var currentRouteIndex = 0
    var distanceText = ""
    var etaText = ""
    var lastUpdated = ""
    var hasArrived = false
    var liveStatusText = ""
    var transitAddressText = ""

    private let directionsService = DirectionsService()
    private var animationTimer: Timer?

    // Constants matching Android
    private static let defaultLat = 30.0444
    private static let defaultLng = 31.2357
    private static let arrivalDistanceThreshold: Double = 30.0 // meters
    private static let meterInMile: Double = 1609.34
    private static let metersPerMinute: Double = 240.0

    func loadOrder(_ order: OrderHistoryItem) {
        self.order = order
        destinationPosition = CLLocationCoordinate2D(
            latitude: order.destinationLat ?? Self.defaultLat,
            longitude: order.destinationLng ?? Self.defaultLng
        )
        courierPosition = CLLocationCoordinate2D(
            latitude: order.currentLat ?? (destinationPosition.latitude + 0.02),
            longitude: order.currentLng ?? (destinationPosition.longitude + 0.01)
        )

        let formatter = DateFormatter()
        formatter.dateFormat = "h:mm a"
        formatter.locale = Locale(identifier: "en_US")
        lastUpdated = "Last updated: \(formatter.string(from: Date()))"

        // Check arrival
        hasArrived = isOrderArrived(order)
        updateStatusTexts(order)
        updateDistanceAndEta()
    }

    func updateOrder(_ order: OrderHistoryItem) {
        let previousOrder = self.order
        self.order = order

        let newCourier = CLLocationCoordinate2D(
            latitude: order.currentLat ?? destinationPosition.latitude,
            longitude: order.currentLng ?? destinationPosition.longitude
        )

        hasArrived = isOrderArrived(order)
        updateStatusTexts(order)

        if hasArrived {
            stopAnimation()
            courierPosition = destinationPosition
            updateDistanceAndEta()
        } else {
            courierPosition = newCourier
            updateDistanceAndEta()
        }

        let formatter = DateFormatter()
        formatter.dateFormat = "h:mm a"
        formatter.locale = Locale(identifier: "en_US")
        lastUpdated = "Last updated: \(formatter.string(from: Date()))"
    }

    func fetchRoute() async {
        do {
            let fetchedRoute = try await directionsService.getRoute(
                from: courierPosition,
                to: destinationPosition
            )
            route = fetchedRoute
            routePoints = directionsService.getRoutePoints(from: fetchedRoute)

            let distanceKm = fetchedRoute.distance / 1000
            let distanceMiles = distanceKm * 0.621371
            distanceText = String(format: "%.1f mi", distanceMiles)

            let etaMinutes = fetchedRoute.expectedTravelTime / 60
            etaText = String(format: "%.0f min", etaMinutes)
        } catch {
            routePoints = [courierPosition, destinationPosition]
            updateDistanceAndEta()
        }
    }

    func startCourierAnimation() {
        guard !routePoints.isEmpty, !hasArrived else { return }
        isAnimating = true
        currentRouteIndex = 0

        animationTimer = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { [weak self] _ in
            guard let self else { return }
            guard self.currentRouteIndex < self.routePoints.count - 1 else {
                self.stopAnimation()
                return
            }
            self.currentRouteIndex += 1
            self.courierPosition = self.routePoints[self.currentRouteIndex]
        }
    }

    func stopAnimation() {
        animationTimer?.invalidate()
        animationTimer = nil
        isAnimating = false
    }

    var courierHeading: Double {
        guard currentRouteIndex < routePoints.count - 1 else { return 0 }
        let from = routePoints[currentRouteIndex]
        let to = routePoints[min(currentRouteIndex + 1, routePoints.count - 1)]
        return bearing(from: from, to: to)
    }

    var effectiveStatusStep: Int {
        guard let order else { return 0 }
        return hasArrived ? OrderHistoryItem.stepDelivered : order.statusStep
    }

    // MARK: - Call Courier

    func callCourier() {
        let phone = order?.phone ?? ""
        guard !phone.isEmpty, let url = URL(string: "tel://\(phone)") else { return }
        UIApplication.shared.open(url)
    }

    var courierPhone: String {
        order?.phone ?? ""
    }

    // MARK: - Private Helpers

    private func isOrderArrived(_ order: OrderHistoryItem) -> Bool {
        if order.statusStep >= OrderHistoryItem.stepDelivered { return true }
        if order.status.caseInsensitiveCompare("Delivered") == .orderedSame { return true }

        let current = CLLocation(
            latitude: order.currentLat ?? destinationPosition.latitude,
            longitude: order.currentLng ?? destinationPosition.longitude
        )
        let destination = CLLocation(
            latitude: order.destinationLat ?? Self.defaultLat,
            longitude: order.destinationLng ?? Self.defaultLng
        )

        return order.statusStep >= OrderHistoryItem.stepInTransit &&
            current.distance(from: destination) <= Self.arrivalDistanceThreshold
    }

    private func updateStatusTexts(_ order: OrderHistoryItem) {
        if hasArrived {
            liveStatusText = "Delivered"
            transitAddressText = "Arrived at destination"
        } else {
            liveStatusText = order.status.isEmpty ? "Packing" : order.status
            transitAddressText = String(
                format: "%.4f, %.4f",
                order.currentLat ?? 0.0,
                order.currentLng ?? 0.0
            )
        }
    }

    private func updateDistanceAndEta() {
        if hasArrived {
            distanceText = "Arrived"
            etaText = "Delivered"
            return
        }

        let current = CLLocation(latitude: courierPosition.latitude, longitude: courierPosition.longitude)
        let destination = CLLocation(latitude: destinationPosition.latitude, longitude: destinationPosition.longitude)
        let distanceMeters = current.distance(from: destination)

        let miles = distanceMeters / Self.meterInMile
        distanceText = String(format: "%.1f mi away", miles)

        let minutes: Int
        if distanceMeters < 15 {
            minutes = 0
        } else {
            minutes = max(1, Int(distanceMeters / Self.metersPerMinute))
        }
        etaText = "\(minutes) min"
    }

    private func bearing(from: CLLocationCoordinate2D, to: CLLocationCoordinate2D) -> Double {
        let lat1 = from.latitude * .pi / 180
        let lat2 = to.latitude * .pi / 180
        let dLon = (to.longitude - from.longitude) * .pi / 180

        let y = sin(dLon) * cos(lat2)
        let x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        return atan2(y, x) * 180 / .pi
    }

    deinit {
        stopAnimation()
    }
}
