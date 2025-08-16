import Foundation

@MainActor
final class SitesVM: ObservableObject
{
    @Published var sites: [Site] = []
    @Published var errorMessage: String?

    private var streamTask: Task<Void, Never>?

    func start()
    {
        let repo = AppComposition.repos.siteRepo
        streamTask = Task
        {
            if let snap = try? await repo.list() { self.sites = snap }
            for await snapshot in repo.changes() { self.sites = snapshot }
        }
    }

    func stop()
    {
        streamTask?.cancel()
        streamTask = nil
    }

    func saveSite(editingOriginalId: UUID?, siteIdString: String, publicKeyHex: String) async -> Bool
    {
        let trimmedId = siteIdString.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedKey = publicKeyHex.trimmingCharacters(in: .whitespacesAndNewlines)

        guard let newId = UUID(uuidString: trimmedId) else
        {
            errorMessage = "Invalid Site UUID (8-4-4-4-12)"
            return false
        }
        guard Validator.isUncompressed65ByteHex(trimmedKey) else
        {
            errorMessage = "Invalid public key (130 hex, starts with 04)"
            return false
        }

        let siteRepo = AppComposition.repos.siteRepo
        let readerRepo = AppComposition.repos.readerRepo

        if let oldId = editingOriginalId, oldId != newId
        {
            do
            {
                if let _ = try await siteRepo.get(newId)
                {
                    errorMessage = "A site with that UUID already exists"
                    return false
                }

                try await siteRepo.upsert(Site(siteId: newId, publicKeyHex: trimmedKey))

                let oldReaders = try await readerRepo.listBySite(oldId)
                for r in oldReaders
                {
                    try await readerRepo.upsert(Reader(readerId: r.readerId, siteId: newId))
                }

                try await siteRepo.delete(oldId)

                errorMessage = nil
                return true
            }
            catch
            {
                errorMessage = "Failed to rename site: \(error)"
                return false
            }
        }
        else
        {
            do
            {
                try await siteRepo.upsert(Site(siteId: newId, publicKeyHex: trimmedKey))
                errorMessage = nil
                return true
            }
            catch
            {
                errorMessage = "Failed to save site: \(error)"
                return false
            }
        }
    }

    func deleteSite(_ id: UUID)
    {
        Task
        {
            do { try await AppComposition.repos.siteRepo.delete(id) }
            catch { errorMessage = "Failed to delete site: \(error)" }
        }
    }
}
