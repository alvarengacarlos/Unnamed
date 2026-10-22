package com.alvarengacarlos.unnamed.app.teacher;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class TeacherNotFoundException extends CommonException {
    public TeacherNotFoundException() {
        super("teacher-not-found", "Teacher not found", HttpStatus.NOT_FOUND);
    }
}
