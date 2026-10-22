package com.alvarengacarlos.unnamed.app.tournament;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class TournamentFormatIsNotSupportedForThisOperationException extends CommonException {
    public TournamentFormatIsNotSupportedForThisOperationException(Tournament.TournamentFormat tournamentFormat) {
        super(
            "tournament-format-not-supported",
            "Tournament format is not supported for this operation",
            String.format("Tournament format %s is not supported for this operation", tournamentFormat.name()),
            HttpStatus.BAD_REQUEST);
    }
}
