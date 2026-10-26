package com.alvarengacarlos.unnamed.coreapi.tournamentmatch;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class TournamentCompetitorsNeedToBeDifferentException extends CommonException {
    public TournamentCompetitorsNeedToBeDifferentException() {
        super(
            "tournament-competitors-need-to-be-different",
            "Tournament competitors need to be different",
            HttpStatus.BAD_REQUEST);
    }    
}
