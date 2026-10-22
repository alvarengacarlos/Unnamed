package com.alvarengacarlos.unnamed.app.studentenrollment;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class StudentEnrollmentNotFoundException extends CommonException {
    public StudentEnrollmentNotFoundException() {
        super("student-enrollment-not-found", "Student enrollment not found", HttpStatus.NOT_FOUND);
    }
}
