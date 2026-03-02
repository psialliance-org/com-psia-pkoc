import XCTest
@testable import PSIAExperienceApp

@MainActor
final class CredentialSelectionViewModelTests: XCTestCase
{
    // MARK: - Helpers

    private func makeCred(email: String, hexBytes: [UInt8] = [0x01, 0x02, 0x03]) -> SentryCredential
    {
        SentryCredential(
            identity: SentryIdentity(identityCase: .email(email)),
            credential: Data(hexBytes),
            credentialType: .p256
        )
    }

    private func makeVM(credentials: [SentryCredential]) -> CredentialSelectionViewModel
    {
        let vm = CredentialSelectionViewModel()
        vm.credentials = credentials
        vm.checkedIndices = Set(credentials.indices)
        return vm
    }

    // MARK: - toggle

    func test_Toggle_unchecksCheckedIndex()
    {
        // Given a VM with two credentials, both checked
        let vm = makeVM(credentials: [makeCred(email: "a@s.com"), makeCred(email: "b@s.com")])

        // When we toggle index 0
        vm.toggle(index: 0)

        // Then only index 1 should remain checked
        XCTAssertFalse(vm.checkedIndices.contains(0))
        XCTAssertTrue(vm.checkedIndices.contains(1))
    }

    func test_Toggle_checksUncheckedIndex()
    {
        // Given a VM with one credential, unchecked
        let vm = CredentialSelectionViewModel()
        vm.credentials = [makeCred(email: "a@s.com")]
        vm.checkedIndices = []

        // When we toggle index 0
        vm.toggle(index: 0)

        // Then index 0 should be checked
        XCTAssertTrue(vm.checkedIndices.contains(0))
    }

    func test_Toggle_twice_restoresOriginalState()
    {
        // Given a VM with one checked credential
        let vm = makeVM(credentials: [makeCred(email: "a@s.com")])

        // When we toggle twice
        vm.toggle(index: 0)
        vm.toggle(index: 0)

        // Then it should be back to checked
        XCTAssertTrue(vm.checkedIndices.contains(0))
    }

    // MARK: - anyChecked

    func test_AnyChecked_allUnchecked_returnsFalse()
    {
        // Given a VM with no checked indices
        let vm = CredentialSelectionViewModel()
        vm.credentials = [makeCred(email: "a@s.com")]
        vm.checkedIndices = []

        // When we check anyChecked
        // Then it should be false
        XCTAssertFalse(vm.anyChecked)
    }

    func test_AnyChecked_someChecked_returnsTrue()
    {
        // Given a VM with one checked index
        let vm = makeVM(credentials: [makeCred(email: "a@s.com")])

        // When we check anyChecked
        // Then it should be true
        XCTAssertTrue(vm.anyChecked)
    }

    // MARK: - label(for:)

    func test_Label_uniqueEmail_returnsPlainEmail()
    {
        // Given two credentials with different emails
        let vm = makeVM(credentials: [
            makeCred(email: "alice@sentry.com"),
            makeCred(email: "bob@sentry.com"),
        ])

        // When we get the label for index 0
        let label = vm.label(for: 0)

        // Then it should be just the email
        XCTAssertEqual(label, "alice@sentry.com")
    }

    func test_Label_duplicateEmail_appendsKeySuffix()
    {
        // Given two credentials with the same email but different keys
        let vm = makeVM(credentials: [
            makeCred(email: "same@sentry.com", hexBytes: [0xAA, 0xBB, 0xCC, 0xDD]),
            makeCred(email: "same@sentry.com", hexBytes: [0x11, 0x22, 0x33, 0x44]),
        ])

        // When we get the labels
        let label0 = vm.label(for: 0)
        let label1 = vm.label(for: 1)

        // Then each should have the email plus the last 6 hex chars as suffix
        // credentialHex for [0xAA, 0xBB, 0xCC, 0xDD] = "aabbccdd" → suffix(6) = "bbccdd"
        XCTAssertEqual(label0, "same@sentry.com  (…bbccdd)")
        // credentialHex for [0x11, 0x22, 0x33, 0x44] = "11223344" → suffix(6) = "223344"
        XCTAssertEqual(label1, "same@sentry.com  (…223344)")
    }

    func test_Label_duplicateEmail_suffixUsesLast6HexChars()
    {
        // Given two credentials with same email and longer keys
        let longKey: [UInt8] = [0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08]
        let vm = makeVM(credentials: [
            makeCred(email: "dup@s.com", hexBytes: longKey),
            makeCred(email: "dup@s.com", hexBytes: [0xFF]),
        ])

        // When we get the label for index 0
        let label = vm.label(for: 0)

        // Then the suffix should be the last 6 characters of the hex string
        // Full hex: "0102030405060708" → last 6 = "060708"
        XCTAssertTrue(label.hasSuffix("(…060708)"))
    }

    // MARK: - selectedCredentials

    func test_SelectedCredentials_allChecked_returnsAll()
    {
        // Given a VM with all credentials checked
        let creds = [makeCred(email: "a@s.com"), makeCred(email: "b@s.com")]
        let vm = makeVM(credentials: creds)

        // When we get selected credentials
        let selected = vm.selectedCredentials()

        // Then all should be returned in order
        XCTAssertEqual(selected.count, 2)
        XCTAssertEqual(selected[0].identity?.email, "a@s.com")
        XCTAssertEqual(selected[1].identity?.email, "b@s.com")
    }

    func test_SelectedCredentials_noneChecked_returnsEmpty()
    {
        // Given a VM with no checked indices
        let vm = CredentialSelectionViewModel()
        vm.credentials = [makeCred(email: "a@s.com")]
        vm.checkedIndices = []

        // When we get selected credentials
        let selected = vm.selectedCredentials()

        // Then it should be empty
        XCTAssertTrue(selected.isEmpty)
    }

    func test_SelectedCredentials_partialSelection_returnsSubset()
    {
        // Given three credentials with only index 1 checked
        let vm = CredentialSelectionViewModel()
        vm.credentials = [
            makeCred(email: "a@s.com"),
            makeCred(email: "b@s.com"),
            makeCred(email: "c@s.com"),
        ]
        vm.checkedIndices = [1]

        // When we get selected credentials
        let selected = vm.selectedCredentials()

        // Then only the middle credential should be returned
        XCTAssertEqual(selected.count, 1)
        XCTAssertEqual(selected[0].identity?.email, "b@s.com")
    }

    func test_SelectedCredentials_outOfBoundsIndex_isSkipped()
    {
        // Given a VM with an index that exceeds the credentials array
        let vm = CredentialSelectionViewModel()
        vm.credentials = [makeCred(email: "a@s.com")]
        vm.checkedIndices = [0, 5]

        // When we get selected credentials
        let selected = vm.selectedCredentials()

        // Then the out-of-bounds index should be safely ignored
        XCTAssertEqual(selected.count, 1)
        XCTAssertEqual(selected[0].identity?.email, "a@s.com")
    }
}
