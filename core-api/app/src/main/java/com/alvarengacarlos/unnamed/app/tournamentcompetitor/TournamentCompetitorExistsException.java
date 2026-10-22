package com.alvarengacarlos.unnamed.app.tournamentcompetitor;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class TournamentCompetitorExistsException extends CommonException {
    public TournamentCompetitorExistsException() {
        super("tournament-competitor-exists", "Tournament competitor exists", HttpStatus.CONFLICT);
    }
}
