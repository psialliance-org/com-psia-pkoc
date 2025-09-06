import SwiftUI

struct SettingsView: View
{
    @State var _transmissionSelected : TransmissionType = .BLE
    @State var _selectedFlow : PKOC_ConnectionType = .Uncompressed
    @State var _autoDiscoverDevices: Bool = false
    @State var _enableRanging: Bool = false
    @State var _showDeviceIdentifier: Bool = false
    @State private var _ranging = 0.0
    @State private var _isEditingRange = false
    
    func loadValues()
    {
        let transmissionTypeRaw = UserDefaults.standard.object(forKey: TransmissionTypeSelected) as? Int
        _transmissionSelected = TransmissionType(rawValue: transmissionTypeRaw ?? TransmissionType.BLE.rawValue) ?? .BLE

        let flowRaw = UserDefaults.standard.object(forKey: PKOC_TransmissionFlow) as? Int
        _selectedFlow = PKOC_ConnectionType(rawValue: flowRaw ?? _selectedFlow.rawValue) ?? _selectedFlow

        _autoDiscoverDevices = UserDefaults.standard.object(forKey: AutoDiscoverDevices) as? Bool ?? _autoDiscoverDevices

        _enableRanging = UserDefaults.standard.object(forKey: EnableRanging) as? Bool ?? _enableRanging
        if _enableRanging
        {
            _ranging = UserDefaults.standard.object(forKey: RangeValue) as? Double ?? _ranging
        }

        _showDeviceIdentifier = UserDefaults.standard.object(forKey: DisplayMAC) as? Bool ?? _showDeviceIdentifier
    }
    
    var body: some View
    {
        Form
        {
            Section(header: Text("Transmission Settings"), content:
            {
                Picker("Select a Transmission Mechanism", selection: $_transmissionSelected)
                {
                    Text("NFC").tag(TransmissionType.NFC)
                    Text("BLE").tag(TransmissionType.BLE)
                }.onChange(of : _transmissionSelected)
                {
                    newValue in UserDefaults.standard.set(newValue.rawValue, forKey: TransmissionTypeSelected)
                }.pickerStyle(.automatic)
            })

            if (_transmissionSelected == TransmissionType.BLE)
            {
                Section(header: Text("PKOC Flow Setting"), content:
                {
                    Picker("PKOC Connection Type", selection: $_selectedFlow)
                    {
                        Text("Normal flow").tag(PKOC_ConnectionType.Uncompressed)
                        Text("ECHDE Perfect Secrecy").tag(PKOC_ConnectionType.ECDHE_Full)
                    }.onChange(of: _selectedFlow)
                    {
                        newValue in UserDefaults.standard.set(newValue.rawValue, forKey: PKOC_TransmissionFlow)
                    }.pickerStyle(.automatic)
                })
                
                Section(header: Text("Discovery and Connection Settings"), content:
                {
                    VStack
                    {
                        Toggle(isOn: $_autoDiscoverDevices)
                        {
                            Text("Auto Discover Devices")
                        }
                        .onChange(of: _autoDiscoverDevices)
                        {
                            newValue in UserDefaults.standard.set(newValue, forKey:AutoDiscoverDevices)
                        }
                        
                        Toggle(isOn: $_enableRanging)
                        {
                            Text("Enable Ranging")
                        }
                        .onChange(of: _enableRanging)
                        {
                            newValue in UserDefaults.standard.set(newValue, forKey:EnableRanging)
                        }
                        
                        if (_enableRanging)
                        {
                            Text("Unlock Range")
                            Slider(value: $_ranging, in: 0...10, step: 1)
                            {
                                Text("Ranging")
                            }
                        minimumValueLabel:
                            {
                                Text("near")
                            }
                        maximumValueLabel:
                            {
                                Text("far")
                            }
                        onEditingChanged:
                            {
                                newValue in UserDefaults.standard.set(_ranging, forKey:RangeValue)
                            }
                        }
                    }
                })
                Section(header: Text("Display Settings"), content:
                {
                    HStack
                    {
                        Toggle(isOn: $_showDeviceIdentifier)
                        {
                            Text("Display Device Identifiers")
                        }
                        .onChange(of: _showDeviceIdentifier)
                        {
                            newValue in UserDefaults.standard.set(newValue, forKey:DisplayMAC)
                        }
                    }
                })
            }
        }
        .onAppear()
        {
            loadValues()
        }
    }
}
