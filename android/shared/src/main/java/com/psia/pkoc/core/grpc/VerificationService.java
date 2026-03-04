package com.psia.pkoc.core.grpc;

import com.google.protobuf.ByteString;
import com.sentryinteractive.opencredential.api.common.CredentialType;
import com.sentryinteractive.opencredential.api.verification.CompleteEmailVerificationRequest;
import com.sentryinteractive.opencredential.api.verification.StartEmailVerificationRequest;
import com.sentryinteractive.opencredential.api.verification.StartEmailVerificationResponse;

import java.io.IOException;

/**
 * Singleton wrapper around the VerificationService gRPC-Web RPCs.
 */
public class VerificationService
{
    private static final String SERVICE_PATH =
            "/com.sentryinteractive.opencredential.verification.v1alpha.VerificationService";

    private static volatile VerificationService instance;

    private final GrpcWebClient client;

    private VerificationService()
    {
        client = GrpcWebClient.getInstance();
    }

    public static VerificationService getInstance()
    {
        if (instance == null)
        {
            synchronized (VerificationService.class)
            {
                if (instance == null)
                {
                    instance = new VerificationService();
                }
            }
        }
        return instance;
    }

    public StartEmailVerificationResponse startEmailVerification(
            String email,
            byte[] publicKeyDer,
            CredentialType credentialType,
            String attestationDocument) throws IOException, GrpcWebException
    {
        StartEmailVerificationRequest request = StartEmailVerificationRequest.newBuilder()
                .setEmail(email)
                .setCredential(ByteString.copyFrom(publicKeyDer))
                .setCredentialType(credentialType)
                .setAttestationDocument(attestationDocument)
                .build();

        byte[] responseBytes = client.call(SERVICE_PATH, "StartEmailVerification", request);
        byte[] msgBytes = client.parseGrpcWebDataFrame(responseBytes);
        return StartEmailVerificationResponse.parseFrom(msgBytes);
    }

    public void completeEmailVerification(String token, String code)
            throws IOException, GrpcWebException
    {
        CompleteEmailVerificationRequest request = CompleteEmailVerificationRequest.newBuilder()
                .setToken(token)
                .setCode(code)
                .build();

        client.call(SERVICE_PATH, "CompleteEmailVerification", request);
    }
}