package com.psia.pkoc;

public class ReaderModel
{
    private byte[] _protocolVersion;
    private byte[] _readerTransientPublicKey;
    private byte[] _readerIdentifier;
    private byte[] _siteIdentifier;

    public ReaderModel() {}

    public ReaderModel(byte[] readerId, byte[] siteId)
    {
        _readerIdentifier = readerId;
        _siteIdentifier = siteId;
    }

    public byte[] getProtocolVersion()
    {
        return _protocolVersion;
    }

    public void setProtocolVersion(byte[] protocolVersion)
    {
        _protocolVersion = protocolVersion;
    }

    public byte[] getReaderTransientPublicKey()
    {
        return _readerTransientPublicKey;
    }

    public void setReaderTransientPublicKey(byte[] readerTransientPublicKey)
    {
        _readerTransientPublicKey = readerTransientPublicKey;
    }

    public byte[] getReaderIdentifier()
    {
        return _readerIdentifier;
    }

    public void setReaderIdentifier(byte[] readerIdentifier)
    {
        _readerIdentifier = readerIdentifier;
    }

    public byte[] getSiteIdentifier()
    {
        return _siteIdentifier;
    }

    public void setSiteIdentifier(byte[] siteIdentifier)
    {
        _siteIdentifier = siteIdentifier;
    }
}
