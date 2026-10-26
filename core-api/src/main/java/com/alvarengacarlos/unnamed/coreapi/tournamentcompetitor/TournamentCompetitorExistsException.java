package com.alvarengacarlos.unnamed.coreapi.tournamentcompetitor;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class TournamentCompetitorExistsException extends CommonException {
    public TournamentCompetitorExistsException() {
        super("tournament-competitor-exists", "Tournament competitor exists", HttpStatus.CONFLICT);
    }
}
