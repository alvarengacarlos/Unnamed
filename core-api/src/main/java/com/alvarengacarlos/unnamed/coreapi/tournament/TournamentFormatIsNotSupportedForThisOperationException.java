package com.alvarengacarlos.unnamed.coreapi.tournament;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class TournamentFormatIsNotSupportedForThisOperationException extends CommonException {
    public TournamentFormatIsNotSupportedForThisOperationException(Tournament.TournamentFormat tournamentFormat) {
        super(
            "tournament-format-not-supported",
            "Tournament format is not supported for this operation",
            String.format("Tournament format %s is not supported for this operation", tournamentFormat.name()),
            HttpStatus.BAD_REQUEST);
    }
}
