import Foundation

struct LoginUseCase {
    private let repository: RepositoryProtocol

    init(repository: RepositoryProtocol) {
        self.repository = repository
    }

    func execute(email: String, password: String) async throws {
        try await repository.login(email: email, password: password)
    }
}

struct RegisterUseCase {
    private let repository: RepositoryProtocol

    init(repository: RepositoryProtocol) {
        self.repository = repository
    }

    func execute(name: String, email: String, password: String) async throws -> String {
        try await repository.register(name: name, email: email, password: password)
    }
}

struct GoogleLoginUseCase {
    private let repository: RepositoryProtocol

    init(repository: RepositoryProtocol) {
        self.repository = repository
    }

    func execute(idToken: String) async throws {
        try await repository.loginWithGoogle(idToken: idToken)
    }
}

struct FacebookLoginUseCase {
    private let repository: RepositoryProtocol

    init(repository: RepositoryProtocol) {
        self.repository = repository
    }

    func execute(accessToken: String) async throws {
        try await repository.loginWithFacebook(accessToken: accessToken)
    }
}
