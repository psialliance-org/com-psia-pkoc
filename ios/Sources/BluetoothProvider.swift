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
            AppLog.info("Starting BLE scan", tag: "BLE")
            isScanning = true
            discoveredPeripherals.removeAll()
            discoveredPeripheralSet.removeAll()
            objectWillChange.send()
            
            let serviceUUIDs = [ServiceUUID, ServiceLegacyUUID]
            centralManager.scanForPeripherals(withServices: serviceUUIDs)

            timer = Timer.scheduledTimer(withTimeInterval: 2.0, repeats: true)
            { [weak self] _ in
                self?.centralManager.stopScan()
                self?.centralManager.scanForPeripherals(withServices: serviceUUIDs)
                AppLog.debug("Rescanning for peripherals", tag: "BLE")
            }
        }
    }

    func stopScan()
    {
        if isScanning { AppLog.info("Stopping BLE scan", tag: "BLE") }
        isScanning = false
        timer?.invalidate()
        centralManager.stopScan()
    }

    func centralManagerDidUpdateState(_ central: CBCentralManager)
    {
        switch central.state
        {
            case .unknown:
                AppLog.warn("Central state unknown", tag: "BLE")
                stopScan()
            case .resetting:
                AppLog.warn("Central state resetting", tag: "BLE")
                stopScan()
            case .unsupported:
                AppLog.error("BLE unsupported", tag: "BLE")
                stopScan()
            case .unauthorized:
                AppLog.error("BLE unauthorized", tag: "BLE")
                stopScan()
            case .poweredOff:
                AppLog.warn("BLE powered off", tag: "BLE")
                stopScan()
            case .poweredOn:
                AppLog.info("Central powered on", tag: "BLE")
                if UserDefaults.standard.bool(forKey: AutoDiscoverDevices)
                {
                    if (!isScanning)
                    {
                        startScan()
                    }
                }
            @unknown default:
                AppLog.error("Central state is unknown default", tag: "BLE")
        }
    }

    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber)
    {
        AppLog.debug("Discovered peripheral: \(peripheral.identifier) RSSI=\(RSSI)", tag: "BLE")

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
                            AppLog.info("Proximity threshold met, attempting connect", tag: "BLE")
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
                    AppLog.debug("Removing stale peripheral: \(discoveredPeripherals[i].name)", tag: "BLE")
                    discoveredPeripherals.remove(at: i);
                    objectWillChange.send()
                    return
                }
            }
        }
    }
    
    func connectPKOCReader(PKOCperipheral: CBPeripheral!)
    {
        AppLog.info("Enter connectPKOCReader", tag: "BLE", payload: PKOCperipheral.identifier.uuidString.data(using: .utf8))
        
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
                AppLog.error("Connection timed out", tag: "BLE")
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
        AppLog.info("Enter centralManager didConnect", tag: "BLE")
        discoverServices(PKOCperipheral: self.connectedPeripheral!)
    }
    
    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?)
    {
        AppLog.error("Failed to connect \(String(describing: error))", tag: "BLE")
    }
    
    func disconnectPeripheral(PKOCperipheral: CBPeripheral)
    {
        AppLog.info("Disconnecting from peripheral \(PKOCperipheral.identifier)", tag: "BLE")
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
            AppLog.error("Error occured during Disconnection \(error)", tag: "BLE")
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
        
        AppLog.info("Successfully disconnected from PKOC Peripheral", tag: "BLE")
    }
    
    func discoverServices(PKOCperipheral: CBPeripheral)
    {
        AppLog.info("Enter discoverServices", tag: "BLE")
        PKOCperipheral.delegate = self
        PKOCperipheral.discoverServices([ServiceUUID, ServiceLegacyUUID])
    }
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?)
    {
        guard let PKOCservice = peripheral.services else
        {
            AppLog.error("Failed to Discover Services/No Services found.", tag: "BLE")
            return
        }
        
        AppLog.info("Services: \(PKOCservice)", tag: "BLE")
        discoverCharacteristics(PKOCperipheral: peripheral)
    }
    
    func discoverCharacteristics(PKOCperipheral: CBPeripheral)
    {
        AppLog.info("Going into discover Characteristics Function", tag: "BLE")
        let PKOCservices = PKOCperipheral.services
        
        if (PKOCservices != nil)
        {
            let i = PKOCservices!.count
            if (i <= 0)
            {
                AppLog.error("Failed to discover PKOC service; exiting handshake", tag: "BLE")
                disconnectPeripheral(PKOCperipheral: PKOCperipheral)
                return;
            }
        }
        
        PKOCperipheral.discoverCharacteristics([WriteUUID, ReadUUID, ConfigUUID], for: (PKOCservices?[0])!)
    }
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?)
    {
        AppLog.info("Enter peripheral didDiscoverCharacteristicsFor", tag: "BLE")
        
        guard let PKOCcharacteristics = service.characteristics else
        {
            AppLog.error("Unable to Find any characteristics within the service. error: \(String(describing: error?.localizedDescription))", tag: "BLE")
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
                AppLog.debug("Not a required Characteristic: \(characteristic.uuid)", tag: "BLE")
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
            AppLog.error("An error occured when trying to subscribe to a characteristic. \(error) \(characteristic.uuid)", tag: "BLE")
            return
        }
        
        AppLog.info("Subscribed to notifications on \(characteristic.uuid)", tag: "BLE")
    }
    
    func loadSiteForVerification(from flowModel: FlowModel?) -> SiteModel?
    {
        guard
            let ctx = flowModel?.reader,
            let readerIdBytes: [UInt8] = ctx.ReaderIdentifier,
            let siteIdBytes:  [UInt8]  = ctx.SiteIdentifier,
            ctx.ReaderEphemeralPublicKey != nil,
            let readerId = UUID(bytes: readerIdBytes),
            let siteId   = UUID(bytes: siteIdBytes)
        else
        {
            return nil
        }

        var result: SiteModel?
        let sem = DispatchSemaphore(value: 0)

        Task.detached(priority: .userInitiated)
        {
            let repos = AppComposition.repos
            let readerOK = (try? await repos.readerRepo.get(readerId: readerId, siteId: siteId)) != nil
            if readerOK,
               let site = try? await repos.siteRepo.get(siteId),
               let pubKeyBytes: [UInt8] = site.publicKeyHex.hexStringToBytes()
            {
                result = SiteModel(SiteIdentifier: siteIdBytes, PublicKey: pubKeyBytes)
            }
            sem.signal()
        }

        sem.wait()
        return result
    }

    func handleNotifications(peripheral: CBPeripheral, notificationData: Data)
    {
        AppLog.debug("Notification Data", tag: "BLE", payload: notificationData)
        let byteArray = [UInt8](notificationData)
        AppLog.debug("Received BLE Byte Array: \(Data(byteArray).toHexString())", tag: "BLE")
        
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
                
                    if (packet.data[0] == ReaderUnlockStatus.CompletedTransaction.rawValue)
                    {
                        iconName = "lock.open.fill"
                        iconTint = Color.yellow
                        Toast.text("TLV Success - Access decision unknown", config: .init(direction: .bottom)).show()
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
                    return
                    
                case .ProtocolVersion:
                    _flowModel?.reader.ProtocolVersion = packet.data
                    AppLog.debug("ProtocolVersion received", tag: "BLE", payload: Data(packet.data))
                    break
                    
                case .CompressedEphemeralPublicKey:
                    _flowModel?.reader.ReaderEphemeralPublicKey = packet.data
                    AppLog.debug("CompressedEphemeralPublicKey received", tag: "BLE", payload: Data(packet.data))
                    break

                case .ReaderLocationIdentifier:
                    _flowModel?.reader.ReaderIdentifier = packet.data
                    AppLog.debug("ReaderIdentifier received", tag: "BLE", payload: Data(packet.data))
                    break
                    
                case .SiteIdenifier:
                    _flowModel?.reader.SiteIdentifier = packet.data;
                    AppLog.debug("SiteIdentifier received", tag: "BLE", payload: Data(packet.data))
                    break
                    
                case .DigitalSignature:
                    AppLog.info("DigitalSignature received", tag: "BLE", payload: Data(packet.data))
                    
                    if (_flowModel == nil)
                    {
                        disconnectPeripheral(PKOCperipheral: peripheral)
                        return
                    }
                
                    let toVerify = _flowModel?.reader
                    if (toVerify?.ReaderIdentifier != nil && toVerify?.SiteIdentifier != nil && toVerify?.ReaderEphemeralPublicKey != nil)
                    {
                        let siteToFind : SiteModel? = loadSiteForVerification(from: _flowModel)
                        
                        if (siteToFind == nil)
                        {
                            AppLog.error("Site not found for verification", tag: "BLE")
                            disconnectPeripheral(PKOCperipheral: peripheral)
                            return
                        }

                        _flowModel?.site = siteToFind
                    }
                                                            
                    _flowModel!.readerValid = CryptoProvider.verifySignedMessage(
                        message: generateSignatureMessage(),
                        signature: packet.data,
                        publicKey: _flowModel!.site!.PublicKey)
                    
                    if (!_flowModel!.readerValid!)
                    {
                        AppLog.error("Signature verification failed", tag: "BLE")
                        disconnectPeripheral(PKOCperipheral: peripheral)
                        return
                    }
                                        
                    break
                                        
                default:
                    AppLog.debug("Notification data decoded: \(notificationData.base64EncodedString())", tag: "BLE")
                    break
            }
        }
                    
        if (_flowModel?.connectionType == PKOC_ConnectionType.ECDHE_Full)
        {
            if (_flowModel?.reader == nil)
            {
                _flowModel?.status = ReaderUnlockStatus.Unrecognized
                AppLog.error("Reader nil during ECDHE_Full", tag: "BLE")
                disconnectPeripheral(PKOCperipheral: peripheral)
                return
            }
            
            if (_flowModel?.reader.ReaderIdentifier == nil)
            {
                _flowModel?.status = ReaderUnlockStatus.Unrecognized
                AppLog.error("ReaderIdentifier nil during ECDHE_Full", tag: "BLE")
                disconnectPeripheral(PKOCperipheral: peripheral)
                return
            }
            
            if (_flowModel?.transientKeyPair == nil)
            {
                AppLog.info("Generating transient key", tag: "BLE")
                _flowModel?.transientKeyPair = CryptoProvider.generateTransientKey()

                let publicKeySecKey = CryptoProvider.fromCompressedPublicKey(compressedPublicKey: _flowModel!.reader.ReaderEphemeralPublicKey!)
                
                if (publicKeySecKey == nil)
                {
                    AppLog.error("Failed to decompress public key", tag: "BLE")
                    disconnectPeripheral(PKOCperipheral: peripheral)
                    return
                }
                
                let rawSharedSecret = [UInt8] (CryptoProvider.createSharedSecret(
                    privateKey: _flowModel!.transientKeyPair!,
                    publicKey: publicKeySecKey!)!)
                _flowModel?.sharedSecret = CryptoProvider.deriveAesKeyFromSharedSecretSimple(rawSharedSecret)
                AppLog.info("Shared secret derived", tag: "BLE")
                
                let transientPublicKey = SecKeyCopyPublicKey(_flowModel!.transientKeyPair!)
                var error: Unmanaged<CFError>?
                let transientPublicKeyData = SecKeyCopyExternalRepresentation(transientPublicKey!, &error)
                var transientPublicKeyBytes = [UInt8](repeating: 0, count: 65)
                CFDataGetBytes(transientPublicKeyData, CFRange(location: CFIndex(), length: CFIndex(integerLiteral: 65)), &transientPublicKeyBytes)
                let transientPublicKeyTLV = TLVProvider.getTLV(type: BLE_PacketType.UncompressedEphemeralPublicKey, data:transientPublicKeyBytes)
                let echdePacket = transientPublicKeyTLV
                
                AppLog.debug("Sending transient public key", tag: "BLE", payload: Data(echdePacket))
                peripheral.writeValue(Data(_: echdePacket), for: self.writeCharacteristic!, type: CBCharacteristicWriteType.withoutResponse)
                return
            }
        }
        
        if (_flowModel?.reader.ReaderEphemeralPublicKey != nil || _flowModel?.sharedSecret != nil)
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
        AppLog.debug("Signature", tag: "BLE", payload: Data(signature))
        let publicKey = CryptoProvider.exportPublicKey().x963Representation
        AppLog.debug("Public key", tag: "BLE", payload: Data(publicKey))
        
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
                AppLog.error("Encryption failed", tag: "BLE")
                disconnectPeripheral(PKOCperipheral: peripheral)
                return
            }
            
            packet = TLVProvider.getTLV(type: BLE_PacketType.EncryptedDataFollows, data: encryptedData!)
        }
        
        AppLog.debug("Sending packet", tag: "BLE", payload: Data(packet))
        peripheral.writeValue(Data(_: packet), for: self.writeCharacteristic!, type: CBCharacteristicWriteType.withoutResponse)
    }
    
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?)
    {
        if let error = error
        {
            AppLog.error("Error reading characteristic: \(error)", tag: "BLE")
            return
        }
        guard let data = characteristic.value else
        {
            AppLog.error("ERROR: No value read from characteristic", tag: "BLE")
            return
        }
        AppLog.debug("Data received, size: \(data.count)", tag: "BLE", payload: data)
        handleNotifications(peripheral: peripheral, notificationData: characteristic.value!)
    }
    
    func peripheral(_ peripheral: CBPeripheral, didWriteValueFor characteristic: CBCharacteristic, error: Error?)
    {
        if let error = error
        {
            AppLog.error("Write error: \(error)", tag: "BLE")
            return
        }
        AppLog.debug("Data written confirmed for \(characteristic.uuid)", tag: "BLE")
    }
}
