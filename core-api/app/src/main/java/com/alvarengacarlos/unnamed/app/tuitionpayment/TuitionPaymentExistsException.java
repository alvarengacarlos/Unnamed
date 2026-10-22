package com.alvarengacarlos.unnamed.app.tuitionpayment;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class TuitionPaymentExistsException extends CommonException {
    public TuitionPaymentExistsException() {
        super("tuition-payment-exists", "Tuition payment exists", HttpStatus.CONFLICT);
    }
}
