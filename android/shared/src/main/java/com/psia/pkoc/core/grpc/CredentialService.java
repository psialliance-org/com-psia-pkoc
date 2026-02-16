package com.psia.pkoc.core.grpc;

import com.google.protobuf.Empty;
import com.sentryinteractive.opencredential.api.credential.GetCredentialsResponse;

import java.io.IOException;

/**
 * Singleton wrapper around the CredentialService gRPC-Web RPCs.
 */
public class CredentialService
{
    private static final String SERVICE_PATH =
            "/com.sentryinteractive.opencredential.credential.v1alpha.CredentialService";

    private static volatile CredentialService instance;

    private final GrpcWebClient client;

    private CredentialService()
    {
        client = GrpcWebClient.getInstance();
    }

    public static CredentialService getInstance()
    {
        if (instance == null)
        {
            synchronized (CredentialService.class)
            {
                if (instance == null)
                {
                    instance = new CredentialService();
                }
            }
        }
        return instance;
    }

    public GetCredentialsResponse getCredentials() throws IOException, GrpcWebException
    {
        byte[] responseBytes = client.call(SERVICE_PATH, "GetCredentials", Empty.getDefaultInstance());
        byte[] msgBytes = client.parseGrpcWebDataFrame(responseBytes);
        return GetCredentialsResponse.parseFrom(msgBytes);
    }
}