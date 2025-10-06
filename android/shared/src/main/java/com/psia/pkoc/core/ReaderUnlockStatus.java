package com.psia.pkoc.core;

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
