import SwiftUI

struct ReadersView: View
{
    let site: Site
    @StateObject private var vm = ReadersVM()

    @State private var readerIdInput = ""

    @State private var isEditingExisting = false
    @State private var originalReaderId: UUID?

    @State private var baselineReaderId = ""

    private let longPressDuration: Double = 0.5

    private var isFormDirty: Bool
    {
        readerIdInput != baselineReaderId
    }

    private var formTitle: String
    {
        isEditingExisting ? "Edit Reader" : "Add Reader"
    }

    var body: some View
    {
        VStack(spacing: 12)
        {
            Text("Long-press a reader to edit its identifier. Delete is in the edit form.")
                .font(.footnote)
                .foregroundColor(.secondary)
                .padding(.horizontal)

            VStack(alignment: .leading, spacing: 8)
            {
                Text("Site").font(.headline)
                Text(site.siteId.uuidString)
                    .font(.system(.caption, design: .monospaced))

                Divider().padding(.vertical, 6)

                Text(formTitle).font(.headline)

                TextField("Reader UUID (8-4-4-4-12)", text: $readerIdInput)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .font(.system(.caption, design: .monospaced))
                    .autocapitalization(.none)
                    .disableAutocorrection(true)

                HStack(spacing: 8)
                {
                    Button(isEditingExisting ? "Update Reader" : "Save Reader")
                    {
                        if isEditingExisting, let orig = originalReaderId
                        {
                            vm.updateReader(originalId: orig, newIdString: readerIdInput, siteId: site.siteId)
                        }
                        else
                        {
                            vm.addReader(readerIdString: readerIdInput, to: site.siteId)
                        }
                        if !isEditingExisting { resetToNew() }
                    }

                    if isFormDirty
                    {
                        Button("Cancel") { restoreBaseline() }
                    }

                    Spacer()

                    if isEditingExisting, let rid = originalReaderId
                    {
                        Button("Delete Reader")
                        {
                            vm.deleteReader(rid, siteId: site.siteId)
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

            List(vm.readers, id: \.readerId)
            { r in
                Text(r.readerId.uuidString)
                    .font(.system(.caption, design: .monospaced))
                    .contentShape(Rectangle())
                    .simultaneousGesture(
                        LongPressGesture(minimumDuration: longPressDuration)
                            .onEnded { _ in populateForEdit(reader: r) }
                    )
            }
            .navigationBarTitle("Readers", displayMode: .inline)
        }
        .onAppear { vm.start(siteId: site.siteId) }
        .onDisappear { vm.stop() }
    }

    private func populateForEdit(reader: Reader)
    {
        isEditingExisting = true
        originalReaderId = reader.readerId
        readerIdInput = reader.readerId.uuidString
        baselineReaderId = readerIdInput
        vm.errorMessage = nil
    }

    private func resetToNew()
    {
        isEditingExisting = false
        originalReaderId = nil
        readerIdInput = ""
        baselineReaderId = ""
        vm.errorMessage = nil
    }

    private func restoreBaseline()
    {
        readerIdInput = baselineReaderId
        vm.errorMessage = nil
    }
}
