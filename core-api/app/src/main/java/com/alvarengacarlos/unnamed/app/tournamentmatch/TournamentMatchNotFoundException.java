package com.alvarengacarlos.unnamed.app.tournamentmatch;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class TournamentMatchNotFoundException extends CommonException {
    public TournamentMatchNotFoundException() {
        super("tournament-match-not-found", "Tournament match not found", HttpStatus.NOT_FOUND);
    }
}
