import Foundation
import CryptoKit

/// Manages top-level routing: loading → noContext | main(credentials)
@MainActor
final class RootViewModel: ObservableObject
{
    enum AppState
    {
        case loading
        case noContext
        case credentialSelection(orgName: String, inviteCode: String, orgId: String)
        case main(credentials: [SentryCredential])
    }

    @Published var state: AppState = .loading
    @Published var pendingInviteCode: String? = nil  // set by universal link

    func start()
    {
        loadKeysAndRoute()
    }

    // MARK: - Key Loading + Routing

    private func loadKeysAndRoute()
    {
        KeyStore.load
        { [weak self] result in
            guard let self else { return }
            switch result
            {
                case .success(let keys):
                    do
                    {
                        try CryptoProvider.loadKeys(
                            privateKey: P256.Signing.PrivateKey(rawRepresentation: keys.privateKey),
                            publicKey:  P256.Signing.PublicKey(rawRepresentation: keys.publicKey)
                        )
                    }
                    catch
                    {
                        self.generateAndStoreKeys()
                    }
                case .failure:
                    self.generateAndStoreKeys()
            }
            Task { await self.checkSavedCredentials() }
        }
    }

    private func generateAndStoreKeys()
    {
        CryptoProvider.generateAndSendPublishKey
        { _ in
            let secondsStamp = Int(Date().timeIntervalSince1970)
            UserDefaults.standard.set(secondsStamp, forKey: PKOC_CreationTime)
            KeyStore.save(
                keyData: KeyData(
                    publicKey: CryptoProvider.exportPublicKey().rawRepresentation,
                    privateKey: CryptoProvider.exportPrivateKey().rawRepresentation
                )
            ) { _ in }
        }
    }

    // MARK: - Credential Check

    private func checkSavedCredentials() async
    {
        let savedIds = CredentialStore.load()
        guard !savedIds.isEmpty else
        {
            state = .noContext
            return
        }

        do
        {
            let response = try await CredentialService.shared.getCredentials(filter: .sameKey)
            let matched = response.credentials.filter
            { cred in
                guard case .email = cred.identity?.identityCase else { return false }
                return savedIds.contains(cred.credentialHex)
            }

            if matched.isEmpty
            {
                CredentialStore.clear()
                state = .noContext
            }
            else
            {
                state = .main(credentials: matched)
            }
        }
        catch
        {
            // Network failure: show no-context rather than crash
            state = .noContext
        }
    }

    // MARK: - Universal Link

    func handleUniversalLink(url: URL)
    {
        guard let inviteCode = extractInviteCode(from: url) else { return }
        pendingInviteCode = inviteCode
    }

    private func extractInviteCode(from url: URL) -> String?
    {
        let parts = url.pathComponents
        // Expected: /share/{inviteCode}
        guard parts.count >= 3, parts[1] == "share" else { return nil }
        return parts[2]
    }

    // MARK: - Navigation callbacks

    func onConsentProceeded(inviteCode: String, orgName: String, orgId: String)
    {
        pendingInviteCode = nil
        state = .credentialSelection(orgName: orgName, inviteCode: inviteCode, orgId: orgId)
    }

    func onConsentCancelled()
    {
        pendingInviteCode = nil
    }

    func onCredentialSelectionApproved(credentials: [SentryCredential])
    {
        state = .main(credentials: credentials)
    }
}
