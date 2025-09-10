import SwiftUI

enum AppLog
{
    static var store: LogStore { .shared }

    static func trace(_ message: String, tag: String? = nil, payload: Data? = nil) { store.append(.trace, tag: tag, message, payload: payload) }
    static func debug(_ message: String, tag: String? = nil, payload: Data? = nil) { store.append(.debug, tag: tag, message, payload: payload) }
    static func info (_ message: String, tag: String? = nil, payload: Data? = nil) { store.append(.info,  tag: tag, message, payload: payload) }
    static func warn (_ message: String, tag: String? = nil, payload: Data? = nil) { store.append(.warn,  tag: tag, message, payload: payload) }
    static func error(_ message: String, tag: String? = nil, payload: Data? = nil) { store.append(.error, tag: tag, message, payload: payload) }
}
