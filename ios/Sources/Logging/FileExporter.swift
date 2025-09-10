import SwiftUI

enum FileExporter
{
    static func writeTemp(text: String, filename: String) -> URL?
    {
        let dir = URL(fileURLWithPath: NSTemporaryDirectory())
        let url = dir.appendingPathComponent(filename)
        do
        {
            try text.data(using: .utf8)?.write(to: url, options: .atomic)
            return url
        }
        catch
        {
            return nil
        }
    }
}
