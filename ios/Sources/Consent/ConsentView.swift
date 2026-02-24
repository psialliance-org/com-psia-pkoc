import SwiftUI

/// Deeplink entry point: shows org details and asks for consent.
/// Mirrors Android's ConsentActivity.
struct ConsentView: View
{
    var inviteCode: String
    /// Called when the user taps Proceed; receives the invite code, org name, and org ID.
    var onProceed: ((_ inviteCode: String, _ orgName: String, _ orgId: String) -> Void)?
    var onCancel: (() -> Void)?

    @StateObject private var vm: ConsentViewModel

    init(inviteCode: String,
         onProceed: ((_ inviteCode: String, _ orgName: String, _ orgId: String) -> Void)? = nil,
         onCancel: (() -> Void)? = nil)
    {
        self.inviteCode = inviteCode
        self.onProceed  = onProceed
        self.onCancel   = onCancel
        _vm = StateObject(wrappedValue: ConsentViewModel(inviteCode: inviteCode))
    }

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
                    if let org = vm.organization
                    {
                        ScrollView
                        {
                            VStack(spacing: 0)
                            {
                                orgContent(org: org)
                                    .padding(.horizontal, 28)
                                    .padding(.vertical, 24)
                            }
                        }
                    }

                case .error(let msg):
                    VStack(spacing: 16)
                    {
                        Text(msg).foregroundColor(.red).multilineTextAlignment(.center)
                        Button("Retry") { vm.load() }
                    }
                    .padding()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }

            bottomButtons
        }
        .onAppear { vm.load() }
    }

    // MARK: - Content

    private func orgContent(org: SentryOrganization) -> some View
    {
        VStack(alignment: .center, spacing: 20)
        {
            Image(uiImage: UIImage(named: ProductImages.PSIA_Logo_Typographic)!)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(height: 64)

            Text("\(org.name) needs your permission to use smartphone access")
                .font(.headline)
                .multilineTextAlignment(.center)

            Divider()

            Text("To use Sentry Interactive Mobile Access at buildings managed by \(org.name) you need to agree to sharing the following data.")
                .font(.footnote)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)

            VStack(alignment: .leading, spacing: 12)
            {
                dataRow(icon: "at", label: "Email Address")
                dataRow(icon: "iphone", label: "Phone Number")
                dataRow(icon: "qrcode", label: "Unique Identifiers")
            }

            Text("Consenting doesn't automatically mean you'll have access")
                .font(.caption)
                .foregroundColor(.secondary)
                .italic()
                .multilineTextAlignment(.center)

            orgCard(org: org)
        }
    }

    private func dataRow(icon: String, label: String) -> some View
    {
        HStack(spacing: 12)
        {
            Image(systemName: icon)
                .frame(width: 28)
                .foregroundColor(.secondary)
            Text(label)
                .font(.body)
        }
    }

    private func orgCard(org: SentryOrganization) -> some View
    {
        HStack(alignment: .top, spacing: 12)
        {
            Image(systemName: "building.2")
                .font(.title2)
                .foregroundColor(.secondary)

            VStack(alignment: .leading, spacing: 4)
            {
                Text(org.name).bold()
                if !org.contactAddress.isEmpty
                {
                    Text(org.contactAddress).font(.caption).foregroundColor(.secondary)
                }
                if !org.contactPhone.isEmpty
                {
                    Text(org.contactPhone).font(.caption).foregroundColor(.secondary)
                }
                Text(org.contactEmail).font(.caption).foregroundColor(.secondary)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.gray.opacity(0.3)))
    }

    // MARK: - Bottom Buttons

    private var bottomButtons: some View
    {
        VStack(spacing: 8)
        {
            Button("Proceed")
            {
                guard let org = vm.organization else { return }
                onProceed?(inviteCode, org.name, org.organizationId)
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.accentColor.opacity(vm.loadState == .loading ? 0.4 : 1))
            .foregroundColor(.white)
            .cornerRadius(8)
            .disabled(vm.organization == nil)

            Button("Cancel")
            {
                onCancel?()
            }
            .frame(maxWidth: .infinity)
            .padding()
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.accentColor))
            .foregroundColor(.accentColor)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 16)
        .background(Color(.systemBackground).shadow(radius: 1))
    }
}
