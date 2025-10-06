package com.psia.pkoc;

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
