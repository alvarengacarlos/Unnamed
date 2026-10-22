package com.alvarengacarlos.unnamed.app.grading;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class GradingNotFoundException extends CommonException {
    public GradingNotFoundException() {
        super("grading-not-found", "Grading not found", HttpStatus.NOT_FOUND);
    }
}
