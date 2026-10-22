package com.alvarengacarlos.unnamed.app.member;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class MemberExistsException extends CommonException {
    public MemberExistsException() {
        super("member-exists", "Member exists", HttpStatus.CONFLICT);
    }
}
