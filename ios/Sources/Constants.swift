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
