import Foundation
import Combine
import SwiftUI

@MainActor
final class PKOCProximityController
{
    static let shared = PKOCProximityController()

    private var bag = Set<AnyCancellable>()

    private var island: ProximityIslandClient =
    {
        #if canImport(ActivityKit)
        if #available(iOS 16.2, *)
        {
            return ActivityKitIslandClient()
        }
        #endif
        return LegacyIslandClient()
    }()

    private let uiHz: Double = 4.0
    private let debounceReadyMs: Int = 300
    private var readySince: Date?

    func start(discoveries: AnyPublisher<[ListModel], Never>,
               selectedID: AnyPublisher<UUID?, Never>,
               isScanning: AnyPublisher<Bool, Never>)
    {
        bag.removeAll()

        let tick = Timer.publish(every: 1.0 / uiHz, on: .main, in: .common).autoconnect()

        Publishers.CombineLatest3(discoveries, selectedID, isScanning)
            .combineLatest(tick.map { _ in () }.prepend(()))
            .map { combined, _ in combined }
            .sink { [weak self] (models, selected, scanning) in
                self?.reevaluate(models: models, selectedID: selected, isScanning: scanning)
            }
            .store(in: &bag)

        _ = AppActive.observeDidBecomeActive
        {
            [weak self] in self?.readySince = nil
        }
    }

    func onNFCOutcome(granted: Bool, readerLabel: String?)
    {
        island.update(label: readerLabel, phase: granted ? .granted : .denied)
        island.markStale(after: 6.0)
    }

    func onBLEOutcome(status: ReaderUnlockStatus, displayName: String?)
    {
        let label = displayName ?? "Reader"
        switch status
        {
            case .AccessGranted, .CompletedTransaction:
                island.update(label: label, phase: .granted)
                island.markStale(after: 6.0)

            case .AccessDenied:
                island.update(label: label, phase: .denied)
                island.markStale(after: 6.0)

            default:
                return
        }
    }

    private func reevaluate(models: [ListModel], selectedID: UUID?, isScanning: Bool)
    {
        guard gatingOn() else
        {
            island.markStale(after: 0.5)
            return
        }

        if (isScanning == false) && (selectedID == nil)
        {
            island.markStale(after: 12.0)
            return
        }

        let t1 = t1Discovery()
        let t2 = t2Activation()

        let target: ListModel? =
            (selectedID.flatMap { id in models.first(where: { $0.peripheral.identifier == id }) })
            ?? models
                .filter { rssiFromProgress($0.progress) >= t1 }
                .max(by: { rssiFromProgress($0.progress) < rssiFromProgress($1.progress) })

        guard let model = target else
        {
            island.markStale(after: 12.0)
            return
        }

        let rssi  = rssiFromProgress(model.progress)
        let label = model.name

        if rssi >= t2
        {
            if readySince == nil { readySince = Date() }
            if let since = readySince,
               Date().timeIntervalSince(since) >= Double(debounceReadyMs) / 1000.0
            {
                island.update(label: label, phase: .ready)
                return
            }
        }
        else
        {
            readySince = nil
        }

        let prog = progress(rssi: rssi, t1: t1, t2: t2)
        island.update(label: label, phase: .approaching(prog))
    }

    private func gatingOn() -> Bool
    {
        let ud = UserDefaults.standard
        let ranging = ud.bool(forKey: "EnableRanging")
        let auto    = ud.bool(forKey: "AutoDiscoverDevices")
        return ranging && auto
    }

    private func t1Discovery() -> Int
    {
        let ud = UserDefaults.standard
        if ud.object(forKey: DiscoveryRangeValue) == nil
        {
            ud.set(-75, forKey: DiscoveryRangeValue)
        }
        return ud.integer(forKey: DiscoveryRangeValue)
    }

    private func t2Activation() -> Int
    {
        let slider = UserDefaults.standard.double(forKey: "RangeValue")
        let fallback = (-60 + 35) / -5
        let effective = (slider == 0.0) ? Double(fallback) : slider
        return Int(effective * -5.0 - 35.0)
    }

    private func rssiFromProgress(_ p: Double) -> Int
    {
        return Int(p.rounded()) - 120
    }

    private func progress(rssi: Int, t1: Int, t2: Int) -> Double
    {
        let denom = Double(t2 - t1)
        guard denom > 0 else { return 0 }
        let fraction = Double(rssi - t1) / denom
        let clamped  = min(max(fraction, 0.0), 1.0)
        return round(clamped * 120.0)
    }
}
