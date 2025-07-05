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
        if let transmissionType : Int = UserDefaults.standard.integer(forKey: TransmissionTypeSelected) as Int?
        {
            let transmissionTypeSelected = TransmissionType(rawValue: transmissionType)
            if (transmissionTypeSelected != nil)
            {
                _transmissionSelected = transmissionTypeSelected!
            }
        }

        if let selectedFlow : Int = UserDefaults.standard.integer(forKey: PKOC_TransmissionFlow) as Int?
        {
            let isSelectedFlow = PKOC_ConnectionType(rawValue: selectedFlow)
            if (isSelectedFlow != nil)
            {
                _selectedFlow = isSelectedFlow!
            }
        }

        if let autoDiscoverDevices : Bool = UserDefaults.standard.bool(forKey: AutoDiscoverDevices) as Bool?
        {
            _autoDiscoverDevices = autoDiscoverDevices
        }

        if let enableRanging : Bool = UserDefaults.standard.bool(forKey: EnableRanging) as Bool?
        {
            _enableRanging = enableRanging
            
            if let ranging : Double = UserDefaults.standard.double(forKey: RangeValue) as Double?
            {
                _ranging = ranging
            }
        }

        if let showDeviceIdentifier : Bool = UserDefaults.standard.bool(forKey: DisplayMAC) as Bool?
        {
            _showDeviceIdentifier = showDeviceIdentifier
        }
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
