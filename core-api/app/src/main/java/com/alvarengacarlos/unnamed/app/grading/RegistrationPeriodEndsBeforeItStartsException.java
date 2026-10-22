package com.alvarengacarlos.unnamed.app.grading;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class RegistrationPeriodEndsBeforeItStartsException extends CommonException {
    public RegistrationPeriodEndsBeforeItStartsException() {
        super(
            "registration-period-ends-before-it-starts",
            "Registration period ends before it starts",
            HttpStatus.BAD_REQUEST);
    }
}
