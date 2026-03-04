import Foundation

// MARK: - Enums

enum SentryCredentialType: Int
{
    case unspecified = 0
    case p256       = 1
    case p384       = 2
    case p521       = 3
    case ed25519    = 4
    case ed448      = 5
    case mlDsa44    = 6
    case mlDsa65    = 7
    case mlDsa87    = 8
}

enum SentryCredentialFilter: Int
{
    case unspecified   = 0
    case sameIdentity  = 1
    case sameKey       = 2
}

// MARK: - Models

struct SentryIdentity
{
    enum IdentityCase
    {
        case email(String)
        case phone(String)
        case none
    }
    var identityCase: IdentityCase = .none

    var email: String?
    {
        if case .email(let v) = identityCase { return v }
        return nil
    }

    var phone: String?
    {
        if case .phone(let v) = identityCase { return v }
        return nil
    }
}

struct SentryCredential
{
    var identity: SentryIdentity?
    var credential: Data = Data()
    var credentialType: SentryCredentialType = .unspecified

    var credentialHex: String
    {
        credential.map { String(format: "%02x", $0) }.joined()
    }
}

struct SentryStartEmailVerificationResponse
{
    var verificationToken: String = ""
}

struct SentryGetCredentialsResponse
{
    var credentials: [SentryCredential] = []
}

struct SentryOrganization
{
    var organizationId: String = ""
    var name: String = ""
    var contactEmail: String = ""
    var contactPhone: String = ""
    var contactAddress: String = ""
}

// MARK: - Protobuf Encoder

struct ProtoEncoder
{
    private var data = Data()

    mutating func encodeString(_ fieldNumber: Int, _ value: String)
    {
        guard !value.isEmpty else { return }
        let bytes = Data(value.utf8)
        appendTag(fieldNumber: fieldNumber, wireType: 2)
        appendVarint(UInt64(bytes.count))
        data.append(bytes)
    }

    mutating func encodeBytes(_ fieldNumber: Int, _ value: Data)
    {
        guard !value.isEmpty else { return }
        appendTag(fieldNumber: fieldNumber, wireType: 2)
        appendVarint(UInt64(value.count))
        data.append(value)
    }

    mutating func encodeEnum(_ fieldNumber: Int, _ value: Int)
    {
        guard value != 0 else { return }
        appendTag(fieldNumber: fieldNumber, wireType: 0)
        appendVarint(UInt64(bitPattern: Int64(value)))
    }

    mutating func encodeMessage(_ fieldNumber: Int, _ inner: ProtoEncoder)
    {
        let built = inner.build()
        guard !built.isEmpty else { return }
        appendTag(fieldNumber: fieldNumber, wireType: 2)
        appendVarint(UInt64(built.count))
        data.append(built)
    }

    func build() -> Data { data }

    private mutating func appendTag(fieldNumber: Int, wireType: Int)
    {
        appendVarint(UInt64((fieldNumber << 3) | wireType))
    }

    private mutating func appendVarint(_ value: UInt64)
    {
        var v = value
        repeat
        {
            let byte = UInt8(v & 0x7F)
            v >>= 7
            data.append(v > 0 ? byte | 0x80 : byte)
        } while v > 0
    }
}

// MARK: - Protobuf Decoder

final class ProtoDecoder
{
    private let data: Data
    private var offset: Data.Index

    init(_ data: Data)
    {
        self.data = data
        self.offset = data.startIndex
    }

    var hasMore: Bool { offset < data.endIndex }

    // Returns (fieldNumber, wireType) or nil at end of data
    func nextTag() -> (Int, Int)?
    {
        guard let tag = readVarint() else { return nil }
        return (Int(tag >> 3), Int(tag & 0x7))
    }

    func readVarint() -> UInt64?
    {
        var result: UInt64 = 0
        var shift = 0
        while offset < data.endIndex
        {
            let byte = data[offset]
            offset = data.index(after: offset)
            result |= UInt64(byte & 0x7F) << shift
            if byte & 0x80 == 0 { return result }
            shift += 7
            if shift >= 64 { return nil }
        }
        return nil
    }

    func readLengthDelimited() -> Data?
    {
        guard let len = readVarint() else { return nil }
        let count = Int(len)
        guard data.distance(from: offset, to: data.endIndex) >= count else { return nil }
        let end = data.index(offset, offsetBy: count)
        let slice = Data(data[offset..<end])
        offset = end
        return slice
    }

    func readString() -> String?
    {
        guard let bytes = readLengthDelimited() else { return nil }
        return String(data: bytes, encoding: .utf8)
    }

    func skip(wireType: Int)
    {
        switch wireType
        {
            case 0: _ = readVarint()
            case 1:
                guard data.distance(from: offset, to: data.endIndex) >= 8 else { offset = data.endIndex; return }
                offset = data.index(offset, offsetBy: 8)
            case 2: _ = readLengthDelimited()
            case 5:
                guard data.distance(from: offset, to: data.endIndex) >= 4 else { offset = data.endIndex; return }
                offset = data.index(offset, offsetBy: 4)
            default:
                offset = data.endIndex
        }
    }
}

// MARK: - Encoding Functions

func encodeStartEmailVerificationRequest(
    email: String,
    credential: Data,
    credentialType: SentryCredentialType,
    attestationDocument: String
) -> Data
{
    var enc = ProtoEncoder()
    enc.encodeString(1, email)
    enc.encodeBytes(2, credential)
    enc.encodeEnum(3, credentialType.rawValue)
    enc.encodeString(4, attestationDocument)
    return enc.build()
}

func encodeCompleteEmailVerificationRequest(token: String, code: String) -> Data
{
    var enc = ProtoEncoder()
    enc.encodeString(1, token)
    enc.encodeString(2, code)
    return enc.build()
}

func encodeGetCredentialsRequest(filter: SentryCredentialFilter) -> Data
{
    var enc = ProtoEncoder()
    enc.encodeEnum(1, filter.rawValue)
    return enc.build()
}

func encodeGetOrganizationByInviteCodeRequest(inviteCode: String) -> Data
{
    var enc = ProtoEncoder()
    enc.encodeString(1, inviteCode)
    return enc.build()
}

func encodeShareCredentialWithOrganizationRequest(
    organizationId: String,
    identity: SentryIdentity,
    inviteCode: String
) -> Data
{
    // Encode the Identity sub-message
    var identityEnc = ProtoEncoder()
    switch identity.identityCase
    {
        case .email(let email): identityEnc.encodeString(1, email)
        case .phone(let phone): identityEnc.encodeString(2, phone)
        case .none: break
    }

    var enc = ProtoEncoder()
    enc.encodeString(1, organizationId)
    enc.encodeMessage(2, identityEnc)
    enc.encodeString(3, inviteCode)
    return enc.build()
}

// MARK: - Decoding Functions

func decodeStartEmailVerificationResponse(_ data: Data) -> SentryStartEmailVerificationResponse
{
    let dec = ProtoDecoder(data)
    var response = SentryStartEmailVerificationResponse()
    while dec.hasMore
    {
        guard let (fieldNumber, wireType) = dec.nextTag() else { break }
        switch fieldNumber
        {
            case 1: response.verificationToken = dec.readString() ?? ""
            default: dec.skip(wireType: wireType)
        }
    }
    return response
}

func decodeGetCredentialsResponse(_ data: Data) -> SentryGetCredentialsResponse
{
    let dec = ProtoDecoder(data)
    var response = SentryGetCredentialsResponse()
    while dec.hasMore
    {
        guard let (fieldNumber, wireType) = dec.nextTag() else { break }
        switch fieldNumber
        {
            case 1:
                if let credData = dec.readLengthDelimited()
                {
                    response.credentials.append(decodeSentryCredential(credData))
                }
            default: dec.skip(wireType: wireType)
        }
    }
    return response
}

private func decodeSentryCredential(_ data: Data) -> SentryCredential
{
    let dec = ProtoDecoder(data)
    var cred = SentryCredential()
    while dec.hasMore
    {
        guard let (fieldNumber, wireType) = dec.nextTag() else { break }
        switch fieldNumber
        {
            case 1:
                if let identityData = dec.readLengthDelimited()
                {
                    cred.identity = decodeSentryIdentity(identityData)
                }
            case 2: cred.credential = dec.readLengthDelimited() ?? Data()
            case 3:
                if let v = dec.readVarint()
                {
                    cred.credentialType = SentryCredentialType(rawValue: Int(v)) ?? .unspecified
                }
            default: dec.skip(wireType: wireType)
        }
    }
    return cred
}

private func decodeSentryIdentity(_ data: Data) -> SentryIdentity
{
    let dec = ProtoDecoder(data)
    var identity = SentryIdentity()
    while dec.hasMore
    {
        guard let (fieldNumber, wireType) = dec.nextTag() else { break }
        switch fieldNumber
        {
            case 1:
                if let email = dec.readString()
                {
                    identity.identityCase = .email(email)
                }
            case 2:
                if let phone = dec.readString()
                {
                    identity.identityCase = .phone(phone)
                }
            default: dec.skip(wireType: wireType)
        }
    }
    return identity
}

func decodeSentryOrganization(_ data: Data) -> SentryOrganization
{
    let dec = ProtoDecoder(data)
    var org = SentryOrganization()
    while dec.hasMore
    {
        guard let (fieldNumber, wireType) = dec.nextTag() else { break }
        switch fieldNumber
        {
            case 1: org.organizationId   = dec.readString() ?? ""
            case 2: org.name             = dec.readString() ?? ""
            case 3: org.contactEmail     = dec.readString() ?? ""
            case 4: org.contactPhone     = dec.readString() ?? ""
            case 5: org.contactAddress   = dec.readString() ?? ""
            default: dec.skip(wireType: wireType)
        }
    }
    return org
}
