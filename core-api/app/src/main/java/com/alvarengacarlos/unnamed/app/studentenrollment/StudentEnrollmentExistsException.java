package com.alvarengacarlos.unnamed.app.studentenrollment;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class StudentEnrollmentExistsException extends CommonException {
    public StudentEnrollmentExistsException() {
        super("student-enrollment-exists", "Student enrollment exists", HttpStatus.CONFLICT);
    }
}
