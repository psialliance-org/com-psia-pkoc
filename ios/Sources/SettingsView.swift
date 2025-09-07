import SwiftUI

struct SettingsView: View
{
    @State var _transmissionSelected : TransmissionType = .BLE
    @State var _selectedFlow : PKOC_ConnectionType = .Uncompressed
    @State var _autoDiscoverDevices: Bool = false
    @State var _enableRanging: Bool = false
    @State var _showDeviceIdentifier: Bool = false

    @State private var activationSlider = 0.0
    @State private var discoveryValue = -85
    @State private var activationValue = -60

    private let bounds = (-100)...(-40)
    private let step = 1

    var body: some View
    {
        Form
        {
            Section(header: Text("Transmission Settings"),
                    content:
            {
                Picker("Select a Transmission Mechanism", selection: $_transmissionSelected)
                {
                    Text("NFC").tag(TransmissionType.NFC)
                    Text("BLE").tag(TransmissionType.BLE)
                }
                .onChange(of: _transmissionSelected)
                {
                    newValue in UserDefaults.standard.set(newValue.rawValue, forKey: TransmissionTypeSelected)
                }
                .pickerStyle(.automatic)
            })

            if (_transmissionSelected == .BLE)
            {
                Section(header: Text("PKOC Flow Setting"),
                        content:
                {
                    Picker("PKOC Connection Type", selection: $_selectedFlow)
                    {
                        Text("Normal flow").tag(PKOC_ConnectionType.Uncompressed)
                        Text("ECHDE Perfect Secrecy").tag(PKOC_ConnectionType.ECDHE_Full)
                    }
                    .onChange(of: _selectedFlow)
                    {
                        newValue in UserDefaults.standard.set(newValue.rawValue, forKey: PKOC_TransmissionFlow)
                    }
                    .pickerStyle(.automatic)
                })

                Section(header: Text("Discovery and Connection Settings"),
                        content:
                {
                    Toggle(isOn: $_autoDiscoverDevices)
                    {
                        Text("Auto Discover Devices")
                    }
                    .onChange(of: _autoDiscoverDevices)
                    {
                        newValue in UserDefaults.standard.set(newValue, forKey: AutoDiscoverDevices)
                    }

                    Toggle(isOn: $_enableRanging)
                    {
                        Text("Enable Ranging")
                    }
                    .onChange(of: _enableRanging)
                    {
                        newValue in UserDefaults.standard.set(newValue, forKey: EnableRanging)
                    }

                    if (_enableRanging)
                    {
                        VStack(alignment: .leading, spacing: 10)
                        {
                            Text("Use the left handle to choose when readers appear (farther away). Use the right handle to choose when to connect (closer). Moving right means closer.")
                                .font(.footnote)
                                .foregroundColor(.secondary)
                                .fixedSize(horizontal: false, vertical: true)

                            RangeSlider(
                                lower: $discoveryValue,
                                upper: $activationValue,
                                bounds: bounds,
                                step: step
                            )
                            .onChange(of: discoveryValue)
                            {
                                v in
                                if activationValue < v { activationValue = v }
                                UserDefaults.standard.set(v, forKey: DiscoveryRangeValue)
                            }
                            .onChange(of: activationValue)
                            {
                                v in
                                if v < discoveryValue { discoveryValue = v }
                                activationSlider = sliderFromValue(v)
                                UserDefaults.standard.set(activationSlider, forKey: RangeValue)
                            }

                            HStack
                            {
                                descriptor(title: "Discovery", value: discoveryValue)
                                Spacer()
                                descriptor(title: "Activation", value: activationValue)
                            }

                            HStack
                            {
                                Stepper("", value: $discoveryValue, in: bounds, step: step)
                                    .labelsHidden()
                                    .onChange(of: discoveryValue)
                                    {
                                        v in
                                        if activationValue < v { activationValue = v }
                                        UserDefaults.standard.set(v, forKey: DiscoveryRangeValue)
                                    }

                                Spacer()

                                Stepper("", value: $activationValue, in: bounds, step: step)
                                    .labelsHidden()
                                    .onChange(of: activationValue)
                                    {
                                        v in
                                        if v < discoveryValue { discoveryValue = v }
                                        activationSlider = sliderFromValue(v)
                                        UserDefaults.standard.set(activationSlider, forKey: RangeValue)
                                    }
                            }
                        }
                        .padding(.top, 4)
                    }

                    Toggle(isOn: $_showDeviceIdentifier)
                    {
                        Text("Display Device Identifier")
                    }
                    .onChange(of: _showDeviceIdentifier)
                    {
                        newValue in UserDefaults.standard.set(newValue, forKey: DisplayMAC)
                    }
                })
            }
        }
        .onAppear
        {
            loadValues()
        }
        .navigationTitle("Settings")
    }

    private func descriptor(title: String, value: Int) -> some View
    {
        HStack(spacing: 6)
        {
            Text(title)
                .font(.body)
            Text(proximityWord(for: value))
                .font(.footnote)
                .foregroundColor(.secondary)
        }
    }

    private func proximityWord(for v: Int) -> String
    {
        let pct = Double(v - bounds.lowerBound) / Double(bounds.upperBound - bounds.lowerBound)

        if pct < 0.20 { return "very far" }
        if pct < 0.40 { return "far" }
        if pct < 0.60 { return "near" }
        if pct < 0.80 { return "close" }
        return "very close"
    }

    private func loadValues()
    {
        let txRaw = UserDefaults.standard.object(forKey: TransmissionTypeSelected) as? Int
        _transmissionSelected = TransmissionType(rawValue: txRaw ?? TransmissionType.BLE.rawValue) ?? .BLE

        let flowRaw = UserDefaults.standard.object(forKey: PKOC_TransmissionFlow) as? Int
        _selectedFlow = PKOC_ConnectionType(rawValue: flowRaw ?? _selectedFlow.rawValue) ?? _selectedFlow

        _autoDiscoverDevices = UserDefaults.standard.object(forKey: AutoDiscoverDevices) as? Bool ?? _autoDiscoverDevices
        _enableRanging = UserDefaults.standard.object(forKey: EnableRanging) as? Bool ?? _enableRanging

        if let saved = UserDefaults.standard.object(forKey: DiscoveryRangeValue) as? Int
        {
            discoveryValue = saved
        }
        else
        {
            discoveryValue = -85
            UserDefaults.standard.set(discoveryValue, forKey: DiscoveryRangeValue)
        }

        activationSlider = UserDefaults.standard.object(forKey: RangeValue) as? Double ?? sliderFromValue(-60)
        activationValue = valueFromSlider(activationSlider)

        discoveryValue  = min(max(discoveryValue, bounds.lowerBound), bounds.upperBound)
        activationValue = min(max(activationValue, bounds.lowerBound), bounds.upperBound)

        if activationValue < discoveryValue { activationValue = discoveryValue }
    }

    private func valueFromSlider(_ slider: Double) -> Int
    {
        Int(slider * -5.0 - 35.0)
    }

    private func sliderFromValue(_ value: Int) -> Double
    {
        (Double(value) + 35.0) / -5.0
    }
}
