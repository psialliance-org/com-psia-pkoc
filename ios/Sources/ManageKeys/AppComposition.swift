import Foundation

enum AppComposition
{
    static let repos: PKOCRepositories =
    {
        return try! PKOCRepositories.make()
    }()

    static func bootstrap()
    {
        _ = repos
    }
}
