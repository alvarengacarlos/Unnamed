package com.alvarengacarlos.unnamed.app.member;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class MemberNotFoundException extends CommonException {
    public MemberNotFoundException() {
        super("member-not-found", "Member not found", HttpStatus.NOT_FOUND);
    }
}
