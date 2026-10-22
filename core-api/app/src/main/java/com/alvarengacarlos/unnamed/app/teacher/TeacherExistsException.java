package com.alvarengacarlos.unnamed.app.teacher;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class TeacherExistsException extends CommonException {
    public TeacherExistsException() {
        super("teacher-exists", "Teacher exists", HttpStatus.CONFLICT);
    }
}
