package com.alvarengacarlos.unnamed.app.common;

import org.springframework.http.HttpStatus;

public class BulkHasDuplicatedItemException extends CommonException {
    public BulkHasDuplicatedItemException() {
        super("bulk-has-duplicated-item", "Bulk has duplicated item", HttpStatus.BAD_REQUEST);
    }
}
