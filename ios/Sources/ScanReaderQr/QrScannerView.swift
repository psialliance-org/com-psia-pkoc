import SwiftUI

struct QrScannerView : UIViewRepresentable
{
    @Binding var isScanning: Bool
    var onCodeFound: (String) -> Void

    func makeUIView(context: Context) -> QrScannerUiView
    {
        let view = QrScannerUiView()
        view.onCodeFound =
        {
            code in
            onCodeFound(code)
        }
        view.configureSession()
        return view
    }

    func updateUIView(_ uiView: QrScannerUiView, context: Context)
    {
        uiView.updateScanning(isScanning)
    }
}
