import SwiftUI
import Combine

enum LogLevel: String, CaseIterable, Identifiable
{
    case trace, debug, info, warn, error
    var id: String { rawValue }

    var priority: Int
    {
        switch self
        {
        case .trace: return 0
        case .debug: return 1
        case .info:  return 2
        case .warn:  return 3
        case .error: return 4
        }
    }
}

struct LogEntry: Identifiable, Equatable
{
    let id = UUID()
    let date: Date
    let level: LogLevel
    let tag: String?
    let message: String
    let payload: Data?
}

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

struct DiagnosticsLogView: View
{
    @ObservedObject var store: LogStore = .shared

    @State private var search = ""
    @State private var selectedLevels = Set(LogLevel.allCases)
    @State private var selectedTag: String? = nil

    var body: some View
    {
        VStack(spacing: 0)
        {
            header
            Divider()
            content
        }
    }

    var header: some View
    {
        ScrollView(.horizontal, showsIndicators: false)
        {
            HStack(spacing: 12)
            {
                TextField("Search", text: $search)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .frame(maxWidth: 200)

                Menu
                {
                    Button("Only WARN+") { selectedLevels = Set(LogLevel.allCases.filter { $0.priority >= LogLevel.warn.priority }) }
                    Button("Only ERROR")  { selectedLevels = [.error] }
                    Button("All Levels")  { selectedLevels = Set(LogLevel.allCases) }
                    Divider()
                    ForEach(LogLevel.allCases)
                    { level in
                        Button(action: { toggle(level: level) })
                        {
                            HStack
                            {
                                Text(level.rawValue.uppercased())
                                Spacer()
                                if selectedLevels.contains(level) { Image(systemName: "checkmark") }
                            }
                        }
                    }
                } label:
                {
                    Label("Levels", systemImage: "line.3.horizontal.decrease.circle")
                }

                Menu
                {
                    Button("All Tags") { selectedTag = nil }
                    let tags = Set(store.entries.compactMap { $0.tag }.filter { !$0.isEmpty }).sorted()
                    ForEach(tags, id: \.self) { tag in Button(tag) { selectedTag = tag } }
                } label:
                {
                    Label(selectedTag ?? "Tags", systemImage: "tag")
                }

                Toggle("Follow", isOn: $store.followTail)
                    .toggleStyle(SwitchToggleStyle())
                    .frame(width: 120)

                Button(store.isPaused ? "Resume" : "Pause") { store.isPaused.toggle() }

                Button("Copy")
                {
                    let text = store.exportText(filtered: filteredEntries())
                    UIPasteboard.general.string = text
                }

                Button("Share")
                {
                    let text = store.exportText(filtered: filteredEntries())
                    if let url = FileExporter.writeTemp(text: text, filename: "diagnostics-log.txt")
                    {
                        let activityVC = UIActivityViewController(activityItems: [url], applicationActivities: nil)
                        UIApplication.shared.windows.first?.rootViewController?.present(activityVC, animated: true, completion: nil)
                    }
                }

                Button("Clear") { store.clear() }
            }
            .padding(8)
        }
    }

    var content: some View
    {
        ScrollViewReader
        { proxy in
            ScrollView
            {
                LazyVStack(alignment: .leading, spacing: 4)
                {
                    ForEach(filteredEntries())
                    { entry in
                        LogRow(entry: entry)
                            .id(entry.id)
                    }
                }
                .padding(8)
            }
            .background(Color(UIColor.systemBackground))
            .onReceive(store.$entries)
            { _ in
                guard store.followTail, !store.isPaused else { return }
                if let last = filteredEntries().last
                {
                    withAnimation(.easeOut(duration: 0.15)) { proxy.scrollTo(last.id, anchor: .bottom) }
                }
            }
        }
    }

    func filteredEntries() -> [LogEntry]
    {
        var items = store.entries
        if !search.isEmpty
        {
            let q = search.lowercased()
            items = items.filter
            {
                $0.message.lowercased().contains(q)
                || ($0.tag?.lowercased().contains(q) ?? false)
                || ($0.payload?.hexadecimal().lowercased().contains(q) ?? false)
            }
        }
        if let tag = selectedTag { items = items.filter { $0.tag == tag } }
        items = items.filter { selectedLevels.contains($0.level) }
        return items
    }

    func toggle(level: LogLevel)
    {
        if selectedLevels.contains(level) { selectedLevels.remove(level) }
        else { selectedLevels.insert(level) }
    }
}

struct LogRow: View
{
    let entry: LogEntry

    var body: some View
    {
        VStack(alignment: .leading, spacing: 2)
        {
            HStack(spacing: 6)
            {
                Text(Self.df.string(from: entry.date))
                    .font(.system(size: 12, weight: .regular, design: .monospaced))
                    .foregroundColor(.secondary)
                if let tag = entry.tag
                {
                    Text(tag)
                        .font(.system(size: 11))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Capsule().fill(Color(UIColor.secondarySystemFill)))
                }
                Text(entry.level.rawValue.uppercased())
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(color(for: entry.level))
            }
            Text(entry.message)
                .font(.system(size: 13, weight: .regular, design: .monospaced))
            if let d = entry.payload, !d.isEmpty
            {
                Text(d.hexadecimal())
                    .font(.system(size: 11, weight: .regular, design: .monospaced))
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 2)
    }

    func color(for level: LogLevel) -> Color
    {
        switch level
        {
        case .trace: return .secondary
        case .debug: return .secondary
        case .info:  return .blue
        case .warn:  return .orange
        case .error: return .red
        }
    }

    static let df: DateFormatter =
    {
        let df = DateFormatter()
        df.dateFormat = "HH:mm:ss.SSS"
        return df
    }()
}

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

enum AppLog
{
    static var store: LogStore { .shared }

    static func trace(_ message: String, tag: String? = nil, payload: Data? = nil) { store.append(.trace, tag: tag, message, payload: payload) }
    static func debug(_ message: String, tag: String? = nil, payload: Data? = nil) { store.append(.debug, tag: tag, message, payload: payload) }
    static func info (_ message: String, tag: String? = nil, payload: Data? = nil) { store.append(.info,  tag: tag, message, payload: payload) }
    static func warn (_ message: String, tag: String? = nil, payload: Data? = nil) { store.append(.warn,  tag: tag, message, payload: payload) }
    static func error(_ message: String, tag: String? = nil, payload: Data? = nil) { store.append(.error, tag: tag, message, payload: payload) }
}
