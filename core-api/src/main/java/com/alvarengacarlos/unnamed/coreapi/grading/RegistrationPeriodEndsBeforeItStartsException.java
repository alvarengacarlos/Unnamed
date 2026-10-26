package com.alvarengacarlos.unnamed.coreapi.grading;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class RegistrationPeriodEndsBeforeItStartsException extends CommonException {
    public RegistrationPeriodEndsBeforeItStartsException() {
        super(
            "registration-period-ends-before-it-starts",
            "Registration period ends before it starts",
            HttpStatus.BAD_REQUEST);
    }
}
