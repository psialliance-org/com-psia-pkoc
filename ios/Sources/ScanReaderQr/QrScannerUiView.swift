import AVFoundation
import UIKit

final class QrScannerUiView : UIView, AVCaptureMetadataOutputObjectsDelegate
{
    var onCodeFound: ((String) -> Void)?
    
    private let captureSession = AVCaptureSession()
    private var previewLayer: AVCaptureVideoPreviewLayer?
    private var isConfigured = false
    private let sessionQueue = DispatchQueue(label: "camera.session.queue")
    
    override class var layerClass : AnyClass
    {
        AVCaptureVideoPreviewLayer.self
    }
    
    override init(frame: CGRect)
    {
        super.init(frame: frame)
        configureLayer()
    }
    
    required init?(coder: NSCoder)
    {
        super.init(coder: coder)
        configureLayer()
    }
    
    func configureSession()
    {
        guard !isConfigured
        else
        {
            return
        }
        
        sessionQueue.async
        {
            guard let device = AVCaptureDevice.default(for: .video) else
            {
                return
            }
            
            do
            {
                let input = try AVCaptureDeviceInput(device: device)
                
                if self.captureSession.canAddInput(input)
                {
                    self.captureSession.addInput(input)
                }
                
                let metadataOutput = AVCaptureMetadataOutput()
                
                if self.captureSession.canAddOutput(metadataOutput)
                {
                    self.captureSession.addOutput(metadataOutput)
                    metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
                    metadataOutput.metadataObjectTypes = [.qr]
                }
                
                DispatchQueue.main.async
                {
                    self.previewLayer?.session = self.captureSession
                }
                
                self.captureSession.startRunning()
                self.isConfigured = true
            }
            catch
            {
                print("Failed to configure capture session: \(error)")
            }
        }
    }
    
    func updateScanning(_ shouldScan: Bool)
    {
        if shouldScan
        {
            if !captureSession.isRunning
            {
                sessionQueue.async
                {
                    self.captureSession.startRunning()
                }
            }
        }
        else
        {
            if captureSession.isRunning
            {
                captureSession.stopRunning()
            }
        }
    }
    
    private func configureLayer()
    {
        guard let layer = self.layer as? AVCaptureVideoPreviewLayer
        else
        {
            return
        }
        
        layer.videoGravity = .resizeAspectFill
        previewLayer = layer
    }
        
    func metadataOutput(
        _ output: AVCaptureMetadataOutput,
        didOutput metadataObjects: [AVMetadataObject],
        from connection: AVCaptureConnection)
    {
        guard
            let object = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
            object.type == .qr,
            let value = object.stringValue
        else
        {
            return
        }
        
        onCodeFound?(value)
    }
}
