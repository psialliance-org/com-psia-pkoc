import Foundation
import UIKit

@MainActor
enum AppActive
{
    @available(iOSApplicationExtension, unavailable)
    static var isActive: Bool
    {
        UIApplication.shared.applicationState == .active
    }

    @available(iOSApplicationExtension, unavailable)
    @discardableResult
    static func observeDidBecomeActive(_ handler: @escaping () -> Void) -> NSObjectProtocol
    {
        NotificationCenter.default.addObserver(
            forName: UIApplication.didBecomeActiveNotification,
            object: nil,
            queue: .main)
        { _ in handler() }
    }
}
