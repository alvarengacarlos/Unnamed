package com.alvarengacarlos.unnamed.app.tournamentmatch;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class TournamentMatchExistsException extends CommonException {
    public TournamentMatchExistsException() {
        super("tournament-match-exists", "Tournament match exists", HttpStatus.CONFLICT);
    }
}
