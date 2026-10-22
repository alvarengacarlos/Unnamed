package com.alvarengacarlos.unnamed.app.tournamentmatch;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class TournamentCompetitorsNeedToBelongToTournamentException extends CommonException {
    public TournamentCompetitorsNeedToBelongToTournamentException() {
        super(
            "tournament-competitors-need-to-belong-to-the-tournament",
            "Tournament competitors need to belong to the tournament",
            HttpStatus.BAD_REQUEST);
    }
}
