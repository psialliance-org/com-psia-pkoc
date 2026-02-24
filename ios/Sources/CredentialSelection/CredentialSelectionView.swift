import SwiftUI

/// Shows email credentials from the server and lets the user approve which to share.
/// Mirrors Android's CredentialSelectionActivity.
struct CredentialSelectionView: View
{
    var organizationName: String = ""
    var organizationId: String = ""
    var inviteCode: String = ""
    /// Called when user approves; receives the selected credentials.
    var onApprove: (([SentryCredential]) -> Void)?

    @StateObject private var vm = CredentialSelectionViewModel()
    @State private var isSharing = false

    var body: some View
    {
        VStack(spacing: 0)
        {
            switch vm.loadState
            {
                case .loading:
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)

                case .loaded:
                    ScrollView
                    {
                        VStack(alignment: .leading, spacing: 0)
                        {
                            header

                            Divider().padding(.vertical, 8)

                            if vm.credentials.isEmpty
                            {
                                Text("No email credentials found.")
                                    .foregroundColor(.secondary)
                                    .padding(.vertical, 8)
                            }
                            else
                            {
                                credentialList
                            }

                            addNewEmailButton
                        }
                        .padding(.horizontal, 24)
                        .padding(.top, 24)
                    }

                case .error(let msg):
                    VStack(spacing: 16)
                    {
                        Text(msg).foregroundColor(.red)
                        Button("Retry") { vm.load() }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }

            bottomButtons
        }
        .sheet(isPresented: $vm.showLoginSheet)
        {
            LoginView(returnOnSuccess: true)
            {
                vm.showLoginSheet = false
                vm.load()
            }
        }
        .onAppear
        {
            vm.organizationName = organizationName
            vm.load()
        }
    }

    // MARK: - Subviews

    private var header: some View
    {
        VStack(spacing: 8)
        {
            Image(uiImage: UIImage(named: ProductImages.PSIA_Logo_Typographic)!)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(height: 56)

            if !organizationName.isEmpty
            {
                Text("\(organizationName) needs your permission to use smartphone access")
                    .font(.headline)
                    .multilineTextAlignment(.center)
                    .padding(.top, 8)
            }

            Text("Select the email address the organisation knows you by")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(.bottom, 8)
    }

    private var credentialList: some View
    {
        VStack(alignment: .leading, spacing: 0)
        {
            Divider()
            ForEach(vm.credentials.indices, id: \.self)
            { index in
                HStack
                {
                    Image(systemName: vm.checkedIndices.contains(index) ? "checkmark.square.fill" : "square")
                        .foregroundColor(.accentColor)
                        .font(.title3)
                    Text(vm.label(for: index))
                        .font(.body)
                    Spacer()
                }
                .contentShape(Rectangle())
                .onTapGesture { vm.toggle(index: index) }
                .padding(.vertical, 14)

                Divider()
            }
        }
    }

    private var addNewEmailButton: some View
    {
        Button
        {
            vm.showLoginSheet = true
        }
        label:
        {
            HStack(spacing: 8)
            {
                Image(systemName: "plus.circle.fill")
                Text("Add new email")
            }
            .font(.callout)
            .foregroundColor(.accentColor)
        }
        .padding(.top, 12)
    }

    private var bottomButtons: some View
    {
        VStack(spacing: 8)
        {
            Button("Approve")
            {
                isSharing = true
                let selected = vm.selectedCredentials()
                Task
                {
                    do
                    {
                        for cred in selected
                        {
                            if let identity = cred.identity
                            {
                                try await OrganizationService.shared.shareCredentialWithOrganization(
                                    organizationId: organizationId,
                                    identity: identity,
                                    inviteCode: inviteCode
                                )
                            }
                        }
                        vm.saveSelectedCredentials()
                        onApprove?(selected)
                    }
                    catch
                    {
                        isSharing = false
                    }
                }
            }
            .buttonStyle(ApproveButtonStyle())
            .disabled(!vm.anyChecked || isSharing)

            Button("Cancel")
            {
                // The parent RootView handles routing; just show no-context if cancelled
            }
            .foregroundColor(.accentColor)
            .padding()
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 16)
        .background(Color(.systemBackground).shadow(radius: 1))
    }
}

private struct ApproveButtonStyle: ButtonStyle
{
    func makeBody(configuration: Configuration) -> some View
    {
        configuration.label
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.accentColor.opacity(configuration.isPressed ? 0.8 : 1))
            .foregroundColor(.white)
            .cornerRadius(8)
    }
}
