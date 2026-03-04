import Foundation

/// Singleton wrapper around the VerificationService gRPC-Web RPCs.
final class VerificationService
{
    static let shared = VerificationService()

    private let servicePath =
        "/com.sentryinteractive.opencredential.verification.v1alpha.VerificationService"

    private let client = GrpcWebClient.shared

    private init() {}

    func startEmailVerification(
        email: String,
        credential: Data,
        credentialType: SentryCredentialType,
        attestationDocument: String
    ) async throws -> SentryStartEmailVerificationResponse
    {
        let body = encodeStartEmailVerificationRequest(
            email: email,
            credential: credential,
            credentialType: credentialType,
            attestationDocument: attestationDocument
        )
        let responseData = try await client.call(
            servicePath: servicePath,
            method: "StartEmailVerification",
            body: body
        )
        let msgData = try client.parseDataFrame(responseData)
        return decodeStartEmailVerificationResponse(msgData)
    }

    func completeEmailVerification(token: String, code: String) async throws
    {
        let body = encodeCompleteEmailVerificationRequest(token: token, code: code)
        _ = try await client.call(
            servicePath: servicePath,
            method: "CompleteEmailVerification",
            body: body
        )
    }
}
