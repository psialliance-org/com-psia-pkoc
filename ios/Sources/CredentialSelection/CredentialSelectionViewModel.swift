import Foundation

@MainActor
final class CredentialSelectionViewModel: ObservableObject
{
    // MARK: - State

    enum LoadState { case loading, loaded, error(String) }

    @Published var loadState: LoadState = .loading
    @Published var credentials: [SentryCredential] = []
    @Published var checkedIndices: Set<Int> = []
    @Published var showLoginSheet = false

    var organizationName: String = ""

    var anyChecked: Bool { !checkedIndices.isEmpty }

    // MARK: - Load

    func load()
    {
        loadState = .loading

        Task
        {
            do
            {
                let response = try await CredentialService.shared.getCredentials(filter: .sameKey)
                let emailCreds = response.credentials.filter
                {
                    if case .email = $0.identity?.identityCase { return true }
                    return false
                }
                credentials    = emailCreds
                checkedIndices = Set(emailCreds.indices)
                loadState      = .loaded
            }
            catch
            {
                // Show empty list so the user can add a new email
                credentials    = []
                checkedIndices = []
                loadState      = .loaded
            }
        }
    }

    // MARK: - Selection

    func toggle(index: Int)
    {
        if checkedIndices.contains(index)
        {
            checkedIndices.remove(index)
        }
        else
        {
            checkedIndices.insert(index)
        }
    }

    // MARK: - Label helpers

    /// Returns the display label for a credential, appending a short key suffix for duplicates.
    func label(for index: Int) -> String
    {
        let cred  = credentials[index]
        let email = cred.identity?.email ?? ""
        let hasDuplicate = credentials.filter { $0.identity?.email == email }.count > 1
        if hasDuplicate
        {
            let hex = cred.credentialHex
            let suffix = hex.suffix(6)
            return "\(email)  (…\(suffix))"
        }
        return email
    }

    // MARK: - Approve

    /// Returns the selected credentials (for passing to ContentView).
    func selectedCredentials() -> [SentryCredential]
    {
        checkedIndices.sorted().compactMap
        {
            index in index < credentials.count ? credentials[index] : nil
        }
    }

    /// Persists the selected credential hex IDs.
    func saveSelectedCredentials()
    {
        let hexIds = selectedCredentials().map { $0.credentialHex }
        CredentialStore.save(hexIds: hexIds)
    }
}
