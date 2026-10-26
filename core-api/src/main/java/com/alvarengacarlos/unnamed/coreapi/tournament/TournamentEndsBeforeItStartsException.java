package com.alvarengacarlos.unnamed.coreapi.tournament;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class TournamentEndsBeforeItStartsException extends CommonException {
    public TournamentEndsBeforeItStartsException() {
        super(
            "tournament-ends-before-it-starts",
            "Tournament ends before it starts",
            HttpStatus.BAD_REQUEST);
    }
}
