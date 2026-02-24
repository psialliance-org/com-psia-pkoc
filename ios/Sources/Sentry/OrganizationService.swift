import Foundation

/// Singleton wrapper around the OrganizationService gRPC-Web RPCs.
final class OrganizationService
{
    static let shared = OrganizationService()

    private let servicePath =
        "/com.sentryinteractive.opencredential.organization.v1alpha.OrganizationService"

    private let client = GrpcWebClient.shared

    private init() {}

    func getOrganizationByInviteCode(_ inviteCode: String) async throws -> SentryOrganization
    {
        let body = encodeGetOrganizationByInviteCodeRequest(inviteCode: inviteCode)
        let responseData = try await client.call(
            servicePath: servicePath,
            method: "GetOrganizationByInviteCode",
            body: body
        )
        let msgData = try client.parseDataFrame(responseData)
        return decodeSentryOrganization(msgData)
    }

    func shareCredentialWithOrganization(
        organizationId: String,
        identity: SentryIdentity,
        inviteCode: String
    ) async throws
    {
        let body = encodeShareCredentialWithOrganizationRequest(
            organizationId: organizationId,
            identity: identity,
            inviteCode: inviteCode
        )
        // Returns google.protobuf.Empty — just fire the call
        _ = try await client.call(
            servicePath: servicePath,
            method: "ShareCredentialWithOrganization",
            body: body
        )
    }
}
