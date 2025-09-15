import SwiftUI
import CoreImage
import CoreImage.CIFilterBuiltins
import Toast
import BigInt

struct DisplayPublicKeyView : View
{
    @State private var uncompressedPublicKey: Data? = nil
    @State private var publicKey: String = ""
    @State private var selectedOption: DisplayPublicKeyOption = .FullPublicKey
    @State private var customBits: Int = 64

    var body: some View
    {
        VStack
        {
            Spacer()

            Picker("Display Options", selection: $selectedOption)
            {
                Text("Full Public Key").tag(DisplayPublicKeyOption.FullPublicKey)
                Text("64 Bit").tag(DisplayPublicKeyOption.Bit64)
                Text("128 Bit").tag(DisplayPublicKeyOption.Bit128)
                Text("200 Bit").tag(DisplayPublicKeyOption.Bit200)
                Text("256 Bit").tag(DisplayPublicKeyOption.Bit256)
                Text("Custom (X bits)").tag(DisplayPublicKeyOption.CustomBits)
            }
            .padding(.vertical, 8)
            .onChange(of: selectedOption)
            {
                _ in updatePublicKeyDisplay()
            }

            if selectedOption == .CustomBits
            {
                HStack
                {
                    Spacer()

                    Stepper(value: $customBits, in: 8...256, step: 8)
                    {
                        Text("\(customBits) bits")
                            .font(.headline)
                            .frame(minWidth: 120)
                            .multilineTextAlignment(.center)
                    }
                    .onChange(of: customBits)
                    {
                        _ in updatePublicKeyDisplay()
                    }

                    Spacer()
                }
                .padding(.vertical)
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
                                .frame(maxWidth: geometry.size.width * 0.7)
                                .background(
                                    RoundedRectangle(cornerRadius: 16)
                                        .fill(Color(.systemBackground))
                                        .shadow(radius: 5)
                                )
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

            Text(publicKey)
                .font(.system(size: 14, design: .monospaced))
                .multilineTextAlignment(.leading)
                .lineLimit(nil)
                .fixedSize(horizontal: false, vertical: true)
                .padding(8)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.secondarySystemBackground))
                .cornerRadius(8)
                .padding(.horizontal)

            Button(action:
            {
                UIPasteboard.general.string = publicKey
                Toast.text("Copied to clipboard", config: .init(direction: .bottom)).show()
            })
            {
                Label("Copy", systemImage: "doc.on.doc")
                    .padding(.horizontal)
            }
            .padding(.vertical, 8)

            Spacer()
        }
        .onAppear
        {
            let keyData = CryptoProvider.exportPublicKey().x963Representation
            uncompressedPublicKey = keyData
            publicKey = keyData.hexadecimal()
            updatePublicKeyDisplay()
        }
        .onChange(of: uncompressedPublicKey)
        {
            _ in updatePublicKeyDisplay()
        }
    }

    private func updatePublicKeyDisplay()
    {
        guard let keyData = uncompressedPublicKey,
              keyData.count == 65,
              keyData[0] == 0x04
        else
        {
            publicKey = "Invalid key"
            return
        }

        let xComponent = keyData[1..<33]

        switch selectedOption
        {
            case .FullPublicKey:
                publicKey = keyData.hexadecimal()

            case .Bit64:
                publicKey = decimalOfLeastSignificantBits(of: xComponent, bits: 64)

            case .Bit128:
                publicKey = decimalOfLeastSignificantBits(of: xComponent, bits: 128)

            case .Bit200:
                publicKey = decimalOfLeastSignificantBits(of: xComponent, bits: 200)

            case .Bit256:
                publicKey = decimalOfLeastSignificantBits(of: xComponent, bits: 256)

            case .CustomBits:
                publicKey = decimalOfLeastSignificantBits(of: xComponent, bits: customBits)
        }
    }

    private func decimalOfLeastSignificantBits(of xBytesSlice: Data.SubSequence, bits: Int) -> String
    {
        let full = BigUInt(Data(xBytesSlice))

        if bits >= 256
        {
            return full.description
        }

        let mask = (BigUInt(1) << Int(bits)) - 1
        let value = full & mask
        return value.description
    }

    private func generateQRCode(from string: String, targetSize: CGSize) -> UIImage?
    {
        let context = CIContext()
        let filter = CIFilter.qrCodeGenerator()
        filter.setValue(Data(string.utf8), forKey: "inputMessage")
        filter.setValue("L", forKey: "inputCorrectionLevel")

        guard let outputImage = filter.outputImage
        else
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
