package com.alvarengacarlos.unnamed.coreapi.tournamentcompetitor;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class TournamentCompetitorNotFoundException extends CommonException {
    public TournamentCompetitorNotFoundException() {
        super("tournament-competitor-not-found", "Tournament competitor not found", HttpStatus.NOT_FOUND);
    }
}
