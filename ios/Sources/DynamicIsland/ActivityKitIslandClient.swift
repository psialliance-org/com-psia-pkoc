import Foundation
import ActivityKit
import SwiftUI

@available(iOS 16.2, *)
@MainActor
final class ActivityKitIslandClient : ProximityIslandClient
{
    private var activity : Activity<PKOCAttributes>?
    private var last = PKOCAttributes.ContentState(phase: .denied, readerLabel: nil, progress: 0)

    private var lastAlertAt : Date?
    private let minAlertInterval : TimeInterval = 10.0

    func update(label: String?, phase: IslandPhase)
    {
        let newState = translate(label: label, phase: phase)

        Task
        {
            guard let a = try? await resolveOrCreate(for: newState) else { return }

            let content = ActivityContent(state: newState, staleDate: nil)

            if shouldAlert(from: self.last, to: newState)
            {
                if #available(iOS 17.0, *)
                {
                    let alert = AlertConfiguration(
                        title: alertTitle(for: newState),
                        body:  alertBody(for: newState),
                        sound: .default
                    )
                    await a.update(content, alertConfiguration: alert)
                    self.lastAlertAt = Date()
                }
                else
                {
                    await a.update(content)
                }
            }
            else
            {
                await a.update(content)
            }

            self.last = newState
        }
    }

    func markStale(after seconds: TimeInterval)
    {
        Task
        {
            guard let a = activity else { return }
            let when = Date().addingTimeInterval(seconds)
            await a.update(ActivityContent(state: last, staleDate: when))
        }
    }

    private func resolveOrCreate(for state: PKOCAttributes.ContentState) async throws -> Activity<PKOCAttributes>
    {
        if let a = activity, a.activityState == .active
        {
            return a
        }

        if let a = Activity<PKOCAttributes>.activities.first(where: { $0.activityState == .active })
        {
            activity = a
            return a
        }

        guard ActivityAuthorizationInfo().areActivitiesEnabled, AppActive.isActive else
        {
            throw NSError(domain: "ActivityKitIslandClient", code: 1)
        }

        let attrs   = PKOCAttributes(site: nil)
        let content = ActivityContent(state: state, staleDate: nil)
        let a = try Activity<PKOCAttributes>.request(attributes: attrs, content: content, pushType: nil)

        activity = a
        return a
    }

    private func translate(label: String?, phase: IslandPhase) -> PKOCAttributes.ContentState
    {
        switch phase
        {
            case .approaching(let p):
                return .init(phase: .approaching, readerLabel: label ?? "Approach", progress: p)
            case .ready:
                return .init(phase: .ready,       readerLabel: label,                 progress: 120.0)
            case .granted:
                return .init(phase: .granted,     readerLabel: label,                 progress: 120.0)
            case .denied:
                return .init(phase: .denied,      readerLabel: label,                 progress: 120.0)
        }
    }

    private func shouldAlert(from old: PKOCAttributes.ContentState,
                             to new: PKOCAttributes.ContentState) -> Bool
    {
        guard #available(iOS 17.0, *) else { return false }

        if let t = lastAlertAt, Date().timeIntervalSince(t) < minAlertInterval
        {
            return false
        }

        let enteringApproach = (old.phase != .approaching) && (new.phase == .approaching)
        let enteringReady    = (old.phase != .ready)       && (new.phase == .ready)
        let outcomeShown     = (new.phase == .granted) || (new.phase == .denied)

        return enteringApproach || enteringReady || outcomeShown
    }

    private func alertTitle(for s: PKOCAttributes.ContentState) -> LocalizedStringResource
    {
        switch s.phase
        {
            case .approaching:
                return LocalizedStringResource("Approach the reader")
            case .ready:
                return LocalizedStringResource("Ready")
            case .granted:
                return LocalizedStringResource("Unlocked")
            case .denied:
                return LocalizedStringResource("Denied")
            default:
                return LocalizedStringResource("Readers nearby")
        }
    }

    private func alertBody(for s: PKOCAttributes.ContentState) -> LocalizedStringResource
    {
        switch s.phase
        {
            case .approaching:
                return LocalizedStringResource("Keep moving closer.")
            case .ready:
                return LocalizedStringResource("Hold near to connect.")
            case .granted:
                return LocalizedStringResource("Access granted.")
            case .denied:
                return LocalizedStringResource("Access denied.")
            default:
                return LocalizedStringResource("Tap to choose a reader.")
        }
    }
}
