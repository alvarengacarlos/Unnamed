package com.alvarengacarlos.unnamed.app.tournament;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record TournamentOutput(
    UUID id,
    String name,
    Tournament.TournamentFormat format,
    Map<TournamentDivisionCriterion.DivisionCriterion, Object> divisionCriterion,
    BigDecimal participationPrice,
    LocalDateTime startDateTime,
    LocalDateTime endDateTime,
    String where,
    Instant createdAt,
    Instant updatedAt,
    UUID organizationId) {
    public static TournamentOutput from(Tournament tournament) {
        return new TournamentOutput(
            tournament.getId(),
            tournament.getName(),
            tournament.getFormat(),
            toDivisionCriterionOutput(tournament.getTournamentDivisionCriterion()),
            tournament.getParticipationPrice(),
            tournament.getStartDateTime(),
            tournament.getEndDateTime(),
            tournament.getWhere(),
            tournament.getCreatedAt(),
            tournament.getUpdatedAt(),
            tournament.getOrganizationId());
    }

    private static Map<TournamentDivisionCriterion.DivisionCriterion, Object> toDivisionCriterionOutput(
        TournamentDivisionCriterion tournamentDivisionCriterion) {
        final Map<TournamentDivisionCriterion.DivisionCriterion, Object> divisionCriterion = new HashMap<>();
        if (tournamentDivisionCriterion.getGender().isPresent()) {
            divisionCriterion.put(
                TournamentDivisionCriterion.DivisionCriterion.GENDER, 
                tournamentDivisionCriterion.getGender().get());
        }

        if (tournamentDivisionCriterion.getWeight().isPresent()) {
            divisionCriterion.put(
                TournamentDivisionCriterion.DivisionCriterion.WEIGHT, 
                tournamentDivisionCriterion.getWeight().get());
        }

        if (tournamentDivisionCriterion.getHeight().isPresent()) {
            divisionCriterion.put(
                TournamentDivisionCriterion.DivisionCriterion.HEIGHT, 
                tournamentDivisionCriterion.getHeight().get());
        }

        if (tournamentDivisionCriterion.getAge().isPresent()) {
            divisionCriterion.put(
                TournamentDivisionCriterion.DivisionCriterion.AGE, 
                tournamentDivisionCriterion.getAge().get());
        }

        if (tournamentDivisionCriterion.getIsPwd().isPresent()) {
            divisionCriterion.put(
                TournamentDivisionCriterion.DivisionCriterion.IS_PWD, 
                tournamentDivisionCriterion.getIsPwd().get());
        }

        if (tournamentDivisionCriterion.getRank().isPresent()) {
            divisionCriterion.put(
                TournamentDivisionCriterion.DivisionCriterion.RANK,
                tournamentDivisionCriterion.getRank().get());
        }
        
        return divisionCriterion;
    }
}
