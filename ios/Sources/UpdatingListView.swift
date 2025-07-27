import Foundation
import SwiftUI
import CoreBluetooth
import CryptoKit

struct UpdatingListView : View
{
    @ObservedObject private var _bluetoothScanner = BluetoothProvider()
        
    var body: some View
    {
        HStack
        {
            if(!UserDefaults.standard.bool(forKey: AutoDiscoverDevices))
            {
                Button(action:
                {
                    if _bluetoothScanner.isScanning
                    {
                        _bluetoothScanner.stopScan()
                    }
                    else
                    {
                        _bluetoothScanner.startScan()
                    }
                })
                {
                    if _bluetoothScanner.isScanning
                    {
                        Text("STOP DISCOVERY")
                    }
                    else
                    {
                        Text("DISCOVER NEW DEVICE")
                    }
                }
                .padding()
                .background(_bluetoothScanner.isScanning ? Color.red : Color(hex: 0xCBCFD3))
                .foregroundColor(Color.white)
                .cornerRadius(5.0)
                Spacer()
            }
        }.padding(.all, 16)
        
        List(_bluetoothScanner.discoveredPeripherals, id: \.peripheral.identifier)
        {
            discoveredPeripheral in
            Button(action:
            {
                _bluetoothScanner.connectPKOCReader(PKOCperipheral: discoveredPeripheral.peripheral)
            })
            {
                HStack
                {
                    VStack(alignment: .leading)
                    {
                        Text(discoveredPeripheral.name)
                            .font(.title2)
                            .bold()

                        if let showDeviceIdentifier: Bool = UserDefaults.standard.bool(forKey: DisplayMAC) as Bool?
                        {
                            if (showDeviceIdentifier)
                            {
                                Text(discoveredPeripheral.peripheral.identifier.uuidString)
                                    .font(.headline)
                                    .foregroundColor(.gray)
                            }
                        }
                        
                        if #available(iOS 16.0, *)
                        {
                            ProgressView(value: discoveredPeripheral.progress, total: 120)
                                .progressViewStyle(.linear)
                                .tint(discoveredPeripheral.progressTint)
                        }
                        else
                        {
                            ProgressView(value: discoveredPeripheral.progress, total: 120)
                                .progressViewStyle(.linear)
                                .accentColor(discoveredPeripheral.progressTint)
                        }

                    }
                    
                    if (discoveredPeripheral.isBusy)
                    {
                        ProgressView().padding(17)
                    }
                    else
                    {
                        Image(systemName: discoveredPeripheral.iconName)
                            .font(.system(size: 48, weight: .light))
                            .foregroundColor(discoveredPeripheral.iconTint)
                    }
                }
            }
        }
        .listStyle(.inset)
        .onAppear()
        {
            if let autoDiscoverDevices: Bool = UserDefaults.standard.bool(forKey: AutoDiscoverDevices) as Bool?
            {
                if (autoDiscoverDevices)
                {
                    if (!_bluetoothScanner.isScanning)
                    {
                        _bluetoothScanner.startScan()
                    }
                }
            }
        }
    }
}
