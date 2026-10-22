package com.alvarengacarlos.unnamed.app.tuition;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class TuitionNotFoundException extends CommonException {
    public TuitionNotFoundException() {
        super("tuition-not-found", "Tuition not found", HttpStatus.NOT_FOUND);
    }
}
