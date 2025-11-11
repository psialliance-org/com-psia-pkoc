package com.psia.pkoc.core.validations;

import com.psia.pkoc.core.ValidationResult;

public class UnexpectedPacketResult extends ValidationResult
{
    public UnexpectedPacketResult()
    {
        cancelTransaction = false;
        isValid = false;
        message = "A packet was received at an expected point in the PKOC handshake.";
    }
}
