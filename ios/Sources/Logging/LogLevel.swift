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
