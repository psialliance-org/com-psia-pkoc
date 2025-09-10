import WidgetKit
import SwiftUI
import ActivityKit

@available(iOSApplicationExtension 16.2, *)
@main
struct PKOCIslandWidget : Widget
{
    var body : some WidgetConfiguration
    {
        ActivityConfiguration(for: PKOCAttributes.self)
        { context in
            PKOCLockExpandedView(context: context)
        }
        dynamicIsland:
        { context in
            DynamicIsland
            {
                DynamicIslandExpandedRegion(.leading)
                {
                    PKOCIslandIcon(context: context)
                }

                DynamicIslandExpandedRegion(.center)
                {
                    PKOCIslandProgress(context: context)
                }

                DynamicIslandExpandedRegion(.trailing)
                {
                    PKOCIslandStatus(context: context)
                }
            }
            compactLeading:
            {
                PKOCIslandIcon(context: context)
            }
            compactTrailing:
            {
                PKOCCompactText(context: context)
            }
            minimal:
            {
                PKOCIslandIcon(context: context)
            }
        }
    }
}
