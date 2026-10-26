package com.alvarengacarlos.unnamed.coreapi.member;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class MemberExistsException extends CommonException {
    public MemberExistsException() {
        super("member-exists", "Member exists", HttpStatus.CONFLICT);
    }
}
