import Foundation

enum AppComposition
{
    static let repos: PKOCRepositories =
    {
        let r = try! PKOCRepositories.make()
        Task.detached
        {
            try? await RepoTools.seedIfEmpty(
                siteRepo: r.siteRepo,
                readerRepo: r.readerRepo,
                seedSites: [Seed.siteA],
                seedReaders: [Seed.readerA]
            )
        }
        return r
    }()

    static func bootstrap()
    {
        _ = repos
    }
}
