package com.alvarengacarlos.unnamed.coreapi.tuitionpayment;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class TuitionPaymentExistsException extends CommonException {
    public TuitionPaymentExistsException() {
        super("tuition-payment-exists", "Tuition payment exists", HttpStatus.CONFLICT);
    }
}
