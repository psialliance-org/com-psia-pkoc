import Foundation
import SwiftUI

@MainActor
protocol ProximityIslandClient : AnyObject
{
    func update(label: String?, phase: IslandPhase)
    func markStale(after seconds: TimeInterval)
}

enum IslandPhase
{
    case approaching(Double)
    case ready
    case granted
    case denied
}

@MainActor
final class LegacyIslandClient : ProximityIslandClient
{
    func update(label: String?, phase: IslandPhase)
    {
    }

    func markStale(after seconds: TimeInterval)
    {
    }
}
