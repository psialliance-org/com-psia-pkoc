package com.psia.pkoc.core.validations;

import com.psia.pkoc.core.ValidationResult;

public class UnrecognizedReaderResult extends ValidationResult
{
    public UnrecognizedReaderResult()
    {
        cancelTransaction = true;
        isValid = false;
        message = "The reader was not recognized.";
    }
}
