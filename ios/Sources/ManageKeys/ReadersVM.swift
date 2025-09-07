import Foundation

@MainActor
final class ReadersVM: ObservableObject
{
    @Published var readers: [Reader] = []
    @Published var errorMessage: String?

    private var streamTask: Task<Void, Never>?

    func start(siteId: UUID)
    {
        let repo = AppComposition.repos.readerRepo
        streamTask = Task
        {
            if let snap = try? await repo.listBySite(siteId) { self.readers = snap }
            for await _ in repo.changes() { self.readers = (try? await repo.listBySite(siteId)) ?? [] }
        }
    }

    func stop()
    {
        streamTask?.cancel()
        streamTask = nil
    }

    func addReader(readerIdString: String, to siteId: UUID)
    {
        let trimmed = readerIdString.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let rid = UUID(uuidString: trimmed) else
        {
            errorMessage = "Invalid Reader UUID"
            return
        }

        Task
        {
            do
            {
                try await AppComposition.repos.readerRepo.upsert(Reader(readerId: rid, siteId: siteId))
                errorMessage = nil
            }
            catch let RepoError.foreignKeyViolation(msg)
            {
                errorMessage = msg
            }
            catch
            {
                errorMessage = "Failed to add reader: \(error)"
            }
        }
    }

    func updateReader(originalId: UUID, newIdString: String, siteId: UUID)
    {
        let trimmed = newIdString.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let newId = UUID(uuidString: trimmed) else
        {
            errorMessage = "Invalid Reader UUID"
            return
        }
        if newId == originalId { errorMessage = nil; return }

        Task
        {
            do
            {
                if let existing = try await AppComposition.repos.readerRepo.get(readerId: newId, siteId: siteId),
                   existing.readerId == newId
                {
                    errorMessage = "Reader with that UUID already exists"
                    return
                }

                try await AppComposition.repos.readerRepo.delete(readerId: originalId, siteId: siteId)
                try await AppComposition.repos.readerRepo.upsert(Reader(readerId: newId, siteId: siteId))
                errorMessage = nil
            }
            catch
            {
                errorMessage = "Failed to update reader: \(error)"
            }
        }
    }

    func deleteReader(_ readerId: UUID, siteId: UUID)
    {
        Task
        {
            do { try await AppComposition.repos.readerRepo.delete(readerId: readerId, siteId: siteId) }
            catch { errorMessage = "Failed to delete reader: \(error)" }
        }
    }
}
