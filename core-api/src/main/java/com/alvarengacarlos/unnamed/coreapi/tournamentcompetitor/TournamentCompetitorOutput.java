package com.alvarengacarlos.unnamed.coreapi.tournamentcompetitor;

import java.time.Instant;
import java.util.UUID;

import com.alvarengacarlos.unnamed.coreapi.common.PaymentMethod;
import com.alvarengacarlos.unnamed.coreapi.member.MemberOutput;
import com.alvarengacarlos.unnamed.coreapi.tournament.TournamentOutput;

public record TournamentCompetitorOutput(
    UUID id,
    PaymentMethod paymentMethod,
    Boolean isPaid,
    Short points,
    Instant createdAt,
    Instant updatedAt,
    TournamentOutput tournament,
    MemberOutput member,
    UUID organizationId) {
    public static TournamentCompetitorOutput from(TournamentCompetitor tournamentCompetitor) {
        return new TournamentCompetitorOutput(
            tournamentCompetitor.getId(),
            tournamentCompetitor.getPaymentMethod(),
            tournamentCompetitor.getIsPaid(),
            tournamentCompetitor.getPoints(),
            tournamentCompetitor.getCreatedAt(),
            tournamentCompetitor.getUpdatedAt(),
            TournamentOutput.from(tournamentCompetitor.getTournament()),
            MemberOutput.from(tournamentCompetitor.getMember()),
            tournamentCompetitor.getOrganizationId());
    }
}
