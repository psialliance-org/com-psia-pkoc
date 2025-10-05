package com.psia.pkoc.core.validations;

import com.psia.pkoc.core.ValidationResult;

public class ValidatedBeforeCompleteResult extends ValidationResult
{
    public ValidatedBeforeCompleteResult()
    {
        cancelTransaction = false;
        isValid = false;
        message = "A message was validated before the transaction was completed. This is allowed in the specification, but may signal that a reader is not sending required TLVs for this transaction.";
    }
}
