package com.alvarengacarlos.unnamed.coreapi.teacher;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class TeacherExistsException extends CommonException {
    public TeacherExistsException() {
        super("teacher-exists", "Teacher exists", HttpStatus.CONFLICT);
    }
}
