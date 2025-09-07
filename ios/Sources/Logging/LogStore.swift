import SwiftUI

final class LogStore: ObservableObject
{
    static let shared = LogStore()

    @Published private(set) var entries: [LogEntry] = []
    @Published var isPaused = false
    @Published var followTail = true

    var capacity = 2000

    func append(_ level: LogLevel, tag: String? = nil, _ message: String, payload: Data? = nil)
    {
        let entry = LogEntry(date: Date(), level: level, tag: tag, message: message, payload: payload)
        DispatchQueue.main.async
        {
            if self.entries.count >= self.capacity
            {
                self.entries.removeFirst(self.entries.count - self.capacity + 1)
            }
            self.entries.append(entry)
        }
    }

    func clear()
    {
        DispatchQueue.main.async
        {
            self.entries.removeAll(keepingCapacity: true)
        }
    }

    func exportText(filtered: [LogEntry]) -> String
    {
        let df = DateFormatter()
        df.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
        return filtered.map
        {
            var line = "\(df.string(from: $0.date)) [\($0.level.rawValue.uppercased())]"
            if let t = $0.tag, !t.isEmpty
            {
                line += " {\(t)}"
            }
            line += " \($0.message)"
            if let d = $0.payload, !d.isEmpty
            {
                line += " | \(d.hexadecimal())"
            }
            return line
        }
        .joined(separator: "\n")
    }
}
