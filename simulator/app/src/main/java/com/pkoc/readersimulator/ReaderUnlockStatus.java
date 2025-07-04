package com.pkoc.readersimulator;

public enum ReaderUnlockStatus
{
    Unknown,
    CompletedTransaction,
    AccessDenied,
    AccessGranted,
    DecryptionErrorGcm,
    InvalidSecurityStatus,
    SignatureInvalid,
    DecryptionErrorCcm,
    Unrecognized
}
