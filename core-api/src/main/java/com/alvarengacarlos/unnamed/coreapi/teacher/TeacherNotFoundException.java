package com.alvarengacarlos.unnamed.coreapi.teacher;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class TeacherNotFoundException extends CommonException {
    public TeacherNotFoundException() {
        super("teacher-not-found", "Teacher not found", HttpStatus.NOT_FOUND);
    }
}
