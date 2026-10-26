package com.alvarengacarlos.unnamed.coreapi.clazz;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class ClazzNotFoundException extends CommonException {
    public ClazzNotFoundException() {
        super("clazz-not-found", "Clazz not found", HttpStatus.NOT_FOUND);
    }
}
