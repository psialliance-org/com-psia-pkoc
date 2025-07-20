package com.pkoc.readersimulator;

/** @noinspection unused*/

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
