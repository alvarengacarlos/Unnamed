package com.alvarengacarlos.unnamed.coreapi.tournamentmatch;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class TournamentMatchExistsException extends CommonException {
    public TournamentMatchExistsException() {
        super("tournament-match-exists", "Tournament match exists", HttpStatus.CONFLICT);
    }
}
