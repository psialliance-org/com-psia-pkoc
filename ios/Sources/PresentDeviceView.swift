import Foundation
import SwiftUI
import CoreBluetooth
import CryptoKit

struct PresentDeviceView : View
{
    @State private var isDoorOpen : Bool = false
    
    var body: some View
    {
        VStack(spacing: 16)
        {
            Spacer()
            
            Image(systemName: isDoorOpen ? "door.left.hand.open" : "door.left.hand.closed")
                .resizable()
                .scaledToFit()
                .frame(width: 96, height: 96)
                .foregroundColor(.primary)

            Text("Please present device to reader")
                .font(.system(size: 22, weight: .semibold))
                .foregroundColor(.secondary)
            
            Spacer()
        }
        .padding()
        .onChange(of: isDoorOpen)
        {
            _ in let light = UIImpactFeedbackGenerator(style: .light)
            let medium = UIImpactFeedbackGenerator(style: .medium)
            let heavy = UIImpactFeedbackGenerator(style: .heavy)
            
            light.prepare()
            light.impactOccurred()
            
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2)
            {
                medium.prepare()
                medium.impactOccurred()
            }
            
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.4)
            {
                heavy.prepare()
                heavy.impactOccurred()
            }
        }
    }
}
