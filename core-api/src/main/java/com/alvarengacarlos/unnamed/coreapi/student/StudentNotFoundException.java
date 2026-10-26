package com.alvarengacarlos.unnamed.coreapi.student;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class StudentNotFoundException extends CommonException {
    public StudentNotFoundException() {
        super("student-not-found", "Student not found", HttpStatus.NOT_FOUND);
    }
}
