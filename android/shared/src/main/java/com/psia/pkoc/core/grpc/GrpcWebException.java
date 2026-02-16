package com.psia.pkoc.core.grpc;

public class GrpcWebException extends Exception
{
    private final int statusCode;
    private final String grpcMessage;

    public GrpcWebException(int statusCode, String grpcMessage)
    {
        super("gRPC status " + statusCode + ": " + grpcMessage);
        this.statusCode = statusCode;
        this.grpcMessage = grpcMessage;
    }

    public int getStatusCode()
    {
        return statusCode;
    }

    public String getGrpcMessage()
    {
        return grpcMessage;
    }

    public String statusName()
    {
        switch (statusCode)
        {
            case 0: return "OK";
            case 1: return "CANCELLED";
            case 2: return "UNKNOWN";
            case 3: return "INVALID_ARGUMENT";
            case 4: return "DEADLINE_EXCEEDED";
            case 5: return "NOT_FOUND";
            case 6: return "ALREADY_EXISTS";
            case 7: return "PERMISSION_DENIED";
            case 8: return "RESOURCE_EXHAUSTED";
            case 9: return "FAILED_PRECONDITION";
            case 10: return "ABORTED";
            case 11: return "OUT_OF_RANGE";
            case 12: return "UNIMPLEMENTED";
            case 13: return "INTERNAL";
            case 14: return "UNAVAILABLE";
            case 15: return "DATA_LOSS";
            case 16: return "UNAUTHENTICATED";
            default: return "STATUS_" + statusCode;
        }
    }
}