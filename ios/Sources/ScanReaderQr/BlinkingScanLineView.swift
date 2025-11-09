import SwiftUI

struct BlinkingScanLineView : View
{
    @State private var opacityValue: Double = 1.0
    
    var body: some View
    {
        VStack(spacing: 0)
        {
            Spacer()
            
            GeometryReader
            {
                geometry in
                
                let side = min(geometry.size.width, geometry.size.height) * 0.7
                
                ZStack
                {
                    Rectangle()
                        .strokeBorder(Color.white.opacity(0.9), lineWidth: 2.0)
                        .frame(width: side, height: side)
                    
                    Rectangle()
                        .fill(Color.red)
                        .frame(width: side, height: 2.0)
                        .opacity(opacityValue)
                        .animation(
                            .easeInOut(duration: 1.5)
                            .repeatForever(autoreverses: true),
                            value: opacityValue
                        )
                        .onAppear
                        {
                            opacityValue = 0.2
                        }
                        .drawingGroup()
                }
                .frame(
                    maxWidth: .infinity,
                    maxHeight: .infinity,
                    alignment: .center
                )
            }
            .allowsHitTesting(false)
        }
    }
}
