import Foundation

#if canImport(ActivityKit)
import ActivityKit
#endif
import SwiftUI

@available(iOS 16.2, *)
struct PKOCAttributes: ActivityAttributes
{
    struct ContentState: Codable, Hashable
    {
        enum Phase: String, Codable
        {
            case nearby, approaching, ready, granted, denied
        }

        var phase: Phase
        var readerLabel: String?
        var progress: Double
    }

    var site: String? = nil
}
