import MapKit

enum DirectionsError: LocalizedError {
    case noRoute
    case calculationFailed(Error)

    var errorDescription: String? {
        switch self {
        case .noRoute: return "No route found"
        case .calculationFailed(let error): return "Route calculation failed: \(error.localizedDescription)"
        }
    }
}

final class DirectionsService {
    func getRoute(
        from source: CLLocationCoordinate2D,
        to destination: CLLocationCoordinate2D
    ) async throws -> MKRoute {
        let request = MKDirections.Request()
        request.source = MKMapItem(placemark: MKPlacemark(coordinate: source))
        request.destination = MKMapItem(placemark: MKPlacemark(coordinate: destination))
        request.transportType = .automobile

        do {
            let directions = MKDirections(request: request)
            let response = try await directions.calculate()
            guard let route = response.routes.first else {
                throw DirectionsError.noRoute
            }
            return route
        } catch let error as DirectionsError {
            throw error
        } catch {
            throw DirectionsError.calculationFailed(error)
        }
    }

    func getRoutePoints(from route: MKRoute) -> [CLLocationCoordinate2D] {
        var coordinates = [CLLocationCoordinate2D]()
        let pointCount = route.polyline.pointCount
        let points = route.polyline.points()
        for i in 0..<pointCount {
            coordinates.append(points[i].coordinate)
        }
        return coordinates
    }
}
