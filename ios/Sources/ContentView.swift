import SwiftUI
import CoreBluetooth
import CryptoKit

struct ContentView : View
{
    @State var settingsLinkSelected = false
    @State var aboutLinkSelected = false
    @State var qrLinkSelected = false
    @State var sitesLinkSelected = false
    @State var logsLinkSelected = false
    @State var importReaderQrLinkSelected = false
    
    @State var _transmissionType : TransmissionType = TransmissionType.BLE

    func loadValues()
    {
        let rawValue = UserDefaults.standard.object(forKey: TransmissionTypeSelected) as? Int
        _transmissionType = TransmissionType(rawValue: rawValue ?? TransmissionType.BLE.rawValue) ?? .BLE
    }

    func loadSecureKeysData()
    {
        KeyStore.load()
        {
            result in switch result
            {
                case .failure(let error):
                    print ("ERROR/WARNING: \(error)")
                    CryptoProvider.generateAndSendPublishKey()
                    {
                        _ in
                        print("DEBUG: New keys created!")
                        print("DEBUG: Saving these keys to storage")
                        storeSecureKeyData()
                        
                        let secondsStamp = Int(Date().timeIntervalSince1970)
                        UserDefaults.standard.set(secondsStamp, forKey: PKOC_CreationTime)
                    }
                case .success(let keys):
                    do
                    {
                        try CryptoProvider.loadKeys(
                            privateKey: P256.Signing.PrivateKey(rawRepresentation: keys.privateKey),
                            publicKey: P256.Signing.PublicKey(rawRepresentation: keys.publicKey)
                        )
                    }
                    catch
                    {
                        print("Error: Error in converting keys from storage to app data")
                        fatalError()
                    }
            }
        }
    }
    
    func storeSecureKeyData()
    {
        KeyStore.save(
            keyData: KeyData(
                publicKey: CryptoProvider.exportPublicKey().rawRepresentation,
                privateKey: CryptoProvider.exportPrivateKey().rawRepresentation
            )
        )
        {
            result in switch result
            {
            case .success( _):
                print("Saving to persistent storage successful")
            case .failure(let error):
                print("ERROR saving to persistent storage: \(error)")
            }
        }
    }
    
    var body : some View
    {
        let navigationViewStyleForView = StackNavigationViewStyle()

        NavigationView
        {
            VStack
            {
                if (_transmissionType == TransmissionType.BLE)
                {
                    UpdatingListView()
                }
                else
                {
                    PresentDeviceView()
                }

                Image(uiImage: UIImage(named: ProductImages.PSIA_Logo_Typographic)!)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .padding(16)
            }
            .onAppear()
            {
                loadValues()
                loadSecureKeysData()
            }
            .navigationTitle("PSIA PKOC Credential")
            .toolbar
            {
                let placement = ToolbarItemPlacement.topBarTrailing

                ToolbarItemGroup(placement: placement)
                {
                    Menu
                    {
                        Button(action:
                        {
                            settingsLinkSelected = true
                        }, label:
                        {
                            Label("Settings", systemImage: "gearshape")
                        }).navigationTitle("Settings")

                        Button(action:
                        {
                            aboutLinkSelected = true
                        }, label:
                        {
                            Label("About", systemImage: "info.circle")
                        }).navigationTitle("About")

                        Button(action:
                        {
                            qrLinkSelected = true
                        }, label:
                        {
                            Label("Display QR Public Key", systemImage: "qrcode")
                        }).navigationTitle("Display QR Public Key")
                        
                        Button(action:
                        {
                            sitesLinkSelected = true
                        }, label:
                        {
                            Label("Sites & Readers", systemImage: "list.bullet.rectangle")
                        }).navigationTitle("Sites & Readers")
                        Button(action:
                        {
                            importReaderQrLinkSelected = true
                        }, label:
                        {
                            Label("Scan Reader", systemImage: "qrcode.viewfinder")
                        }).navigationTitle("Scan Reader QR Code")
                        Button(action:
                        {
                            logsLinkSelected = true
                        }, label:
                        {
                            Label("Diagnostics Log", systemImage: "doc.text.magnifyingglass")
                        }).navigationTitle("Diagnostics Log")
                        
                    }
                    label:
                    {
                        Label("Menu", systemImage: "ellipsis.circle")
                    }
                }
            }
            .background(
                NavigationLink(
                    destination: SitesView().navigationTitle("Sites & Readers"),
                    isActive: $sitesLinkSelected
                ) { EmptyView() }.hidden()
            )
            .background(
                NavigationLink(
                    destination: SettingsView().navigationTitle("Settings"),
                    isActive: $settingsLinkSelected
                ) { EmptyView() }.hidden()
            )
            .background(
                NavigationLink(
                    destination: AboutView().navigationTitle("About"),
                    isActive: $aboutLinkSelected
                ) { EmptyView() }.hidden()
            )
            .background(
                NavigationLink(
                    destination: DisplayPublicKeyView().navigationTitle("Display QR Public Key"),
                    isActive: $qrLinkSelected
                ) { EmptyView() }.hidden()
            )
            .background(
                NavigationLink(
                    destination: ReaderQrImportView().navigationTitle("Scan Reader QR Code"),
                    isActive: $importReaderQrLinkSelected
                )
                { EmptyView() }.hidden()
            )
            .background(
                NavigationLink(
                    destination: DiagnosticsLogView().navigationTitle("Diagnostics Log"),
                    isActive: $logsLinkSelected
                ) { EmptyView() }.hidden()
            )
        }
        .navigationViewStyle(navigationViewStyleForView)
    }
}
