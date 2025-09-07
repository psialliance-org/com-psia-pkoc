import Foundation
import CoreBluetooth

let ServiceUUID = CBUUID(string: "0000FFF0-0000-1000-8000-00805F9B34FB");
let ServiceLegacyUUID = CBUUID(string: "41fb60a1-d4d0-4ae9-8cbb-b62b5ae81810");
let WriteUUID = CBUUID(string: "fe278a85-89ae-191f-5dde-841202693835");
let ReadUUID = CBUUID(string: "e5b1b3b5-3cca-3f76-cd86-a884cc239692");
let ConfigUUID = CBUUID(string: "00002902-0000-1000-8000-00805f9b34fb");

let PKOC_TransmissionFlow = "PKOC_TransmissionFlow";
let PKOC_CredentialSet = "PKOC_CredentialSet";
let PKOC_CreationTime = "PKOC_CreationTime";
let TransmissionTypeSelected = "TransmissionTypeSelected";
let AutoDiscoverDevices = "AutoDiscoverDevices";
let EnableRanging = "EnableRanging";
let RangeValue = "RangeValue";
let DiscoveryRangeValue = "DiscoveryRangeValue"
let DisplayMAC = "DisplayMAC";
let EulaAccepted = "EulaAccepted";

let KnownReaders : [ReaderModel]  =
{
    [
        ReaderModel(
            ProtocolVersion: [200],
            ReaderIdentifier: UUID(uuidString: "ad0cbc8f-c353-427a-b479-37b5efcff6be")!.asUInt8Array(),
            SiteIdentifier: UUID(uuidString: "b9897ed0-5272-4341-979a-b69850112d80")!.asUInt8Array())
    ]
}()

let KnownSites : [SiteModel] =
{
   [
        SiteModel(
            SiteIdentifier: UUID(uuidString: "b9897ed0-5272-4341-979a-b69850112d80")!.asUInt8Array(),
            PublicKey: "04b71bb4b0de53f06a09ea6c91b483a898645005a30ec9422b95a67908f640abac440b1e4e705db4a626f7ac4e4dcfeba9f7157872446e61f58282c426f4e838af".hexStringToBytes()!)
   ]
}()
