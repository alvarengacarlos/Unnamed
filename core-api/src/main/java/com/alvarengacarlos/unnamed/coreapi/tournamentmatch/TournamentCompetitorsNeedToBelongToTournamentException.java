package com.alvarengacarlos.unnamed.coreapi.tournamentmatch;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class TournamentCompetitorsNeedToBelongToTournamentException extends CommonException {
    public TournamentCompetitorsNeedToBelongToTournamentException() {
        super(
            "tournament-competitors-need-to-belong-to-the-tournament",
            "Tournament competitors need to belong to the tournament",
            HttpStatus.BAD_REQUEST);
    }
}
