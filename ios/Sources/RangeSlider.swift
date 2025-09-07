import SwiftUI

struct RangeSlider: View
{
    @Binding var lower: Int
    @Binding var upper: Int

    let bounds: ClosedRange<Int>
    let step: Int

    var trackHeight: CGFloat = 6
    var thumbDiameter: CGFloat = 28

    var body: some View
    {
        GeometryReader
        {
            geo in
            let width  = geo.size.width
            let height = geo.size.height

            let lo = min(max(lower, bounds.lowerBound), bounds.upperBound)
            let hi = min(max(upper, bounds.lowerBound), bounds.upperBound)

            let xFor =
            {
                (value: Int) -> CGFloat in
                let v = min(max(value, bounds.lowerBound), bounds.upperBound)
                let ratio = (Double(v - bounds.lowerBound) / Double(bounds.upperBound - bounds.lowerBound))
                return CGFloat(ratio) * (width - thumbDiameter) + thumbDiameter / 2.0
            }

            let lx = xFor(lo)
            let ux = xFor(hi)

            ZStack
            {
                Capsule()
                    .fill(Color.secondary.opacity(0.25))
                    .frame(height: trackHeight)
                    .position(x: width / 2.0, y: height / 2.0)

                Capsule()
                    .fill(Color.accentColor)
                    .frame(width: max(abs(ux - lx), 2), height: trackHeight)
                    .position(x: (lx + ux) / 2.0, y: height / 2.0)

                thumb(atX: lx, atY: height / 2.0)
                    .highPriorityGesture(dragGesture(isLower: true, width: width))

                thumb(atX: ux, atY: height / 2.0)
                    .highPriorityGesture(dragGesture(isLower: false, width: width))
            }
            .contentShape(Rectangle())
        }
        .frame(height: max(thumbDiameter + 12, 48))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Range")
        .accessibilityValue("\(lower) to \(upper)")
    }

    @ViewBuilder
    private func thumb(atX x: CGFloat, atY y: CGFloat) -> some View
    {
        Circle()
            .fill(Color(.systemBackground).opacity(0.9))
            .overlay(Circle().stroke(Color.primary.opacity(0.2), lineWidth: 1))
            .shadow(radius: 1, y: 1)
            .frame(width: thumbDiameter, height: thumbDiameter)
            .position(x: x, y: y)
    }

    private func dragGesture(isLower: Bool, width: CGFloat) -> some Gesture
    {
        DragGesture(minimumDistance: 0)
            .onChanged
            {
                value in
                let minX = thumbDiameter / 2.0
                let maxX = width - thumbDiameter / 2.0
                let px = min(max(value.location.x, minX), maxX)

                let ratio = (px - minX) / (maxX - minX)
                let raw = Double(bounds.lowerBound) + ratio * Double(bounds.upperBound - bounds.lowerBound)
                let snapped = snap(Int(round(raw)))

                if isLower
                {
                    lower = min(snapped, upper)
                }
                else
                {
                    upper = max(snapped, lower)
                }
            }
    }

    private func snap(_ v: Int) -> Int
    {
        let clamped = min(max(v, bounds.lowerBound), bounds.upperBound)
        let offset  = clamped - bounds.lowerBound
        let snapped = Int(round(Double(offset) / Double(step))) * step + bounds.lowerBound
        return min(max(snapped, bounds.lowerBound), bounds.upperBound)
    }
}
