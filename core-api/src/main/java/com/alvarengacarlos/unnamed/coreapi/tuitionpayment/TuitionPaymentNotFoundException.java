package com.alvarengacarlos.unnamed.coreapi.tuitionpayment;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class TuitionPaymentNotFoundException extends CommonException {
    public TuitionPaymentNotFoundException() {
        super("tuition-payment-not-found", "Tuition payment not found", HttpStatus.NOT_FOUND);
    }
}
