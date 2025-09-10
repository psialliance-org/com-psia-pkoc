import SwiftUI

struct LogEntry: Identifiable, Equatable
{
    let id = UUID()
    let date: Date
    let level: LogLevel
    let tag: String?
    let message: String
    let payload: Data?
}
