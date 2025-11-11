package com.psia.pkoc.core;


public class ValidationResult
{
    public Boolean cancelTransaction = true;
    public Boolean isValid = true;
    public String message = "";

    public ValidationResult()
    {
    }

    public ValidationResult(boolean _cancelTransaction, boolean _isValid, String _message)
    {
        cancelTransaction = _cancelTransaction;
        isValid = _isValid;
        message = _message;
    }
}
