package com.alvarengacarlos.unnamed.app.tournamentcompetitor;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class TournamentCompetitorNotFoundException extends CommonException {
    public TournamentCompetitorNotFoundException() {
        super("tournament-competitor-not-found", "Tournament competitor not found", HttpStatus.NOT_FOUND);
    }
}
