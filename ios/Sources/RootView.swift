import SwiftUI

/// Top-level routing view.
/// Mirrors the Android LoginActivity routing logic.
struct RootView: View
{
    @StateObject private var vm = RootViewModel()

    var body: some View
    {
        Group
        {
            switch vm.state
            {
                case .loading:
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)

                case .noContext:
                    noContextView

                case .credentialSelection(let orgName, let inviteCode, let orgId):
                    CredentialSelectionView(
                        organizationName: orgName,
                        organizationId: orgId,
                        inviteCode: inviteCode,
                        onApprove:
                        { credentials in
                            vm.onCredentialSelectionApproved(credentials: credentials)
                        }
                    )

                case .main(let credentials):
                    ContentView(selectedCredentials: credentials)
            }
        }
        .sheet(item: $vm.pendingInviteCode)
        { inviteCode in
            if #available(iOS 15.0, *) {
                ConsentView(
                    inviteCode: inviteCode,
                    onProceed:
                        { code, orgName, orgId in
                            vm.onConsentProceeded(inviteCode: code, orgName: orgName, orgId: orgId)
                        },
                    onCancel:
                        {
                            vm.onConsentCancelled()
                        }
                )
            } else {
                // Fallback on earlier versions
            }
        }
        .onAppear { vm.start() }
        .onOpenURL { url in vm.handleUniversalLink(url: url) }
    }

    // MARK: - No Context

    private var noContextView: some View
    {
        VStack(spacing: 24)
        {
            Image(uiImage: UIImage(named: ProductImages.PSIA_Logo_Typographic)!)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(height: 80)

            Text("No organisation in context.")
                .font(.headline)

            Text("Get an invite link from your organisation to start with Sentry Interactive.")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
        }
    }
}

// MARK: - String: Identifiable for sheet(item:)

extension String: @retroactive Identifiable
{
    public var id: String { self }
}
