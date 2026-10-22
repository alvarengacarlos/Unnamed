package com.alvarengacarlos.unnamed.app.tournamentcompetitor;

import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertSameEntity;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.alvarengacarlos.unnamed.app.common.Page;
import com.alvarengacarlos.unnamed.app.common.Pageable;
import com.alvarengacarlos.unnamed.app.common.PaymentMethod;
import com.alvarengacarlos.unnamed.app.testsupport.Fixtures;
import com.alvarengacarlos.unnamed.app.testsupport.IntegrationTest;
import com.alvarengacarlos.unnamed.app.testsupport.OrganizationScope;
import com.alvarengacarlos.unnamed.app.testsupport.OrganizationScoped;
import com.alvarengacarlos.unnamed.app.tournament.Tournament;

@IntegrationTest
public class TournamentCompetitorRepositoryIT implements OrganizationScoped {
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();
    private final TournamentCompetitorInput.GetTournamentCompetitors noFilter =
        new TournamentCompetitorInput.GetTournamentCompetitors(Optional.empty());
    private Tournament tournament;

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private TournamentCompetitorRepository tournamentCompetitorRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @BeforeEach
    void beforeEach() {
        tournament = fixtures.createTournament();
    }

    @Test
    void shouldSaveATournamentCompetitor() {
        final TournamentCompetitor tournamentCompetitor = fixtures.newTournamentCompetitor(
            tournament,
            fixtures.createMember());

        tournamentCompetitorRepository.save(tournamentCompetitor);

        assertSameEntity(
            tournamentCompetitorRepository.read(tournamentCompetitor.getId()).orElseThrow(),
            tournamentCompetitor);
    }

    @Test
    void shouldSaveTournamentCompetitorsInBulk() {
        final List<TournamentCompetitor> tournamentCompetitors = List.of(
            fixtures.newTournamentCompetitor(tournament, fixtures.createMember()),
            fixtures.newTournamentCompetitor(tournament, fixtures.createMember()),
            fixtures.newTournamentCompetitor(tournament, fixtures.createMember()));

        tournamentCompetitorRepository.saveInBulk(tournamentCompetitors);

        assertSameEntity(
            tournamentCompetitorRepository.read(new Pageable(0, 10), noFilter).content(),
            tournamentCompetitors);
    }

    @Test
    void shouldUpdateATournamentCompetitor() {
        final TournamentCompetitor tournamentCompetitor = fixtures.createTournamentCompetitor(tournament);
        final TournamentCompetitor updatedTournamentCompetitor = tournamentCompetitor.toBuilder()
            .paymentMethod(tournamentCompetitor.getPaymentMethod() == PaymentMethod.PIX
                ? PaymentMethod.MONEY
                : PaymentMethod.PIX)
            .isPaid(!tournamentCompetitor.getIsPaid())
            .points((short) (tournamentCompetitor.getPoints() + 1))
            .tournament(fixtures.createTournament())
            .member(fixtures.createMember())
            .build();

        tournamentCompetitorRepository.update(updatedTournamentCompetitor);

        assertSameEntity(
            tournamentCompetitorRepository.read(tournamentCompetitor.getId()).orElseThrow(),
            updatedTournamentCompetitor);
    }

    @Test
    void shouldNotReadATournamentCompetitorThatDoesNotExist() {
        assertThat(tournamentCompetitorRepository.read(UUID.randomUUID())).isEmpty();
    }

    @Test
    void shouldReadOnlyTheTournamentCompetitorsOfTheGivenIds() {
        final TournamentCompetitor tournamentCompetitorA = fixtures.createTournamentCompetitor(tournament);
        final TournamentCompetitor tournamentCompetitorB = fixtures.createTournamentCompetitor(tournament);
        fixtures.createTournamentCompetitor(tournament);

        final List<TournamentCompetitor> tournamentCompetitors = tournamentCompetitorRepository.read(
            Set.of(tournamentCompetitorA.getId(), tournamentCompetitorB.getId(), UUID.randomUUID()));

        assertSameEntity(tournamentCompetitors, List.of(tournamentCompetitorA, tournamentCompetitorB));
    }

    @Nested
    class Exists {
        @Test
        void shouldFindTheMemberInTheTournament() {
            final TournamentCompetitor tournamentCompetitor = fixtures.createTournamentCompetitor(tournament);

            assertThat(tournamentCompetitorRepository.exists(
                tournament.getId(),
                tournamentCompetitor.getMember().getId())).isTrue();
        }

        @Test
        void shouldNotFindTheMemberInAnotherTournament() {
            final TournamentCompetitor tournamentCompetitor = fixtures.createTournamentCompetitor(tournament);

            assertThat(tournamentCompetitorRepository.exists(
                fixtures.createTournament().getId(),
                tournamentCompetitor.getMember().getId())).isFalse();
        }
    }

    @Nested
    class ReadMany {
        @Test
        void shouldReadNone() {
            final Page<TournamentCompetitor> page = tournamentCompetitorRepository.read(
                new Pageable(0, 1), noFilter);

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isZero();
        }

        @Test
        void shouldReadAll() {
            final List<TournamentCompetitor> tournamentCompetitors = List.of(
                fixtures.createTournamentCompetitor(tournament),
                fixtures.createTournamentCompetitor(tournament),
                fixtures.createTournamentCompetitor(tournament));

            final Page<TournamentCompetitor> page = tournamentCompetitorRepository.read(
                new Pageable(0, 10), noFilter);

            assertSameEntity(page.content(), tournamentCompetitors);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadTheLastPage() {
            fixtures.createTournamentCompetitor(tournament);
            fixtures.createTournamentCompetitor(tournament);
            fixtures.createTournamentCompetitor(tournament);

            final Page<TournamentCompetitor> page = tournamentCompetitorRepository.read(
                new Pageable(1, 2), noFilter);

            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadOnlyTheCompetitorsOfTheTournament() {
            final Tournament tournament = fixtures.createTournament();
            final TournamentCompetitor tournamentCompetitor = fixtures.createTournamentCompetitor(tournament);
            fixtures.createTournamentCompetitor(fixtures.createTournament());

            final Page<TournamentCompetitor> page = tournamentCompetitorRepository.read(
                new Pageable(0, 10),
                new TournamentCompetitorInput.GetTournamentCompetitors(Optional.of(tournament.getId())));

            assertSameEntity(page.content(), List.of(tournamentCompetitor));
            assertThat(page.totalElements()).isEqualTo(1);
        }
    }

    @Test
    void shouldDeleteATournamentCompetitor() {
        final TournamentCompetitor tournamentCompetitor = fixtures.createTournamentCompetitor(tournament);

        tournamentCompetitorRepository.delete(tournamentCompetitor.getId());

        assertThat(tournamentCompetitorRepository.read(tournamentCompetitor.getId())).isEmpty();
    }

    @Nested
    class WhenTheTournamentCompetitorBelongsToAnotherOrganization {
        private TournamentCompetitor tournamentCompetitor;

        @BeforeEach
        void beforeEach() {
            tournamentCompetitor = OrganizationScope.call(
                anotherOrganizationId,
                () -> fixtures.createTournamentCompetitor(fixtures.createTournament()));
        }

        @Test
        void shouldNotReadIt() {
            assertThat(tournamentCompetitorRepository.read(tournamentCompetitor.getId())).isEmpty();
            assertThat(tournamentCompetitorRepository.read(Set.of(tournamentCompetitor.getId()))).isEmpty();
            assertThat(tournamentCompetitorRepository.exists(
                tournamentCompetitor.getTournament().getId(),
                tournamentCompetitor.getMember().getId())).isFalse();
            assertThat(tournamentCompetitorRepository.read(new Pageable(0, 1), noFilter).totalElements()).isZero();
        }

        @Test
        void shouldNotUpdateIt() {
            tournamentCompetitorRepository.update(tournamentCompetitor.toBuilder()
                .isPaid(!tournamentCompetitor.getIsPaid())
                .build());

            assertSameEntity(readInAnotherOrganization(), tournamentCompetitor);
        }

        @Test
        void shouldNotDeleteIt() {
            tournamentCompetitorRepository.delete(tournamentCompetitor.getId());

            assertSameEntity(readInAnotherOrganization(), tournamentCompetitor);
        }

        private TournamentCompetitor readInAnotherOrganization() {
            return OrganizationScope.call(
                anotherOrganizationId,
                () -> tournamentCompetitorRepository.read(tournamentCompetitor.getId()).orElseThrow());
        }
    }
}
