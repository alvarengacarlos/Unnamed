package com.alvarengacarlos.unnamed.app.student;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class StudentNotFoundException extends CommonException {
    public StudentNotFoundException() {
        super("student-not-found", "Student not found", HttpStatus.NOT_FOUND);
    }
}
