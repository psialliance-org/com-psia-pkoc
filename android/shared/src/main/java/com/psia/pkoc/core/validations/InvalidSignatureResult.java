package com.psia.pkoc.core.validations;

import com.psia.pkoc.core.ValidationResult;

public class InvalidSignatureResult extends ValidationResult
{
    public InvalidSignatureResult()
    {
        cancelTransaction = true;
        isValid = false;
        message = "Failed to validate signature with provided public key";
    }
}
