import Foundation

enum GrpcWebError: Error, LocalizedError
{
    case grpcStatus(Int, String)
    case httpError(Int)
    case malformedResponse

    var statusCode: Int
    {
        if case .grpcStatus(let code, _) = self { return code }
        return -1
    }

    var grpcMessage: String
    {
        if case .grpcStatus(_, let msg) = self { return msg }
        return ""
    }

    var statusName: String
    {
        switch statusCode
        {
            case 0:  return "OK"
            case 1:  return "CANCELLED"
            case 2:  return "UNKNOWN"
            case 3:  return "INVALID_ARGUMENT"
            case 4:  return "DEADLINE_EXCEEDED"
            case 5:  return "NOT_FOUND"
            case 6:  return "ALREADY_EXISTS"
            case 7:  return "PERMISSION_DENIED"
            case 8:  return "RESOURCE_EXHAUSTED"
            case 9:  return "FAILED_PRECONDITION"
            case 10: return "ABORTED"
            case 11: return "OUT_OF_RANGE"
            case 12: return "UNIMPLEMENTED"
            case 13: return "INTERNAL"
            case 14: return "UNAVAILABLE"
            case 15: return "DATA_LOSS"
            case 16: return "UNAUTHENTICATED"
            default: return "STATUS_\(statusCode)"
        }
    }

    var errorDescription: String?
    {
        switch self
        {
            case .grpcStatus(let code, let msg):
                return "gRPC error \(code) (\(statusName)): \(msg)"
            case .httpError(let code):
                return "HTTP error \(code)"
            case .malformedResponse:
                return "Malformed gRPC-Web response"
        }
    }
}
