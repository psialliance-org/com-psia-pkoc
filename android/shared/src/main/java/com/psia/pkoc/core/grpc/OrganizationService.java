package com.psia.pkoc.core.grpc;

import com.sentryinteractive.opencredential.api.organization.GetOrganizationByInviteCodeRequest;
import com.sentryinteractive.opencredential.api.organization.Organization;

import java.io.IOException;

/**
 * Singleton wrapper around the OrganizationService gRPC-Web RPCs.
 */
public class OrganizationService
{
    private static final String SERVICE_PATH =
            "/com.sentryinteractive.opencredential.organization.v1alpha.OrganizationService";

    private static volatile OrganizationService instance;

    private final GrpcWebClient client;

    private OrganizationService()
    {
        client = GrpcWebClient.getInstance();
    }

    public static OrganizationService getInstance()
    {
        if (instance == null)
        {
            synchronized (OrganizationService.class)
            {
                if (instance == null)
                {
                    instance = new OrganizationService();
                }
            }
        }
        return instance;
    }

    public Organization getOrganizationByInviteCode(String inviteCode)
            throws IOException, GrpcWebException
    {
        GetOrganizationByInviteCodeRequest request = GetOrganizationByInviteCodeRequest.newBuilder()
                .setInviteCode(inviteCode)
                .build();

        byte[] responseBytes = client.call(SERVICE_PATH, "GetOrganizationByInviteCode", request);
        byte[] msgBytes = client.parseGrpcWebDataFrame(responseBytes);
        return Organization.parseFrom(msgBytes);
    }
}
