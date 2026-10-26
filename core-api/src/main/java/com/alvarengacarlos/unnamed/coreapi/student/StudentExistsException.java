package com.alvarengacarlos.unnamed.coreapi.student;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class StudentExistsException extends CommonException {
    public StudentExistsException() {
        super("student-exists", "Student exists", HttpStatus.CONFLICT);
    }
}
