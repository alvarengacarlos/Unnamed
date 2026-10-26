package com.alvarengacarlos.unnamed.coreapi.studentenrollment;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class StudentEnrollmentExistsException extends CommonException {
    public StudentEnrollmentExistsException() {
        super("student-enrollment-exists", "Student enrollment exists", HttpStatus.CONFLICT);
    }
}
