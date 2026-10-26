package com.alvarengacarlos.unnamed.coreapi.grading;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class GradingNotFoundException extends CommonException {
    public GradingNotFoundException() {
        super("grading-not-found", "Grading not found", HttpStatus.NOT_FOUND);
    }
}
