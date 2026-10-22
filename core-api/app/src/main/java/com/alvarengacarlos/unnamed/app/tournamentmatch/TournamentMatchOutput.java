package com.alvarengacarlos.unnamed.app.tournamentmatch;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import com.alvarengacarlos.unnamed.app.tournament.TournamentOutput;
import com.alvarengacarlos.unnamed.app.tournamentcompetitor.TournamentCompetitorOutput;

public record TournamentMatchOutput(
    UUID id,
    LocalDateTime when,
    String where,
    TournamentMatch.TournamentMatchResult result,
    Instant createdAt,
    Instant updatedAt,
    TournamentCompetitorOutput competitorA,
    TournamentCompetitorOutput competitorB,
    TournamentOutput tournament,
    UUID organizationId) {
    public static TournamentMatchOutput from(TournamentMatch tournamentMatch) {
        return new TournamentMatchOutput(
            tournamentMatch.getId(),
            tournamentMatch.getWhen(),
            tournamentMatch.getWhere(),
            tournamentMatch.getResult(),
            tournamentMatch.getCreatedAt(),
            tournamentMatch.getUpdatedAt(),
            TournamentCompetitorOutput.from(tournamentMatch.getCompetitorA()),
            TournamentCompetitorOutput.from(tournamentMatch.getCompetitorB()),
            TournamentOutput.from(tournamentMatch.getTournament()),
            tournamentMatch.getOrganizationId());
    }
}
