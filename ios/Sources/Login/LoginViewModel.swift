import Foundation
import CryptoKit

@MainActor
final class LoginViewModel: ObservableObject
{
    // MARK: - State

    enum Step { case email, code }

    @Published var step: Step = .email
    @Published var email: String = ""
    @Published var code: String = ""
    @Published var isLoading = false
    @Published var emailError: String? = nil
    @Published var codeError: String? = nil
    @Published var codeStatus: String? = nil  // success message (green)

    // When true, dismiss (return) instead of navigating to CredentialSelection
    var returnOnSuccess: Bool = false
    var onSuccess: (() -> Void)?

    private var verificationToken = ""

    // MARK: - Actions

    func sendCode()
    {
        let trimmed = email.trimmingCharacters(in: .whitespaces)
        guard isValidEmail(trimmed) else
        {
            emailError = "Please enter a valid email address."
            return
        }

        isLoading = true
        emailError = nil
        codeError  = nil
        codeStatus = nil

        Task
        {
            do
            {
                let publicKeyX963 = CryptoProvider.exportPublicKey().x963Representation
                let spkiPrefix = Data([
                    0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x02, 0x01,
                    0x06, 0x08, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x03, 0x01, 0x07, 0x03, 0x42, 0x00
                ])
                let derKey = spkiPrefix + publicKeyX963

                let response = try await VerificationService.shared.startEmailVerification(
                    email: trimmed,
                    credential: derKey,
                    credentialType: .p256,
                    attestationDocument: "TBD"
                )
                verificationToken = response.verificationToken

                isLoading  = false
                step       = .code
                codeStatus = "Verification code sent!"
            }
            catch let error as GrpcWebError
            {
                isLoading  = false
                emailError = "Network error (\(error.statusName))."
            }
            catch
            {
                isLoading  = false
                emailError = "Network error. Please try again."
            }
        }
    }

    func verifyCode()
    {
        let trimmed = code.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else
        {
            codeError = "Please enter the verification code."
            return
        }

        isLoading  = true
        codeError  = nil
        codeStatus = nil

        Task
        {
            do
            {
                try await VerificationService.shared.completeEmailVerification(
                    token: verificationToken,
                    code: trimmed
                )
                isLoading = false
                onSuccess?()
            }
            catch let error as GrpcWebError
            {
                isLoading = false
                switch error.statusCode
                {
                    case 3, 5:
                        codeError = "Invalid verification code. Please try again."
                    case 4:
                        codeError = "Verification code expired. Please request a new one."
                    default:
                        codeError = "Network error (\(error.statusName))."
                }
            }
            catch
            {
                isLoading  = false
                codeError  = "Network error. Please try again."
            }
        }
    }

    func resendCode()
    {
        step      = .email
        codeError = nil
        codeStatus = nil
        sendCode()
    }

    // MARK: - Helpers

    private func isValidEmail(_ value: String) -> Bool
    {
        let pattern = "[A-Z0-9a-z._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}"
        return value.range(of: pattern, options: .regularExpression) != nil
    }
}
