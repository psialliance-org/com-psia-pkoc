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
                .frame(width: 128, height: 128)
                .foregroundColor(Color(hex: 0x9CC3C9))

            Text("Please present device to reader to unlock.")
                .font(.system(size: 22, weight: .semibold))
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            
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
