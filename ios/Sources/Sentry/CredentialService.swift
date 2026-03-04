import Foundation

/// Singleton wrapper around the CredentialService gRPC-Web RPCs.
final class CredentialService
{
    static let shared = CredentialService()

    private let servicePath =
        "/com.sentryinteractive.opencredential.credential.v1alpha.CredentialService"

    private let client = GrpcWebClient.shared

    private init() {}

    func getCredentials(filter: SentryCredentialFilter = .sameKey) async throws -> SentryGetCredentialsResponse
    {
        let body = encodeGetCredentialsRequest(filter: filter)
        let responseData = try await client.call(
            servicePath: servicePath,
            method: "GetCredentials",
            body: body
        )
        let msgData = try client.parseDataFrame(responseData)
        return decodeGetCredentialsResponse(msgData)
    }
}
