import SwiftUI
import CoreBluetooth

struct ListModel
{
    var isBusy: Bool
    var name: String
    var peripheral: CBPeripheral
    var progress: Double
    var iconName: String
    var iconTint: Color
    var progressTint: Color
    var lastSeen: Date
}
