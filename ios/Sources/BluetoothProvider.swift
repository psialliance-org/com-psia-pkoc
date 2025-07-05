import SwiftUI
import CoreBluetooth
import Toast

class BluetoothProvider : NSObject, CBCentralManagerDelegate, CBPeripheralDelegate, ObservableObject
{
    @Published var discoveredPeripherals = [ListModel]()
    @Published var isScanning = false
    @Published var connectedPeripheral : CBPeripheral?
    
    var centralManager : CBCentralManager!
    var discoveredPeripheralSet = Set<CBPeripheral>()
    var timer : Timer?
    var readCharacteristic : CBCharacteristic?
    var writeCharacteristic : CBCharacteristic?
    var timeoutWorkItem : DispatchWorkItem?
    
    var _flowModel : FlowModel?
    
    override init()
    {
        super.init()
        centralManager = CBCentralManager(delegate: self, queue: nil)
    }

    func startScan()
    {
        if centralManager.state == .poweredOn
        {
            isScanning = true
            discoveredPeripherals.removeAll()
            discoveredPeripheralSet.removeAll()
            objectWillChange.send()
            
            var ServiceUUIDS = [CBUUID]()
            ServiceUUIDS.append(ServiceUUID)
            ServiceUUIDS.append(ServiceLegacyUUID)
            centralManager.scanForPeripherals(withServices: ServiceUUIDS)

            timer = Timer.scheduledTimer(withTimeInterval: 2.0, repeats: true)
            {
                [weak self] timer in
                self?.centralManager.stopScan()
                self?.centralManager.scanForPeripherals(withServices: ServiceUUIDS)
            }
        }
    }

    func stopScan()
    {
        isScanning = false
        timer?.invalidate()
        centralManager.stopScan()
    }

    func centralManagerDidUpdateState(_ central: CBCentralManager)
    {
        switch central.state
        {
            case .unknown:
                stopScan()
            case .resetting:
                stopScan()
            case .unsupported:
                stopScan()
            case .unauthorized:
                stopScan()
            case .poweredOff:
                stopScan()
            case .poweredOn:
                if let autoDiscoverDevices: Bool = UserDefaults.standard.bool(forKey: AutoDiscoverDevices) as Bool?
                {
                    if (autoDiscoverDevices)
                    {
                        if (!isScanning)
                        {
                            startScan()
                        }
                    }
                }
            @unknown default:
                print("central.state is unknown")
        }
    }

    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber)
    {
        if !discoveredPeripheralSet.contains(peripheral)
        {
            var progress = RSSI.doubleValue
            progress += 120
            
            if (progress >= 120)
            {
                progress = 50
            }
            
            var name = peripheral.name
            let advertisedName = advertisementData["kCBAdvDataLocalName"] as? String
            
            if (advertisedName != nil)
            {
                name = advertisedName
            }
            
            if (name == nil)
            {
                name = "Reader"
            }
            
            var progressTint = Color(hex: 0x9CC3C9)
            
            if let enableRanging: Bool = UserDefaults.standard.bool(forKey: EnableRanging) as Bool?
            {
                if (enableRanging)
                {
                    progressTint = Color.red
                }
            }
            
            discoveredPeripherals.append(ListModel(
                isBusy: false,
                name: name!,
                peripheral: peripheral,
                progress: progress,
                iconName: "lock.fill",
                iconTint: Color.gray,
                progressTint: progressTint,
                lastSeen: Date()
                ))
            
            discoveredPeripheralSet.insert(peripheral)
            objectWillChange.send()
        }
        else
        {
            if let index = discoveredPeripherals.firstIndex(where: { $0.peripheral == peripheral })
            {
                var progress = RSSI.doubleValue
                progress += 120

                discoveredPeripherals[index].lastSeen = Date()

                if (progress < 120)
                {
                    discoveredPeripherals[index].progress = progress
                }
                
                objectWillChange.send()
            }
        }
                
        if let enableRanging: Bool = UserDefaults.standard.bool(forKey: EnableRanging) as Bool?
        {
            if (enableRanging)
            {
                if (RSSI.doubleValue < 0)
                {
                    if var ranging: Double = UserDefaults.standard.double(forKey: RangeValue) as Double?
                    {
                        ranging *= -5
                        ranging -= 35
                        
                        if (RSSI.doubleValue >= ranging)
                        {
                            connectPKOCReader(PKOCperipheral: peripheral)

                            if let index = discoveredPeripherals.firstIndex(where: { $0.peripheral == peripheral })
                            {
                                discoveredPeripherals[index].progressTint = Color(hex: 0x9CC3C9)
                                objectWillChange.send()
                            }
                        }
                    }
                }
            }
        }
        
        if (discoveredPeripherals.count > 0)
        {
            for i in 0...discoveredPeripherals.count - 1
            {
                if (discoveredPeripherals[i].lastSeen.timeIntervalSinceNow < -15)
                {
                    discoveredPeripherals.remove(at: i);
                    objectWillChange.send()
                    return
                }
            }
        }
    }
    
    func connectPKOCReader(PKOCperipheral: CBPeripheral!)
    {
        print("Enter connectPKOCReader")
        
        stopScan()
        
        if (connectedPeripheral != nil)
        {
            return
        }
    
        timeoutWorkItem?.cancel()
        
        if let index = discoveredPeripherals.firstIndex(where: { $0.peripheral == PKOCperipheral })
        {
            discoveredPeripherals[index].isBusy = true
            objectWillChange.send()
        }

        var _selectedFlow = PKOC_ConnectionType.Uncompressed
        if let selectedFlow: Int = UserDefaults.standard.integer(forKey: PKOC_TransmissionFlow) as Int?
        {
            let isSelectedFlow = PKOC_ConnectionType(rawValue: selectedFlow)
            if (isSelectedFlow != nil)
            {
                _selectedFlow = isSelectedFlow!
            }
        }

        _flowModel = FlowModel(connectionType: _selectedFlow)
        
        self.connectedPeripheral = PKOCperipheral
        self.centralManager?.connect(PKOCperipheral)
        
        self.timeoutWorkItem = DispatchWorkItem(block:
        {
            if (self.connectedPeripheral != nil)
            {
                self.disconnectPeripheral(PKOCperipheral: self.connectedPeripheral!)
                
                if let index = self.discoveredPeripherals.firstIndex(where: { $0.peripheral == self.connectedPeripheral })
                {
                    if (self.discoveredPeripherals.count <= index)
                    {
                        return
                    }
                    
                    self.discoveredPeripherals[index].iconName = "lock.fill"
                    self.discoveredPeripherals[index].iconTint = Color.gray
                    self.objectWillChange.send()
                }
                
                Toast.text("Interaction with the reader has timed out", config: .init(direction: .bottom)).show()
            }
        })
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 6, execute: timeoutWorkItem!)
    }
    
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral)
    {
        print("Enter centralManager didConnect")
        
        discoverServices(PKOCperipheral: self.connectedPeripheral!)
    }
    
    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?)
    {
        print("Failed to connect \(String(describing: error))")
    }
    
    func disconnectPeripheral(PKOCperipheral: CBPeripheral)
    {
        if let index = discoveredPeripherals.firstIndex(where: { $0.peripheral == PKOCperipheral })
        {            
            if (discoveredPeripherals.count <= index)
            {
                return
            }

            discoveredPeripherals[index].isBusy = false
            objectWillChange.send()
        }
    
        self.connectedPeripheral = nil
        centralManager?.cancelPeripheralConnection(PKOCperipheral)
    }
    
    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?)
    {
        if let error = error
        {
            print("Error occured during Disconnection \(error)")
            return
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 4)
        {
            if let autoDiscoverDevices: Bool = UserDefaults.standard.bool(forKey: AutoDiscoverDevices) as Bool?
            {
                if (autoDiscoverDevices)
                {
                    if (!self.isScanning)
                    {
                        self.startScan()
                    }
                }
            }
        }
        
        print("Successfully disconnected from PKOC Peripheral")
    }
    
    func discoverServices(PKOCperipheral: CBPeripheral)
    {
        print("Enter discoverServices")
        PKOCperipheral.delegate = self
        PKOCperipheral.discoverServices([ServiceUUID, ServiceLegacyUUID])
    }
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?)
    {
        guard let PKOCservice = peripheral.services else
        {
            print("Failed to Discover Services/No Services found.")
            return
        }
        
        print("Services: \(PKOCservice)")
        discoverCharacteristics(PKOCperipheral: peripheral)
    }
    
    func discoverCharacteristics(PKOCperipheral: CBPeripheral)
    {
        print("Going into discover Characteristics Function")
        let PKOCservices = PKOCperipheral.services
        
        if (PKOCservices != nil)
        {
            let i = PKOCservices!.count
            if (i <= 0)
            {
                print("Failed to discover PKOC service; exiting handshake")
                disconnectPeripheral(PKOCperipheral: PKOCperipheral)
                return;
            }
        }
        
        PKOCperipheral.discoverCharacteristics([WriteUUID, ReadUUID, ConfigUUID], for: (PKOCservices?[0])!)
    }
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?)
    {
        print("Enter peripheral didDiscoverCharacteristicsFor")
        
        guard let PKOCcharacteristics = service.characteristics else
        {
            print("Unable to Find any characteristics within the service.")
            print("error: \(String(describing: error?.localizedDescription))")
            return
        }
        
        for characteristic in PKOCcharacteristics
        {
            if(characteristic.uuid == ReadUUID)
            {
                self.readCharacteristic = characteristic
                subscribeToNotifications(PKOCperipheral: peripheral, characteristic: readCharacteristic!)
            }
            else if(characteristic.uuid == WriteUUID)
            {
                self.writeCharacteristic = characteristic
            }
            else
            {
                print("Not a required Characteristic, where did you come from \(characteristic.uuid)")
            }
        }
    }

    func subscribeToNotifications(PKOCperipheral: CBPeripheral, characteristic: CBCharacteristic)
    {
        PKOCperipheral.setNotifyValue(true, for: characteristic)
    }
        
    func peripheral(_ peripheral: CBPeripheral, didUpdateNotificationStateFor characteristic: CBCharacteristic, error: Error?)
    {
        if let error = error
        {
            print("An error occured when trying to subscribe to a characteristics. \(error) \(characteristic.uuid)")
            return
        }
        
        print("Subscribed to notifications on \(characteristic.uuid)")
    }
    
    func handleNotifications(peripheral: CBPeripheral, notificationData: Data)
    {
        print("Notification Data: \(notificationData)")
        let byteArray = [UInt8](notificationData)
        print("Byte Array: \(byteArray)")
        
        if (byteArray.isEmpty)
        {
            return
        }
        
        let packets = TLVProvider.getValues(data: byteArray)
        
        for packet in packets
        {
            switch packet.type
            {
                case .Response:
                    var iconName = "lock.fill"
                    var iconTint = Color.red
                    
                    if (packet.data[0] == ReaderUnlockStatus.AccessGranted.rawValue)
                    {
                        iconName = "lock.open.fill"
                        iconTint = Color(hex: 0x9CC3C9)
                    }
                    
                    if let index = discoveredPeripherals.firstIndex(where: { $0.peripheral == peripheral })
                    {
                        discoveredPeripherals[index].iconName = iconName
                        discoveredPeripherals[index].iconTint = iconTint
                        objectWillChange.send()
                        
                        DispatchQueue.main.asyncAfter(deadline: .now() + 4)
                        {
                            if (self.discoveredPeripherals.count <= index)
                            {
                                return
                            }
                            
                            self.discoveredPeripherals[index].iconName = "lock.fill"
                            self.discoveredPeripherals[index].iconTint = Color.gray
                            self.objectWillChange.send()
                        }
                    }
                    
                    disconnectPeripheral(PKOCperipheral: peripheral)
                    break
                    
                case .ProtocolVersion:
                    _flowModel?.reader.ProtocolVersion = packet.data
                    break
                    
                case .CompressedEphemeralPublicKey:
                    _flowModel?.reader.ReaderEphemeralPublicKey = packet.data
                    break

                case .ReaderLocationIdentifier:
                    _flowModel?.reader.ReaderIdentifier = packet.data
                    break
                    
                case .SiteIdenifier:
                    _flowModel?.reader.SiteIdentifier = packet.data;
                    break
                    
                case .DigitalSignature:
                    print(Data(packet.data).hexadecimal())
                    
                    if (_flowModel == nil || _flowModel?.site == nil)
                    {
                        disconnectPeripheral(PKOCperipheral: peripheral)
                        return
                    }
                                                            
                    _flowModel!.readerValid = CryptoProvider.verifySignedMessage(
                        message: generateSignatureMessage(),
                        signature: packet.data,
                        publicKey: _flowModel!.site!.PublicKey)
                    
                    if (!_flowModel!.readerValid!)
                    {
                        disconnectPeripheral(PKOCperipheral: peripheral)
                        return
                    }
                    
                    if (_flowModel?.sharedSecret != nil)
                    {
                        completeTransaction(peripheral: peripheral)
                    }
                    
                    break
                                        
                default:
                    print("Notification data decoded: \(notificationData.base64EncodedString())")
                    break
            }
        }
        
        let toVerify = _flowModel?.reader
        if (toVerify?.ReaderIdentifier != nil && toVerify?.SiteIdentifier != nil && toVerify?.ReaderEphemeralPublicKey != nil)
        {
            var siteToFind : SiteModel? = nil;
            
            for reader in KnownReaders
            {
                if (reader.ReaderIdentifier == _flowModel?.reader.ReaderIdentifier && reader.SiteIdentifier == _flowModel?.reader.SiteIdentifier)
                {
                    for site in KnownSites
                    {
                        if (site.SiteIdentifier == _flowModel?.reader.SiteIdentifier)
                        {
                            siteToFind = site
                        }
                    }
                }
            }
            
            if (siteToFind == nil)
            {
                disconnectPeripheral(PKOCperipheral: peripheral)
                return
            }

            _flowModel?.site = siteToFind
        }
            
        if (_flowModel?.connectionType == PKOC_ConnectionType.ECDHE_Full)
        {
            if (_flowModel?.reader == nil)
            {
                _flowModel?.status = ReaderUnlockStatus.Unrecognized
                disconnectPeripheral(PKOCperipheral: peripheral)
                return
            }
            
            if (_flowModel?.reader.ReaderIdentifier == nil)
            {
                disconnectPeripheral(PKOCperipheral: peripheral)
                return
            }
            
            if (_flowModel?.transientKeyPair == nil)
            {
                _flowModel?.transientKeyPair = CryptoProvider.generateTransientKey()

                let publicKeySecKey = CryptoProvider.fromCompressedPublicKey(compressedPublicKey: _flowModel!.reader.ReaderEphemeralPublicKey!)
                
                if (publicKeySecKey == nil)
                {
                    disconnectPeripheral(PKOCperipheral: peripheral)
                    return
                }
                
                _flowModel?.sharedSecret = [UInt8] (CryptoProvider.createSharedSecret(
                    privateKey: _flowModel!.transientKeyPair!,
                    publicKey: publicKeySecKey!)!)
                
                let transientPublicKey = SecKeyCopyPublicKey(_flowModel!.transientKeyPair!)
                
                var error: Unmanaged<CFError>?
                let transientPublicKeyData = SecKeyCopyExternalRepresentation(transientPublicKey!, &error)
                var transientPublicKeyBytes = [UInt8](repeating: 0, count: 65)
                CFDataGetBytes(transientPublicKeyData, CFRange(location: CFIndex(), length: CFIndex(integerLiteral: 65)), &transientPublicKeyBytes)
                let transientPublicKeyTLV = TLVProvider.getTLV(type: BLE_PacketType.UncompressedEphemeralPublicKey, data:transientPublicKeyBytes)
                let echdePacket = transientPublicKeyTLV
                
                peripheral.writeValue(Data(_: echdePacket), for: self.writeCharacteristic!, type: CBCharacteristicWriteType.withoutResponse)
            }
            
            if ((_flowModel?.readerValid) != nil && _flowModel!.readerValid! && _flowModel?.sharedSecret != nil)
            {
                completeTransaction(peripheral: peripheral)
            }
            
            return
        }
        
        if (_flowModel?.reader.ReaderEphemeralPublicKey != nil)
        {
            completeTransaction(peripheral: peripheral)
        }
    }
    
    func generateSignatureMessage() -> [UInt8]
    {
        if (_flowModel?.connectionType == PKOC_ConnectionType.ECDHE_Full)
        {
            let siteIdentifier = _flowModel!.reader.SiteIdentifier!
            let readerIdentifier = _flowModel!.reader.ReaderIdentifier!
            
            let transientPublicKey = SecKeyCopyPublicKey(_flowModel!.transientKeyPair!)
            var error: Unmanaged<CFError>?
            let transientPublicKeyData = SecKeyCopyExternalRepresentation(transientPublicKey!, &error)
            var transientPublicKeyBytes = [UInt8](repeating: 0, count: 65)
            CFDataGetBytes(transientPublicKeyData, CFRange(location: CFIndex(), length: CFIndex(integerLiteral: 65)), &transientPublicKeyBytes)
            let deviceX = Array(transientPublicKeyBytes[1...32])

            let readerX = Array(_flowModel!.reader.ReaderEphemeralPublicKey![1...])

            return siteIdentifier + readerIdentifier + deviceX + readerX
        }
        
        return _flowModel!.reader.ReaderEphemeralPublicKey!
    }
    
    func completeTransaction(peripheral : CBPeripheral)
    {        
        let signature = CryptoProvider.signNonceWithPrivateKey(nonce: Data(generateSignatureMessage()))
        print("Signature: " + Data(signature).hexadecimal())
        let publicKey = CryptoProvider.exportPublicKey().x963Representation
        print("public key: " + Data(publicKey).hexadecimal())
        
        var secondsStamp = UInt32(Date().timeIntervalSince1970)
        if let secondsStampHistorical: Int = UserDefaults.standard.integer(forKey: PKOC_CreationTime) as Int?
        {
            secondsStamp = UInt32(secondsStampHistorical)
        }

        let secondsArray = withUnsafeBytes(of: secondsStamp.bigEndian, Array.init)

        let publicKeyTLV = TLVProvider.getTLV(type: BLE_PacketType.PublicKey, data: [UInt8](publicKey))
        let signatureTLV = TLVProvider.getTLV(type: BLE_PacketType.DigitalSignature, data: signature)
        let timeTLV = TLVProvider.getTLV(type: BLE_PacketType.LastUpdateTime, data: secondsArray)
        var packet = publicKeyTLV + signatureTLV + timeTLV
        
        if (self._flowModel?.connectionType == PKOC_ConnectionType.ECDHE_Full)
        {
            let encryptedData = CryptoProvider.getAES256(secretKey: self._flowModel!.sharedSecret!, data: packet, counter: _flowModel!.counter)
            _flowModel?.counter += 1
            
            if (encryptedData == nil)
            {
                disconnectPeripheral(PKOCperipheral: peripheral)
                return
            }
            
            packet = TLVProvider.getTLV(type: BLE_PacketType.EncryptedDataFollows, data: encryptedData!)
        }
        
        print(Data(packet).hexadecimal())
        
        peripheral.writeValue(Data(_: packet), for: self.writeCharacteristic!, type: CBCharacteristicWriteType.withoutResponse)
    }
    
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?)
    {
        guard let data = characteristic.value else
        {
            print("ERROR: No value read from characteristic")
            return
        }
        print("Data received: \(data.base64EncodedData()) \(data.base64EncodedString()), size: \(data.count)")
        handleNotifications(peripheral: peripheral, notificationData: characteristic.value!)
    }
    
    func peripheral(_ peripheral: CBPeripheral, didWriteValueFor characteristic: CBCharacteristic, error: Error?)
    {
        guard let data = characteristic.value else
        {
            print("\(String(describing: error))");
            return
        }
        print("Data sent: \(data) \(data.base64EncodedData())")
    }
}
