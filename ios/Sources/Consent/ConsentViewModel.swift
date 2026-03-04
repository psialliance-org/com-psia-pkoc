import Foundation

@MainActor
final class ConsentViewModel: ObservableObject
{
    enum LoadState: Equatable { case loading, loaded, error(String) }

    @Published var loadState: LoadState = .loading
    @Published var organization: SentryOrganization?

    private(set) var inviteCode: String

    init(inviteCode: String)
    {
        self.inviteCode = inviteCode
    }

    func load()
    {
        loadState = .loading
        Task
        {
            do
            {
                let org = try await OrganizationService.shared.getOrganizationByInviteCode(inviteCode)
                organization = org
                loadState    = .loaded
            }
            catch
            {
                loadState = .error("Failed to load organization details. Please try again.")
            }
        }
    }
}
