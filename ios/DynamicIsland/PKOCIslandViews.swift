import SwiftUI
import ActivityKit
import WidgetKit

@available(iOSApplicationExtension 16.2, *)
struct PKOCLockExpandedView : View
{
    let context : ActivityViewContext<PKOCAttributes>

    var body : some View
    {
        HStack(spacing: 12)
        {
            Image(systemName: leadingSymbol)
                .imageScale(.large)

            VStack(alignment: .leading, spacing: 6)
            {
                Text(title)
                    .font(.headline)
                    .lineLimit(1)

                if showsProgress
                {
                    ProgressView(value: context.state.progress, total: 120)
                        .progressViewStyle(.linear)
                        .tint(progressTint)
                }
            }

            Spacer(minLength: 8)

            Text(lockStatusText)
                .font(.subheadline)
                .lineLimit(1)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
    }

    private var phase : PKOCAttributes.ContentState.Phase { context.state.phase }
    private var showsProgress : Bool { phase == .approaching || phase == .ready }
    private var progressTint : Color { phase == .ready ? Color(hex: 0x9CC3C9) : .red }

    private var title : String
    {
        switch phase
        {
            case .nearby: return "Readers nearby"
            default:      return context.state.readerLabel ?? "Reader"
        }
    }

    private var leadingSymbol : String
    {
        switch phase
        {
            case .granted:     return "lock.open.fill"
            case .denied:      return "xmark.octagon.fill"
            case .ready:       return "lock.fill"
            case .nearby,
                 .approaching: return "lock.fill"
        }
    }

    private var lockStatusText : String
    {
        switch phase
        {
            case .nearby:      return "Nearby"
            case .approaching: return "Approaching"
            case .ready:       return "Ready"
            case .granted:     return "Unlocked"
            case .denied:      return "Denied"
        }
    }
}

@available(iOSApplicationExtension 16.2, *)
struct PKOCIslandIcon : View
{
    let context : ActivityViewContext<PKOCAttributes>

    var body : some View
    {
        Image(systemName: symbol)
            .symbolRenderingMode(.monochrome)
            .foregroundColor(.white)
            .imageScale(.large)
            .accessibilityHidden(true)
    }

    private var symbol : String
    {
        switch context.state.phase
        {
            case .granted:     return "lock.open.fill"
            case .denied:      return "xmark.octagon.fill"
            case .ready:       return "lock.fill"
            case .nearby,
                 .approaching: return "lock.fill"
        }
    }
}

@available(iOSApplicationExtension 16.2, *)
struct PKOCIslandStatus : View
{
    let context : ActivityViewContext<PKOCAttributes>

    var body : some View
    {
        Text(status)
            .font(.subheadline)
            .foregroundColor(.white.opacity(0.9))
            .lineLimit(1)
    }

    private var status : String
    {
        switch context.state.phase
        {
            case .nearby:      return "Nearby"
            case .approaching: return "Approaching"
            case .ready:       return "Ready"
            case .granted:     return "Unlocked"
            case .denied:      return "Denied"
        }
    }
}

@available(iOSApplicationExtension 16.2, *)
struct PKOCIslandProgress : View
{
    let context : ActivityViewContext<PKOCAttributes>

    var body : some View
    {
        if context.state.phase == .approaching || context.state.phase == .ready
        {
            VStack(alignment: .leading, spacing: 6)
            {
                Text(context.state.readerLabel ?? "Reader")
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.9))
                    .lineLimit(1)

                ProgressView(value: context.state.progress, total: 120)
                    .progressViewStyle(.linear)
                    .tint(context.state.phase == .ready ? Color(hex: 0x9CC3C9) : .red)
            }
        }
    }
}

@available(iOSApplicationExtension 16.2, *)
struct PKOCCompactText : View
{
    let context : ActivityViewContext<PKOCAttributes>

    var body: some View
    {
        Text(short)
            .font(.caption2)
            .foregroundColor(.white.opacity(0.95))
            .lineLimit(1)
    }

    private var short : String
    {
        switch context.state.phase
        {
            case .nearby:      return "Nearby"
            case .approaching: return "Approach"
            case .ready:       return "Ready"
            case .granted:     return "Unlocked"
            case .denied:      return "Denied"
        }
    }
}
