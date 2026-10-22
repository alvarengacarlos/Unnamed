package com.alvarengacarlos.unnamed.app.student;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class StudentExistsException extends CommonException {
    public StudentExistsException() {
        super("student-exists", "Student exists", HttpStatus.CONFLICT);
    }
}
