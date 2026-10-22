package com.alvarengacarlos.unnamed.app.tournamentmatch;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvarengacarlos.unnamed.app.OrganizationContext;
import com.alvarengacarlos.unnamed.app.common.AuditLog;
import com.alvarengacarlos.unnamed.app.common.BulkHasDuplicatedItemException;
import com.alvarengacarlos.unnamed.app.common.CommonInput;
import com.alvarengacarlos.unnamed.app.common.Page;
import com.alvarengacarlos.unnamed.app.common.Pageable;
import com.alvarengacarlos.unnamed.app.tournament.Tournament;
import com.alvarengacarlos.unnamed.app.tournament.TournamentService;
import com.alvarengacarlos.unnamed.app.tournamentcompetitor.TournamentCompetitor;
import com.alvarengacarlos.unnamed.app.tournamentcompetitor.TournamentCompetitorService;

@Service
public class TournamentMatchService {
    private final String creatingLog = "creating tournament match {}";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TournamentMatchRepository tournamentMatchRepository;
    private final TournamentService tournamentService;
    private final TournamentCompetitorService tournamentCompetitorService;

    public TournamentMatchService(
        TournamentMatchRepository tournamentMatchRepository,
        TournamentService tournamentService,
        TournamentCompetitorService tournamentCompetitorService) {
        this.tournamentMatchRepository = tournamentMatchRepository;
        this.tournamentService = tournamentService;
        this.tournamentCompetitorService = tournamentCompetitorService;
    }

    @Transactional
    public TournamentMatch createTournamentMatch(TournamentMatchInput.Create input) {
        checkCompetitorIds(input.tournamentCompetitorAId(), input.tournamentCompetitorBId());
        final TournamentMatch tournamentMatch = mountTournamentMatch(
            input,
            tournamentService.getTournament(input.tournamentId()),
            tournamentCompetitorService.getTournamentCompetitor(input.tournamentCompetitorAId()),
            tournamentCompetitorService.getTournamentCompetitor(input.tournamentCompetitorBId()));
        tournamentMatchRepository.lockTournaments(List.of(input.tournamentId()));
        checkIfTournamentMatchExists(
            input.tournamentId(),
            input.tournamentCompetitorAId(),
            input.tournamentCompetitorBId());

        AuditLog.log(creatingLog, tournamentMatch.getId());
        tournamentMatchRepository.save(tournamentMatch);
        return tournamentMatch;
    }

    private void checkCompetitorIds(UUID tournamentCompetitorAId, UUID tournamentCompetitorBId) {
        if (tournamentCompetitorAId.equals(tournamentCompetitorBId)) {
            throw new TournamentCompetitorsNeedToBeDifferentException();
        }
    }

    private TournamentMatch mountTournamentMatch(
        TournamentMatchInput.Create input,
        Tournament tournament,
        TournamentCompetitor tournamentCompetitorA,
        TournamentCompetitor tournamentCompetitorB) {
        checkIfCompetitorsBelongToTournament(
            tournament.getId(),
            tournamentCompetitorA,
            tournamentCompetitorB);

        return TournamentMatch.builder()
            .when(input.when())
            .where(input.where())
            .result(TournamentMatch.TournamentMatchResult.PENDING)
            .tournamentCompetitorA(tournamentCompetitorA)
            .tournamentCompetitorB(tournamentCompetitorB)
            .tournament(tournament)
            .organizationId(OrganizationContext.require())
            .build();
    }

    private void checkIfCompetitorsBelongToTournament(
        UUID tournamentId,
        TournamentCompetitor tournamentCompetitorA,
        TournamentCompetitor tournamentCompetitorB) {
        if (!tournamentCompetitorA.getTournament().getId().equals(tournamentId)
            || !tournamentCompetitorB.getTournament().getId().equals(tournamentId)) {
            throw new TournamentCompetitorsNeedToBelongToTournamentException();
        }
    }

    private void checkIfTournamentMatchExists(
        UUID tournamentId, 
        UUID tournamentCompetitorAId, 
        UUID tournamentCompetitorBId) {
        if (tournamentMatchRepository.exists(tournamentId, tournamentCompetitorAId, tournamentCompetitorBId)) {
            throw new TournamentMatchExistsException();
        }
    }

    @Transactional
    public TournamentMatch updateTournamentMatch(UUID id, TournamentMatchInput.Update input) {
        AuditLog.log("updating tournament match {}", id);

        final TournamentMatch tournamentMatch = getTournamentMatch(id);

        final TournamentMatch updatedTournamentMatch = tournamentMatch.toBuilder()
            .when(input.when())
            .where(input.where())
            .result(input.result())
            .build();

        tournamentMatchRepository.update(updatedTournamentMatch);
        return updatedTournamentMatch;
    }

    public TournamentMatch getTournamentMatch(UUID id) {
        logger.debug("Getting tournament match {}", id);

        return tournamentMatchRepository.read(id)
            .orElseThrow(() -> new TournamentMatchNotFoundException());
    }

    @Transactional
    public TournamentMatch deleteTournamentMatch(UUID id) {
        AuditLog.log("deleting tournament match {}", id);

        final TournamentMatch tournamentMatch = getTournamentMatch(id);
        tournamentMatchRepository.delete(id);
        return tournamentMatch;
    }

    public Page<TournamentMatch> getTournamentMatches(
        CommonInput.PaginationInput paginationInput,
        TournamentMatchInput.GetTournamentMatches filterInput) {
        return tournamentMatchRepository.read(Pageable.from(paginationInput), filterInput);
    }

    @Transactional
    public List<TournamentMatch> createTournamentMatchesInBulk(
        TournamentMatchInput.CreateInBulk input) {

        input.bulk()
            .stream()
            .forEach(c -> checkCompetitorIds(c.tournamentCompetitorAId(), c.tournamentCompetitorBId()));
        checkIfHasDuplicatedItem(input);

        final Map<UUID, Tournament> tournaments = tournamentService.getTournaments(input.bulk()
            .stream()
            .map(TournamentMatchInput.Create::tournamentId)
            .collect(Collectors.toSet()));
        final Map<UUID, TournamentCompetitor> tournamentCompetitors = tournamentCompetitorService
            .getTournamentCompetitors(input.bulk()
                .stream()
                .flatMap(tm -> Stream.of(tm.tournamentCompetitorAId(), tm.tournamentCompetitorBId()))
                .collect(Collectors.toSet()));

        final List<TournamentMatch> tournamentMatches = input.bulk()
            .stream()
            .map(tm -> mountTournamentMatch(
                tm,
                tournaments.get(tm.tournamentId()),
                tournamentCompetitors.get(tm.tournamentCompetitorAId()),
                tournamentCompetitors.get(tm.tournamentCompetitorBId())))
            .toList();
        tournamentMatchRepository.lockTournaments(tournaments.keySet());
        if (tournamentMatchRepository.exists(tournamentMatches)) {
            throw new TournamentMatchExistsException();
        }

        tournamentMatches.forEach(created -> AuditLog.log(creatingLog, created.getId()));
        tournamentMatchRepository.saveInBulk(tournamentMatches);

        return tournamentMatches;
    }

    private void checkIfHasDuplicatedItem(TournamentMatchInput.CreateInBulk input) {
        final Boolean hasDuplicatedItem = input.bulk()
            .stream()
            .map(MatchKey::from)
            .distinct()
            .count() != input.bulk().size();
        if (hasDuplicatedItem) {
            throw new BulkHasDuplicatedItemException();
        }
    }

    // A x B and B x A are the same match, so the competitors are kept in a fixed order.
    private static record MatchKey(UUID tournamentId, UUID lowerCompetitorId, UUID higherCompetitorId) {
        static MatchKey from(TournamentMatchInput.Create input) {
            final UUID competitorAId = input.tournamentCompetitorAId();
            final UUID competitorBId = input.tournamentCompetitorBId();
            if (competitorAId.compareTo(competitorBId) <= 0) {
                return new MatchKey(input.tournamentId(), competitorAId, competitorBId);
            }
            return new MatchKey(input.tournamentId(), competitorBId, competitorAId);
        }
    }
}
