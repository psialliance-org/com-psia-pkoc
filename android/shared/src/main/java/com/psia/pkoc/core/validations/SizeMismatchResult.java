package com.psia.pkoc.core.validations;

import com.psia.pkoc.core.ValidationResult;

public class SizeMismatchResult extends ValidationResult
{
    public SizeMismatchResult(int _actualSize, int[] _expectedSizes)
    {
        for (int expectedSize : _expectedSizes)
        {
            if (_actualSize == expectedSize)
                return;
        }

        cancelTransaction = true;
        isValid = false;
        message = "Size mismatch in parsing TLV";
    }

    public SizeMismatchResult(int _actualSize, int _expectedSize)
    {
        if (_actualSize != _expectedSize)
        {
            cancelTransaction = true;
            isValid = false;
            message = "Size mismatch in parsing TLV";
        }
    }

    public SizeMismatchResult(int _actualSize, int _expectedMinimum, int _expectedMaximum)
    {
        if (_actualSize < _expectedMinimum || _actualSize > _expectedMaximum)
        {
            cancelTransaction = true;
            isValid = false;
            message = "Size mismatch in parsing TLV";
        }
    }
}
