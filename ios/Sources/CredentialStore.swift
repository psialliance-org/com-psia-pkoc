import Foundation

/// UserDefaults wrapper for persisting selected credential hex IDs.
/// Mirrors Android's CredentialStore.java.
enum CredentialStore
{
    private static let key = "pkoc_selected_credential_ids"

    static func save(hexIds: [String])
    {
        UserDefaults.standard.set(hexIds.joined(separator: ","), forKey: key)
    }

    static func load() -> Set<String>
    {
        guard let stored = UserDefaults.standard.string(forKey: key), !stored.isEmpty else
        {
            return []
        }
        return Set(stored.split(separator: ",").map { String($0).trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty })
    }

    static func clear()
    {
        UserDefaults.standard.removeObject(forKey: key)
    }
}
