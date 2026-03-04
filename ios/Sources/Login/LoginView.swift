import SwiftUI

/// Email + 2FA verification screen.
/// Used both as the initial launch gate and as a sheet from CredentialSelectionView.
struct LoginView: View
{
    @StateObject private var vm = LoginViewModel()

    /// When true the view is presented as a sheet; success calls onSuccess and dismisses.
    var returnOnSuccess: Bool = false
    var onSuccess: (() -> Void)?

    var body: some View
    {
        ScrollView
        {
            VStack(spacing: 0)
            {
                // Logo
                Image(uiImage: UIImage(named: ProductImages.PSIA_Logo_Typographic)!)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(height: 80)
                    .padding(.top, 48)
                    .padding(.bottom, 16)

                Text("Email Verification")
                    .font(.title2)
                    .bold()
                    .padding(.bottom, 32)

                if vm.step == .email
                {
                    emailStep
                }
                else
                {
                    codeStep
                }

                if vm.isLoading
                {
                    ProgressView()
                        .padding(.top, 16)
                }
            }
            .padding(.horizontal, 24)
        }
        .onAppear
        {
            vm.returnOnSuccess = returnOnSuccess
            vm.onSuccess = onSuccess
        }
    }

    // MARK: - Email Step

    private var emailStep: some View
    {
        VStack(alignment: .leading, spacing: 12)
        {
            TextField("Email address", text: $vm.email)
                .keyboardType(.emailAddress)
                .autocapitalization(.none)
                .textContentType(.emailAddress)
                .padding()
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.gray.opacity(0.4)))

            if let error = vm.emailError
            {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
            }

            Button("Send Verification Code")
            {
                vm.sendCode()
            }
            .buttonStyle(PrimaryButtonStyle())
            .disabled(vm.isLoading)
        }
    }

    // MARK: - Code Step

    private var codeStep: some View
    {
        VStack(alignment: .leading, spacing: 12)
        {
            Text("Enter the verification code sent to your email")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)

            TextField("6-digit code", text: $vm.code)
                .keyboardType(.numberPad)
                .textContentType(.oneTimeCode)
                .padding()
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.gray.opacity(0.4)))

            if let status = vm.codeStatus
            {
                Text(status)
                    .font(.caption)
                    .foregroundColor(.green)
            }

            if let error = vm.codeError
            {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
            }

            Button("Verify") { vm.verifyCode() }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(vm.isLoading)

            Button("Resend Code") { vm.resendCode() }
                .buttonStyle(SecondaryButtonStyle())
                .disabled(vm.isLoading)
        }
    }
}

// MARK: - Button Styles

private struct PrimaryButtonStyle: ButtonStyle
{
    func makeBody(configuration: Configuration) -> some View
    {
        configuration.label
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.accentColor.opacity(configuration.isPressed ? 0.8 : 1))
            .foregroundColor(.white)
            .cornerRadius(8)
    }
}

private struct SecondaryButtonStyle: ButtonStyle
{
    func makeBody(configuration: Configuration) -> some View
    {
        configuration.label
            .frame(maxWidth: .infinity)
            .padding()
            .foregroundColor(.accentColor)
    }
}
