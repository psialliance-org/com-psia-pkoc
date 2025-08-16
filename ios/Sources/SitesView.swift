import SwiftUI

private struct SiteRow: View
{
    let site: Site

    var body: some View
    {
        VStack(alignment: .leading, spacing: 6)
        {
            Text(site.siteId.uuidString)
                .font(.system(.caption, design: .monospaced))
            Text(site.publicKeyHex)
                .font(.system(.footnote, design: .monospaced))
                .foregroundColor(.secondary)
                .lineLimit(nil)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .contentShape(Rectangle())
    }
}

struct SitesView: View
{
    @StateObject private var vm = SitesVM()

    @State private var siteIdInput = ""
    @State private var pkHexInput  = ""

    @State private var isEditingExisting = false
    @State private var editingSiteId: UUID?

    @State private var baselineSiteId = ""
    @State private var baselinePkHex  = ""

    private let longPressDuration: Double = 0.5

    private var isFormDirty: Bool
    {
        siteIdInput != baselineSiteId || pkHexInput != baselinePkHex
    }

    private var formTitle: String
    {
        isEditingExisting ? "Edit Site" : "Add Site"
    }

    var body: some View
    {
        NavigationView
        {
            VStack(spacing: 12)
            {
                Text("Tap a site to view readers. Long-press a site to edit the identifier or key. Delete is in the edit form.")
                    .font(.footnote)
                    .foregroundColor(.secondary)
                    .padding(.horizontal)

                VStack(alignment: .leading, spacing: 8)
                {
                    Text(formTitle).font(.headline)

                    TextField("Site UUID (8-4-4-4-12)", text: $siteIdInput)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .font(.system(.caption, design: .monospaced))
                        .autocapitalization(.none)
                        .disableAutocorrection(true)

                    TextField("Public key (130 hex, starts with 04)", text: $pkHexInput)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .font(.system(.caption, design: .monospaced))
                        .autocapitalization(.none)
                        .disableAutocorrection(true)

                    HStack(spacing: 8)
                    {
                        Button(isEditingExisting ? "Update Site" : "Save Site")
                        {
                            Task
                            {
                                let ok = await vm.saveSite(editingOriginalId: editingSiteId,
                                                           siteIdString: siteIdInput,
                                                           publicKeyHex: pkHexInput)
                                if ok
                                {
                                    if isEditingExisting
                                    {
                                        baselineSiteId = siteIdInput
                                        baselinePkHex  = pkHexInput
                                        if let newId = UUID(uuidString: siteIdInput.trimmingCharacters(in: .whitespacesAndNewlines))
                                        {
                                            editingSiteId = newId
                                        }
                                    }
                                    else
                                    {
                                        resetToNew()
                                    }
                                }
                            }
                        }

                        if isFormDirty
                        {
                            Button("Cancel") { restoreBaseline() }
                        }

                        Spacer()

                        if isEditingExisting, let sid = editingSiteId
                        {
                            Button("Delete Site")
                            {
                                vm.deleteSite(sid)
                                resetToNew()
                            }
                            .foregroundColor(.red)
                        }
                    }
                }
                .padding(.horizontal)

                if let err = vm.errorMessage
                {
                    Text(err)
                        .foregroundColor(.red)
                        .font(.footnote)
                        .padding(.horizontal)
                }

                List
                {
                    ForEach(vm.sites, id: \.siteId)
                    { s in
                        NavigationLink(destination: ReadersView(site: s))
                        {
                            SiteRow(site: s)
                        }
                        .simultaneousGesture(
                            LongPressGesture(minimumDuration: longPressDuration)
                                .onEnded { _ in populateForEdit(site: s) }
                        )
                    }
                }
            }
            .navigationBarTitle("Sites", displayMode: .inline)
        }
        .onAppear { vm.start() }
        .onDisappear { vm.stop() }
    }

    private func populateForEdit(site: Site)
    {
        isEditingExisting = true
        editingSiteId = site.siteId
        siteIdInput = site.siteId.uuidString
        pkHexInput = site.publicKeyHex
        baselineSiteId = siteIdInput
        baselinePkHex = pkHexInput
        vm.errorMessage = nil
    }

    private func resetToNew()
    {
        isEditingExisting = false
        editingSiteId = nil
        siteIdInput = ""
        pkHexInput = ""
        baselineSiteId = ""
        baselinePkHex = ""
        vm.errorMessage = nil
    }

    private func restoreBaseline()
    {
        siteIdInput = baselineSiteId
        pkHexInput = baselinePkHex
        vm.errorMessage = nil
    }
}
