package com.alvarengacarlos.unnamed.coreapi.tuition;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class TuitionNotFoundException extends CommonException {
    public TuitionNotFoundException() {
        super("tuition-not-found", "Tuition not found", HttpStatus.NOT_FOUND);
    }
}
