import Foundation
import CryptoKit

/// Low-level gRPC-Web transport over URLSession.
/// Mirrors the Android GrpcWebClient + GrpcWebInterceptor.
final class GrpcWebClient
{
    static let shared = GrpcWebClient()

    private let baseURL = "https://api.opencredential.sentryinteractive.com"
    private let session = URLSession.shared

    private init() {}

    // MARK: - Public API

    /// Execute a gRPC-Web unary call and return the raw framed response.
    func call(servicePath: String, method: String, body: Data) async throws -> Data
    {
        let url = URL(string: baseURL + servicePath + "/" + method)!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/grpc-web", forHTTPHeaderField: "Content-Type")
        request.setValue("application/grpc-web", forHTTPHeaderField: "Accept")
        request.setValue("1", forHTTPHeaderField: "X-Grpc-Web")

        let framedBody = frameGrpcWeb(body)
        addSigningHeaders(to: &request, body: framedBody)
        request.httpBody = framedBody

        let (responseData, response) = try await session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else
        {
            throw GrpcWebError.malformedResponse
        }

        let status = extractGrpcStatus(from: httpResponse, data: responseData)
        if status != 0
        {
            let message = extractGrpcMessage(from: httpResponse, data: responseData)
            throw GrpcWebError.grpcStatus(status, message)
        }

        guard (200..<300).contains(httpResponse.statusCode) else
        {
            throw GrpcWebError.httpError(httpResponse.statusCode)
        }

        return responseData
    }

    /// Extract the protobuf message bytes from a gRPC-Web data frame.
    func parseDataFrame(_ responseData: Data) throws -> Data
    {
        guard responseData.count >= 5 else { throw GrpcWebError.malformedResponse }
        let flags = responseData[responseData.startIndex]
        let length = Int(responseData[responseData.startIndex + 1]) << 24
                   | Int(responseData[responseData.startIndex + 2]) << 16
                   | Int(responseData[responseData.startIndex + 3]) << 8
                   | Int(responseData[responseData.startIndex + 4])
        guard (flags & 0x80) == 0 else { throw GrpcWebError.malformedResponse }
        guard responseData.count >= 5 + length else { throw GrpcWebError.malformedResponse }
        return responseData[(responseData.startIndex + 5)..<(responseData.startIndex + 5 + length)]
    }

    // MARK: - gRPC-Web Framing

    private func frameGrpcWeb(_ message: Data) -> Data
    {
        var result = Data(capacity: 5 + message.count)
        result.append(0x00)  // flags: no compression
        result.append(UInt8((message.count >> 24) & 0xFF))
        result.append(UInt8((message.count >> 16) & 0xFF))
        result.append(UInt8((message.count >> 8) & 0xFF))
        result.append(UInt8(message.count & 0xFF))
        result.append(message)
        return result
    }

    // MARK: - Status / Message Extraction

    private func extractGrpcStatus(from response: HTTPURLResponse, data: Data) -> Int
    {
        if let header = response.value(forHTTPHeaderField: "grpc-status"),
           let code = Int(header)
        {
            return code
        }
        return scanTrailerFrame(data: data, key: "grpc-status").flatMap { Int($0) }
            ?? ((200..<300).contains(response.statusCode) ? 0 : 2)
    }

    private func extractGrpcMessage(from response: HTTPURLResponse, data: Data) -> String
    {
        if let header = response.value(forHTTPHeaderField: "grpc-message") { return header }
        return scanTrailerFrame(data: data, key: "grpc-message") ?? ""
    }

    private func scanTrailerFrame(data: Data, key: String) -> String?
    {
        var offset = 0
        while offset + 5 <= data.count
        {
            let flags = data[offset]
            let len = Int(data[offset + 1]) << 24
                    | Int(data[offset + 2]) << 16
                    | Int(data[offset + 3]) << 8
                    | Int(data[offset + 4])
            if (flags & 0x80) != 0, offset + 5 + len <= data.count,
               let trailers = String(data: data[(offset + 5)..<(offset + 5 + len)], encoding: .utf8)
            {
                for line in trailers.components(separatedBy: "\r\n")
                {
                    let prefix = key + ":"
                    if line.hasPrefix(prefix)
                    {
                        return String(line.dropFirst(prefix.count)).trimmingCharacters(in: .whitespaces)
                    }
                }
            }
            offset += 5 + len
            if len == 0 { break }
        }
        return nil
    }

    // MARK: - RFC 9421 HTTP Message Signatures

    private func addSigningHeaders(to request: inout URLRequest, body: Data)
    {
        do
        {
            let privateKey = CryptoProvider.exportPrivateKey()
            let publicKey  = CryptoProvider.exportPublicKey()

            let derPublicKey = publicKey.derRepresentation

            // content-digest: sha-256=:<base64>:
            let bodyHash     = SHA256.hash(data: body)
            let contentDigest = "sha-256=:" + Data(bodyHash).base64EncodedString() + ":"

            // keyid = base64url-unpadded SHA-256 of DER public key
            let thumbprint = SHA256.hash(data: derPublicKey)
            let keyId      = Data(thumbprint).base64URLEncoded()

            let created   = Int(Date().timeIntervalSince1970)
            let path      = request.url?.path ?? "/"
            let authority = request.url?.host ?? ""

            let sigInputValue =
                "(\"@method\" \"@path\" \"@authority\" \"content-digest\")" +
                ";alg=\"ecdsa-p256-sha256\"" +
                ";keyid=\"\(keyId)\"" +
                ";created=\(created)"

            let sigBase =
                "\"@method\": POST\n" +
                "\"@path\": \(path)\n" +
                "\"@authority\": \(authority)\n" +
                "\"content-digest\": \(contentDigest)\n" +
                "\"@signature-params\": \(sigInputValue)"

            let signature = try privateKey.signature(for: Data(sigBase.utf8))

            request.setValue(contentDigest,                                   forHTTPHeaderField: "content-digest")
            request.setValue("sig1=\(sigInputValue)",                          forHTTPHeaderField: "signature-input")
            request.setValue("sig1=:\(signature.derRepresentation.base64EncodedString()):", forHTTPHeaderField: "signature")
        }
        catch
        {
            // Proceed unsigned – signing is best-effort
            AppLog.warn("Failed to sign gRPC-Web request: \(error)", tag: "GrpcWebClient")
        }
    }
}

// MARK: - Data helpers

private extension Data
{
    func base64URLEncoded() -> String
    {
        base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}
