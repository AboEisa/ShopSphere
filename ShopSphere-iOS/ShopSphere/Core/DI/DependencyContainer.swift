import SwiftUI
import FirebaseAuth
import FirebaseFirestore

@Observable
final class DependencyContainer {
    let localStorage: LocalStorageService
    let networkService: NetworkService
    let dummyAPIService: DummyJSONAPIService
    let fakeStoreAPIService: FakeStoreAPIService
    let firestoreDataSource: FirestoreRemoteDataSource
    let directionsService: DirectionsService
    let repository: ShopRepository

    init() {
        localStorage = LocalStorageService()
        networkService = NetworkService()
        dummyAPIService = DummyJSONAPIService(networkService: networkService)
        fakeStoreAPIService = FakeStoreAPIService(networkService: networkService)
        firestoreDataSource = FirestoreRemoteDataSource(dummyAPIService: dummyAPIService)
        directionsService = DirectionsService()
        repository = ShopRepository(
            remoteDataSource: firestoreDataSource,
            localStorage: localStorage,
            firebaseAuth: Auth.auth()
        )
    }
}

// MARK: - Environment Key
private struct DependencyContainerKey: EnvironmentKey {
    static let defaultValue = DependencyContainer()
}

extension EnvironmentValues {
    var container: DependencyContainer {
        get { self[DependencyContainerKey.self] }
        set { self[DependencyContainerKey.self] = newValue }
    }
}
