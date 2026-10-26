package com.alvarengacarlos.unnamed.coreapi.tournament;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class TournamentNotFoundException extends CommonException {
    public TournamentNotFoundException() {
        super("tournament-not-found", "Tournament not found", HttpStatus.NOT_FOUND);
    }
}
