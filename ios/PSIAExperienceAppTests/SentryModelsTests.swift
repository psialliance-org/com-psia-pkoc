import XCTest
@testable import PSIAExperienceApp

final class SentryModelsTests: XCTestCase
{
    // MARK: - SentryCredential.credentialHex

    func test_CredentialHex_formatsAsLowercaseHex()
    {
        // Given a credential with known bytes
        let cred = SentryCredential(credential: Data([0x00, 0xFF, 0x0A, 0xBC]))

        // When we read the hex representation
        let hex = cred.credentialHex

        // Then it should be lowercase, zero-padded hex
        XCTAssertEqual(hex, "00ff0abc")
    }

    func test_CredentialHex_emptyCredential_returnsEmptyString()
    {
        // Given a credential with no bytes
        let cred = SentryCredential()

        // When we read the hex representation
        let hex = cred.credentialHex

        // Then it should be empty
        XCTAssertEqual(hex, "")
    }

    func test_CredentialHex_singleByte()
    {
        // Given a credential with a single byte
        let cred = SentryCredential(credential: Data([0x0F]))

        // When we read the hex representation
        // Then the single byte should be zero-padded
        XCTAssertEqual(cred.credentialHex, "0f")
    }

    // MARK: - SentryIdentity

    func test_SentryIdentity_emailCase_exposesEmailOnly()
    {
        // Given an identity with an email
        let id = SentryIdentity(identityCase: .email("user@sentry.com"))

        // When we access its properties
        // Then email returns the value and phone returns nil
        XCTAssertEqual(id.email, "user@sentry.com")
        XCTAssertNil(id.phone)
    }

    func test_SentryIdentity_phoneCase_exposesPhoneOnly()
    {
        // Given an identity with a phone number
        let id = SentryIdentity(identityCase: .phone("+1234567890"))

        // When we access its properties
        // Then phone returns the value and email returns nil
        XCTAssertNil(id.email)
        XCTAssertEqual(id.phone, "+1234567890")
    }

    func test_SentryIdentity_noneCase_returnsNilForBoth()
    {
        // Given an identity with no case set
        let id = SentryIdentity()

        // When we access its properties
        // Then both return nil
        XCTAssertNil(id.email)
        XCTAssertNil(id.phone)
    }

    // MARK: - SentryCredentialType

    func test_CredentialType_rawValues()
    {
        // Given the credential type enum
        // When we check raw values
        // Then they should match the protobuf spec
        XCTAssertEqual(SentryCredentialType.unspecified.rawValue, 0)
        XCTAssertEqual(SentryCredentialType.p256.rawValue, 1)
        XCTAssertEqual(SentryCredentialType.p384.rawValue, 2)
        XCTAssertEqual(SentryCredentialType.ed25519.rawValue, 4)
    }

    // MARK: - Encode: StartEmailVerificationRequest

    func test_EncodeStartEmailVerification_roundTrip()
    {
        // Given a verification request with all fields
        let email = "test@sentry.com"
        let credential = Data([0xDE, 0xAD, 0xBE, 0xEF])
        let credType = SentryCredentialType.p256

        // When we encode it
        let encoded = encodeStartEmailVerificationRequest(
            email: email,
            credential: credential,
            credentialType: credType,
            attestationDocument: "doc123"
        )

        // Then decoding should recover all fields
        let dec = ProtoDecoder(encoded)
        var decodedEmail = ""
        var decodedCred = Data()
        var decodedType = 0
        var decodedDoc = ""

        while dec.hasMore
        {
            guard let (field, wireType) = dec.nextTag() else { break }
            switch field
            {
                case 1: decodedEmail = dec.readString() ?? ""
                case 2: decodedCred = dec.readLengthDelimited() ?? Data()
                case 3: decodedType = Int(dec.readVarint() ?? 0)
                case 4: decodedDoc = dec.readString() ?? ""
                default: dec.skip(wireType: wireType)
            }
        }

        XCTAssertEqual(decodedEmail, email)
        XCTAssertEqual(decodedCred, credential)
        XCTAssertEqual(decodedType, credType.rawValue)
        XCTAssertEqual(decodedDoc, "doc123")
    }

    // MARK: - Encode: CompleteEmailVerificationRequest

    func test_EncodeCompleteEmailVerification_roundTrip()
    {
        // Given a token and code
        // When we encode then decode
        let encoded = encodeCompleteEmailVerificationRequest(token: "tok-abc", code: "123456")
        let dec = ProtoDecoder(encoded)
        var token = ""
        var code = ""

        while dec.hasMore
        {
            guard let (field, wireType) = dec.nextTag() else { break }
            switch field
            {
                case 1: token = dec.readString() ?? ""
                case 2: code = dec.readString() ?? ""
                default: dec.skip(wireType: wireType)
            }
        }

        // Then both fields should be recovered
        XCTAssertEqual(token, "tok-abc")
        XCTAssertEqual(code, "123456")
    }

    // MARK: - Encode: GetCredentialsRequest

    func test_EncodeGetCredentials_sameKeyFilter()
    {
        // Given a sameKey filter
        // When we encode it
        let encoded = encodeGetCredentialsRequest(filter: .sameKey)

        // Then field 1 should contain the enum value 2
        let dec = ProtoDecoder(encoded)
        guard let (field, _) = dec.nextTag() else { return XCTFail("No tag") }
        XCTAssertEqual(field, 1)
        XCTAssertEqual(dec.readVarint(), UInt64(SentryCredentialFilter.sameKey.rawValue))
    }

    func test_EncodeGetCredentials_unspecifiedFilter_producesEmptyBody()
    {
        // Given an unspecified filter (value 0)
        // When we encode it
        let encoded = encodeGetCredentialsRequest(filter: .unspecified)

        // Then the body should be empty (protobuf skips default values)
        XCTAssertTrue(encoded.isEmpty)
    }

    // MARK: - Decode: StartEmailVerificationResponse

    func test_DecodeStartEmailVerificationResponse_extractsToken()
    {
        // Given a protobuf-encoded response with a verification token
        var enc = ProtoEncoder()
        enc.encodeString(1, "my-token")

        // When we decode it
        let response = decodeStartEmailVerificationResponse(enc.build())

        // Then the token should be extracted
        XCTAssertEqual(response.verificationToken, "my-token")
    }

    func test_DecodeStartEmailVerificationResponse_emptyData_defaultsToEmpty()
    {
        // Given empty data
        // When we decode it
        let response = decodeStartEmailVerificationResponse(Data())

        // Then the token should default to empty
        XCTAssertEqual(response.verificationToken, "")
    }

    // MARK: - Decode: GetCredentialsResponse

    func test_DecodeGetCredentialsResponse_emailCredential()
    {
        // Given encoded protobuf with one email credential
        var identityEnc = ProtoEncoder()
        identityEnc.encodeString(1, "user@sentry.com")

        var credEnc = ProtoEncoder()
        credEnc.encodeMessage(1, identityEnc)
        credEnc.encodeBytes(2, Data([0x01, 0x02, 0x03]))
        credEnc.encodeEnum(3, SentryCredentialType.p256.rawValue)

        var responseEnc = ProtoEncoder()
        responseEnc.encodeMessage(1, credEnc)

        // When we decode it
        let response = decodeGetCredentialsResponse(responseEnc.build())

        // Then it should contain one credential with the email identity
        XCTAssertEqual(response.credentials.count, 1)
        let cred = response.credentials[0]
        XCTAssertEqual(cred.identity?.email, "user@sentry.com")
        XCTAssertNil(cred.identity?.phone)
        XCTAssertEqual(cred.credential, Data([0x01, 0x02, 0x03]))
        XCTAssertEqual(cred.credentialType, .p256)
    }

    func test_DecodeGetCredentialsResponse_phoneCredential()
    {
        // Given encoded protobuf with a phone identity credential
        var identityEnc = ProtoEncoder()
        identityEnc.encodeString(2, "+1234567890")

        var credEnc = ProtoEncoder()
        credEnc.encodeMessage(1, identityEnc)
        credEnc.encodeBytes(2, Data([0xFF]))
        credEnc.encodeEnum(3, SentryCredentialType.ed25519.rawValue)

        var responseEnc = ProtoEncoder()
        responseEnc.encodeMessage(1, credEnc)

        // When we decode it
        let response = decodeGetCredentialsResponse(responseEnc.build())

        // Then it should have a phone identity
        XCTAssertEqual(response.credentials.count, 1)
        XCTAssertEqual(response.credentials[0].identity?.phone, "+1234567890")
        XCTAssertNil(response.credentials[0].identity?.email)
        XCTAssertEqual(response.credentials[0].credentialType, .ed25519)
    }

    func test_DecodeGetCredentialsResponse_multipleCredentials()
    {
        // Given two encoded credentials
        var cred1 = ProtoEncoder()
        var id1 = ProtoEncoder()
        id1.encodeString(1, "a@sentry.com")
        cred1.encodeMessage(1, id1)
        cred1.encodeBytes(2, Data([0x0A]))

        var cred2 = ProtoEncoder()
        var id2 = ProtoEncoder()
        id2.encodeString(1, "b@sentry.com")
        cred2.encodeMessage(1, id2)
        cred2.encodeBytes(2, Data([0x0B]))

        var responseEnc = ProtoEncoder()
        responseEnc.encodeMessage(1, cred1)
        responseEnc.encodeMessage(1, cred2)

        // When we decode them
        let response = decodeGetCredentialsResponse(responseEnc.build())

        // Then both credentials should be present in order
        XCTAssertEqual(response.credentials.count, 2)
        XCTAssertEqual(response.credentials[0].identity?.email, "a@sentry.com")
        XCTAssertEqual(response.credentials[1].identity?.email, "b@sentry.com")
    }

    func test_DecodeGetCredentialsResponse_emptyData_returnsEmptyList()
    {
        // Given empty data
        // When we decode it
        let response = decodeGetCredentialsResponse(Data())

        // Then credentials should be empty
        XCTAssertTrue(response.credentials.isEmpty)
    }

    // MARK: - Decode: Organization

    func test_DecodeSentryOrganization_allFields()
    {
        // Given encoded organization with all fields
        var enc = ProtoEncoder()
        enc.encodeString(1, "org-123")
        enc.encodeString(2, "Acme Corp")
        enc.encodeString(3, "info@acme.com")
        enc.encodeString(4, "+1999000111")
        enc.encodeString(5, "123 Main St")

        // When we decode it
        let org = decodeSentryOrganization(enc.build())

        // Then all fields should match
        XCTAssertEqual(org.organizationId, "org-123")
        XCTAssertEqual(org.name, "Acme Corp")
        XCTAssertEqual(org.contactEmail, "info@acme.com")
        XCTAssertEqual(org.contactPhone, "+1999000111")
        XCTAssertEqual(org.contactAddress, "123 Main St")
    }

    func test_DecodeSentryOrganization_partialFields_defaultsToEmpty()
    {
        // Given encoded organization with only id and name
        var enc = ProtoEncoder()
        enc.encodeString(1, "org-456")
        enc.encodeString(2, "Solo Inc")

        // When we decode it
        let org = decodeSentryOrganization(enc.build())

        // Then missing fields should default to empty strings
        XCTAssertEqual(org.organizationId, "org-456")
        XCTAssertEqual(org.name, "Solo Inc")
        XCTAssertEqual(org.contactEmail, "")
        XCTAssertEqual(org.contactPhone, "")
        XCTAssertEqual(org.contactAddress, "")
    }

    func test_DecodeSentryOrganization_unknownFields_skippedGracefully()
    {
        // Given encoded data with an extra unknown field 99
        var enc = ProtoEncoder()
        enc.encodeString(1, "org-skip")
        enc.encodeEnum(99, 42)
        enc.encodeString(2, "Name")

        // When we decode it
        let org = decodeSentryOrganization(enc.build())

        // Then known fields should be parsed correctly
        XCTAssertEqual(org.organizationId, "org-skip")
        XCTAssertEqual(org.name, "Name")
    }

    // MARK: - Encode: ShareCredentialWithOrganization

    func test_EncodeShareCredential_emailIdentity_roundTrip()
    {
        // Given an email identity and org details
        let identity = SentryIdentity(identityCase: .email("share@sentry.com"))

        // When we encode a share request
        let encoded = encodeShareCredentialWithOrganizationRequest(
            organizationId: "org-1",
            identity: identity,
            inviteCode: "inv-abc"
        )

        // Then decoding should recover orgId, email, and inviteCode
        let dec = ProtoDecoder(encoded)
        var orgId = ""
        var email = ""
        var inviteCode = ""

        while dec.hasMore
        {
            guard let (field, wireType) = dec.nextTag() else { break }
            switch field
            {
                case 1: orgId = dec.readString() ?? ""
                case 2:
                    if let identityData = dec.readLengthDelimited()
                    {
                        let idDec = ProtoDecoder(identityData)
                        while idDec.hasMore
                        {
                            guard let (idField, idWt) = idDec.nextTag() else { break }
                            if idField == 1 { email = idDec.readString() ?? "" }
                            else { idDec.skip(wireType: idWt) }
                        }
                    }
                case 3: inviteCode = dec.readString() ?? ""
                default: dec.skip(wireType: wireType)
            }
        }

        XCTAssertEqual(orgId, "org-1")
        XCTAssertEqual(email, "share@sentry.com")
        XCTAssertEqual(inviteCode, "inv-abc")
    }

    func test_EncodeShareCredential_phoneIdentity_roundTrip()
    {
        // Given a phone identity
        let identity = SentryIdentity(identityCase: .phone("+4400000000"))

        // When we encode a share request
        let encoded = encodeShareCredentialWithOrganizationRequest(
            organizationId: "org-2",
            identity: identity,
            inviteCode: "inv-xyz"
        )

        // Then the phone field should be present in the identity submessage
        let dec = ProtoDecoder(encoded)
        var phone = ""

        while dec.hasMore
        {
            guard let (field, wireType) = dec.nextTag() else { break }
            if field == 2, let identityData = dec.readLengthDelimited()
            {
                let idDec = ProtoDecoder(identityData)
                while idDec.hasMore
                {
                    guard let (idField, idWt) = idDec.nextTag() else { break }
                    if idField == 2 { phone = idDec.readString() ?? "" }
                    else { idDec.skip(wireType: idWt) }
                }
            }
            else { dec.skip(wireType: wireType) }
        }

        XCTAssertEqual(phone, "+4400000000")
    }

    func test_EncodeShareCredential_noneIdentity_omitsSubmessage()
    {
        // Given a .none identity
        let identity = SentryIdentity(identityCase: .none)

        // When we encode a share request
        let encoded = encodeShareCredentialWithOrganizationRequest(
            organizationId: "org-3",
            identity: identity,
            inviteCode: "inv-123"
        )

        // Then field 2 (identity) should not appear since the inner message is empty
        let dec = ProtoDecoder(encoded)
        var foundIdentityField = false

        while dec.hasMore
        {
            guard let (field, wireType) = dec.nextTag() else { break }
            if field == 2 { foundIdentityField = true }
            dec.skip(wireType: wireType)
        }

        XCTAssertFalse(foundIdentityField)
    }

    // MARK: - Encode: GetOrganizationByInviteCode

    func test_EncodeGetOrganizationByInviteCode()
    {
        // Given an invite code
        // When we encode the request
        let encoded = encodeGetOrganizationByInviteCodeRequest(inviteCode: "abc-def")

        // Then field 1 should contain the invite code
        let dec = ProtoDecoder(encoded)
        guard let (field, _) = dec.nextTag() else { return XCTFail("No tag") }
        XCTAssertEqual(field, 1)
        XCTAssertEqual(dec.readString(), "abc-def")
    }
}
