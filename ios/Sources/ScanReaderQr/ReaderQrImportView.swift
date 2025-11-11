import SwiftUI

struct ReaderQrImportView : View
{
    @Environment(\.presentationMode) private var presentationMode

    @State private var isScanning = true
    @State private var errorMessage: String?
    
    var body: some View
    {
        VStack
        {
            ZStack
            {
                QrScannerView(isScanning: $isScanning, onCodeFound: handleCode)
                BlinkingScanLineView()
            }
                    
            if let errorMessage
            {
                Text(errorMessage)
                    .font(.footnote)
                    .foregroundColor(.white)
                    .padding(.vertical, 8)
                    .padding(.horizontal, 16)
                    .background(Color.black.opacity(0.7))
                    .cornerRadius(12)
                    .padding(.top, 12)
            }
            
            Text("Point the camera at a reader QR code")
                .font(.body)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
        }
    }

    private func handleCode(_ value: String)
    {
        guard isScanning
        else
        {
            return
        }

        isScanning = false
        errorMessage = nil

        Task
        {
            do
            {
                _ = try await ReaderQrCodeFactory.importFromQRCodeJSON(value)

                await MainActor.run
                {
                    presentationMode.wrappedValue.dismiss()
                }
            }
            catch
            {
                await MainActor.run
                {
                    isScanning = true
                    errorMessage = "Unable to import reader QR."
                }
            }
        }
    }
}
