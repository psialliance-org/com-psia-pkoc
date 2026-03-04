package com.psia.pkoc.core.grpc;

import com.google.protobuf.MessageLite;

import java.io.IOException;
import java.nio.ByteBuffer;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Low-level gRPC-Web client over OkHttp. Singleton — obtain via {@link #getInstance()}.
 */
public class GrpcWebClient
{
    private static final String BASE_URL = "https://api.opencredential.sentryinteractive.com";
    private static final MediaType GRPC_WEB_MEDIA_TYPE = MediaType.parse("application/grpc-web");

    private static volatile GrpcWebClient instance;

    private final OkHttpClient httpClient;

    private GrpcWebClient()
    {
        httpClient = new OkHttpClient.Builder()
                .addInterceptor(new GrpcWebInterceptor())
                .build();
    }

    public static GrpcWebClient getInstance()
    {
        if (instance == null)
        {
            synchronized (GrpcWebClient.class)
            {
                if (instance == null)
                {
                    instance = new GrpcWebClient();
                }
            }
        }
        return instance;
    }

    /**
     * Execute a gRPC-Web unary call.
     *
     * @param servicePath fully-qualified service path, e.g.
     *                    "/com.sentryinteractive.opencredential.verification.v1alpha.VerificationService"
     * @param method      RPC method name, e.g. "StartEmailVerification"
     * @param request     protobuf request message
     * @return raw response bytes (the full gRPC-Web frame including data + trailer frames)
     */
    public byte[] call(String servicePath, String method, MessageLite request)
            throws IOException, GrpcWebException
    {
        byte[] body = frameGrpcWeb(request);

        Request httpRequest = new Request.Builder()
                .url(BASE_URL + servicePath + "/" + method)
                .post(RequestBody.create(body, GRPC_WEB_MEDIA_TYPE))
                .addHeader("Accept", "application/grpc-web")
                .addHeader("X-Grpc-Web", "1")
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute())
        {
            byte[] responseBytes = response.body() != null ? response.body().bytes() : new byte[0];

            int status = extractGrpcStatus(response, responseBytes);
            if (status != 0)
            {
                throw new GrpcWebException(status, extractGrpcMessage(response, responseBytes));
            }
            if (!response.isSuccessful())
            {
                throw new IOException("HTTP error: " + response.code());
            }
            return responseBytes;
        }
    }

    // --- framing / parsing helpers ---

    public byte[] frameGrpcWeb(MessageLite message)
    {
        byte[] msgBytes = message.toByteArray();
        ByteBuffer buf = ByteBuffer.allocate(5 + msgBytes.length);
        buf.put((byte) 0x00);        // no compression
        buf.putInt(msgBytes.length);  // 4-byte big-endian length
        buf.put(msgBytes);
        return buf.array();
    }

    public byte[] parseGrpcWebDataFrame(byte[] responseBytes) throws IOException
    {
        if (responseBytes.length < 5)
        {
            throw new IOException("gRPC-Web response too short: " + responseBytes.length);
        }
        ByteBuffer buf = ByteBuffer.wrap(responseBytes);
        byte flags = buf.get();
        int length = buf.getInt();
        if ((flags & 0x80) != 0)
        {
            throw new IOException("Expected data frame, got trailer frame");
        }
        if (responseBytes.length < 5 + length)
        {
            throw new IOException("gRPC-Web response truncated");
        }
        byte[] msgBytes = new byte[length];
        buf.get(msgBytes);
        return msgBytes;
    }

    private int extractGrpcStatus(Response response, byte[] responseBytes)
    {
        String header = response.header("grpc-status");
        if (header != null)
        {
            return Integer.parseInt(header);
        }
        int offset = 0;
        while (offset + 5 <= responseBytes.length)
        {
            byte flags = responseBytes[offset];
            int len = ByteBuffer.wrap(responseBytes, offset + 1, 4).getInt();
            if ((flags & 0x80) != 0 && offset + 5 + len <= responseBytes.length)
            {
                String trailers = new String(responseBytes, offset + 5, len);
                for (String line : trailers.split("\r\n"))
                {
                    if (line.startsWith("grpc-status:"))
                    {
                        return Integer.parseInt(line.substring("grpc-status:".length()).trim());
                    }
                }
            }
            offset += 5 + len;
        }
        return response.isSuccessful() ? 0 : 2;
    }

    private String extractGrpcMessage(Response response, byte[] responseBytes)
    {
        String header = response.header("grpc-message");
        if (header != null) return header;

        int offset = 0;
        while (offset + 5 <= responseBytes.length)
        {
            byte flags = responseBytes[offset];
            int len = ByteBuffer.wrap(responseBytes, offset + 1, 4).getInt();
            if ((flags & 0x80) != 0 && offset + 5 + len <= responseBytes.length)
            {
                String trailers = new String(responseBytes, offset + 5, len);
                for (String line : trailers.split("\r\n"))
                {
                    if (line.startsWith("grpc-message:"))
                    {
                        return line.substring("grpc-message:".length()).trim();
                    }
                }
            }
            offset += 5 + len;
        }
        return "";
    }
}