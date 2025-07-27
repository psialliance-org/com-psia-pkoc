
import SwiftUI
import CoreImage
import CoreImage.CIFilterBuiltins
import Toast
import BigInt

struct DisplayPublicKeyView : View

{
    @State private var uncompressedPublicKey: Data? = nil
    @State private var publicKey: String = ""
    @State private var selectedOption : DisplayPublicKeyOption = .FullPublicKey

    var body : some View
    {
        VStack
        {
            Spacer()

            Picker("Display Options", selection: $selectedOption)
            {
                Text("Full Public Key").tag(DisplayPublicKeyOption.FullPublicKey)
                Text("64 Bit").tag(DisplayPublicKeyOption.Bit64)
                Text("128 Bit").tag(DisplayPublicKeyOption.Bit128)
                Text("256 Bit").tag(DisplayPublicKeyOption.Bit256)
            }
            .onChange(of: selectedOption)
            {
                newValue in guard let keyData = uncompressedPublicKey,
                    keyData.count == 65,
                    keyData[0] == 0x04
                else
                {
                    publicKey = "Invalid key"
                    return
                }

                let xComponent = keyData[1..<33]

                switch newValue
                {
                    case .FullPublicKey:
                        publicKey = keyData.hexadecimal()

                    case .Bit64:
                        let last8 = xComponent.suffix(8)
                        let value = BigUInt(Data(last8))
                        publicKey = value.description

                    case .Bit128:
                        let last16 = xComponent.suffix(16)
                        let value = BigUInt(Data(last16))
                        publicKey = value.description

                    case .Bit256:
                        let value = BigUInt(Data(xComponent))
                        publicKey = value.description
                }
            }

            GeometryReader
            {
                geometry in HStack
                {
                    Spacer()

                    Group
                    {
                        if let qrImage = generateQRCode(from: publicKey, targetSize: geometry.size)
                        {
                            Image(uiImage: qrImage)
                                .interpolation(.none)
                                .resizable()
                                .scaledToFit()
                                .frame(maxWidth: geometry.size.width * 0.8)
                                .background(Color.white)
                        }
                        else
                        {
                            Text("Failed to generate QR code")
                                .foregroundColor(.red)
                        }
                    }

                    Spacer()
                }
            }
            .frame(maxHeight: 300)
            .layoutPriority(1)
            
            TextEditor(text: $publicKey)
                .font(.system(size: 14, design: .monospaced))
                .frame(height: 100)
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.gray.opacity(0.4)))
                .padding(.horizontal)
                .disabled(true)

            Button(action:
            {
                UIPasteboard.general.string = publicKey
                Toast.text("Copied to clipboard", config: .init(direction: .bottom)).show()
            })
            {
                Label("Copy", systemImage: "doc.on.doc")
                    .padding(.horizontal)
            }

            Spacer()
        }.onAppear
        {
            let keyData = CryptoProvider.exportPublicKey().x963Representation
            uncompressedPublicKey = keyData
            publicKey = keyData.hexadecimal()
        }
    }

    private func generateQRCode(from string: String, targetSize: CGSize) -> UIImage?
    {
        let context = CIContext()
        let filter = CIFilter.qrCodeGenerator()
        filter.setValue(Data(string.utf8), forKey: "inputMessage")
        filter.setValue("L", forKey: "inputCorrectionLevel")

        guard let outputImage = filter.outputImage else
        {
            return nil
        }

        let scaleX = floor(targetSize.width / outputImage.extent.width)
        let scaleY = floor(targetSize.width / outputImage.extent.height)
        let scaledImage = outputImage.transformed(by: CGAffineTransform(scaleX: scaleX, y: scaleY))

        if let cgImage = context.createCGImage(scaledImage, from: scaledImage.extent)
        {
            return UIImage(cgImage: cgImage)
        }

        return nil
    }
}
