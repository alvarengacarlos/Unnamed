package com.alvarengacarlos.unnamed.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.alvarengacarlos.unnamed.app.common.PaymentMethod;
import com.alvarengacarlos.unnamed.app.member.Member;
import com.alvarengacarlos.unnamed.app.testsupport.Fixtures;
import com.alvarengacarlos.unnamed.app.testsupport.IntegrationTest;
import com.alvarengacarlos.unnamed.app.testsupport.OrganizationScoped;
import com.alvarengacarlos.unnamed.app.tournament.Tournament;
import com.alvarengacarlos.unnamed.app.tournamentcompetitor.TournamentCompetitor;
import com.alvarengacarlos.unnamed.app.tournamentcompetitor.TournamentCompetitorInput;
import com.alvarengacarlos.unnamed.app.tournamentcompetitor.TournamentCompetitorRepository;
import com.alvarengacarlos.unnamed.app.tournamentcompetitor.TournamentCompetitorService;
import com.alvarengacarlos.unnamed.app.tournamentmatch.TournamentMatchInput;
import com.alvarengacarlos.unnamed.app.tournamentmatch.TournamentMatchRepository;
import com.alvarengacarlos.unnamed.app.tournamentmatch.TournamentMatchService;

/**
 * A bulk is saved in one transaction: when anything fails after the insert, none of its items stays in the database.
 */
@IntegrationTest
public class BulkTransactionIT implements OrganizationScoped {
    private static final String FAILURE = "Failure after the insert";
    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 9, 12, 10, 30);
    private final UUID organizationId = UUID.randomUUID();

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private TournamentCompetitorService tournamentCompetitorService;

    @Autowired
    private TournamentMatchService tournamentMatchService;

    @MockitoSpyBean
    private TournamentCompetitorRepository tournamentCompetitorRepository;

    @MockitoSpyBean
    private TournamentMatchRepository tournamentMatchRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @Test
    void shouldSaveNoTournamentCompetitorWhenTheBulkFails() {
        final Tournament tournament = fixtures.createTournament();
        final Member member = fixtures.createMember();
        final Member anotherMember = fixtures.createMember();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException(FAILURE);
        }).when(tournamentCompetitorRepository).saveInBulk(anyList());

        assertThatThrownBy(() -> tournamentCompetitorService.createTournamentCompetitorsInBulk(
            new TournamentCompetitorInput.CreateInBulk(Set.of(
                new TournamentCompetitorInput.Create(PaymentMethod.PIX, false, tournament.getId(), member.getId()),
                new TournamentCompetitorInput.Create(
                    PaymentMethod.PIX,
                    false,
                    tournament.getId(),
                    anotherMember.getId())))))
            .hasMessage(FAILURE);

        assertThat(tournamentCompetitorRepository.exists(tournament.getId(), member.getId())).isFalse();
        assertThat(tournamentCompetitorRepository.exists(tournament.getId(), anotherMember.getId())).isFalse();
    }

    @Test
    void shouldSaveNoTournamentMatchWhenTheBulkFails() {
        final Tournament tournament = fixtures.createTournament();
        final TournamentCompetitor competitorA = fixtures.createTournamentCompetitor(tournament);
        final TournamentCompetitor competitorB = fixtures.createTournamentCompetitor(tournament);
        final TournamentCompetitor competitorC = fixtures.createTournamentCompetitor(tournament);
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException(FAILURE);
        }).when(tournamentMatchRepository).saveInBulk(anyList());

        assertThatThrownBy(() -> tournamentMatchService.createTournamentMatchesInBulk(
            new TournamentMatchInput.CreateInBulk(Set.of(
                matchInput(tournament, competitorA, competitorB),
                matchInput(tournament, competitorA, competitorC)))))
            .hasMessage(FAILURE);

        assertThat(tournamentMatchRepository.exists(tournament.getId(), competitorA.getId(), competitorB.getId()))
            .isFalse();
        assertThat(tournamentMatchRepository.exists(tournament.getId(), competitorA.getId(), competitorC.getId()))
            .isFalse();
    }

    private static TournamentMatchInput.Create matchInput(
        Tournament tournament,
        TournamentCompetitor competitorA,
        TournamentCompetitor competitorB) {
        return new TournamentMatchInput.Create(
            WHEN,
            "Mat 1",
            competitorA.getId(),
            competitorB.getId(),
            tournament.getId());
    }
}
