package com.psia.pkoc.core.validations;

import com.psia.pkoc.core.ValidationResult;

public class SuccessResult extends ValidationResult
{
    public SuccessResult()
    {
        super(false, true, "");
    }

}
