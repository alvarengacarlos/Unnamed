package com.alvarengacarlos.unnamed.coreapi.tournamentmatch;

import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertSameEntity;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.alvarengacarlos.unnamed.coreapi.common.Page;
import com.alvarengacarlos.unnamed.coreapi.common.Pageable;
import com.alvarengacarlos.unnamed.coreapi.testsupport.Fixtures;
import com.alvarengacarlos.unnamed.coreapi.testsupport.IntegrationTest;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScope;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScoped;
import com.alvarengacarlos.unnamed.coreapi.tournament.Tournament;
import com.alvarengacarlos.unnamed.coreapi.tournamentcompetitor.TournamentCompetitor;

@IntegrationTest
public class TournamentMatchRepositoryIT implements OrganizationScoped {
    private static final String UPDATED_PLACE = "Updated place";
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();
    private final TournamentMatchInput.GetTournamentMatches noFilter =
        new TournamentMatchInput.GetTournamentMatches(Optional.empty());
    private Tournament tournament;
    private TournamentCompetitor tournamentCompetitorA;
    private TournamentCompetitor tournamentCompetitorB;

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private TournamentMatchRepository tournamentMatchRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @BeforeEach
    void beforeEach() {
        tournament = fixtures.createTournament();
        tournamentCompetitorA = fixtures.createTournamentCompetitor(tournament);
        tournamentCompetitorB = fixtures.createTournamentCompetitor(tournament);
    }

    @Test
    void shouldSaveATournamentMatch() {
        final TournamentMatch tournamentMatch = fixtures.newTournamentMatch(
            tournament,
            tournamentCompetitorA,
            tournamentCompetitorB);

        tournamentMatchRepository.save(tournamentMatch);

        assertSameEntity(tournamentMatchRepository.read(tournamentMatch.getId()).orElseThrow(), tournamentMatch);
    }

    @Test
    void shouldSaveTournamentMatchesInBulk() {
        final List<TournamentMatch> tournamentMatches = List.of(newTournamentMatch(), newTournamentMatch());

        tournamentMatchRepository.saveInBulk(tournamentMatches);

        assertSameEntity(tournamentMatchRepository.read(new Pageable(0, 10), noFilter).content(), tournamentMatches);
    }

    @Test
    void shouldUpdateATournamentMatch() {
        final TournamentMatch tournamentMatch = createTournamentMatch();
        final TournamentMatch updatedTournamentMatch = tournamentMatch.toBuilder()
            .when(LocalDateTime.of(2030, 3, 1, 15, 0))
            .where(UPDATED_PLACE)
            .result(tournamentMatch.getResult() == TournamentMatch.TournamentMatchResult.A
                ? TournamentMatch.TournamentMatchResult.B
                : TournamentMatch.TournamentMatchResult.A)
            .build();

        tournamentMatchRepository.update(updatedTournamentMatch);

        assertSameEntity(tournamentMatchRepository.read(tournamentMatch.getId()).orElseThrow(), updatedTournamentMatch);
    }

    @Test
    void shouldNotReadATournamentMatchThatDoesNotExist() {
        assertThat(tournamentMatchRepository.read(UUID.randomUUID())).isEmpty();
    }

    @Nested
    class ExistsByCompetitors {
        @BeforeEach
        void beforeEach() {
            createTournamentMatch();
        }

        @Test
        void shouldFindTheMatchWithTheCompetitorsInTheSameOrder() {
            assertThat(tournamentMatchRepository.exists(
                tournament.getId(),
                tournamentCompetitorA.getId(),
                tournamentCompetitorB.getId())).isTrue();
        }

        @Test
        void shouldFindTheMatchWithTheCompetitorsInTheOppositeOrder() {
            assertThat(tournamentMatchRepository.exists(
                tournament.getId(),
                tournamentCompetitorB.getId(),
                tournamentCompetitorA.getId())).isTrue();
        }

        @Test
        void shouldNotFindAMatchWithAnotherCompetitor() {
            assertThat(tournamentMatchRepository.exists(
                tournament.getId(),
                tournamentCompetitorA.getId(),
                fixtures.createTournamentCompetitor(tournament).getId())).isFalse();
        }

        @Test
        void shouldNotFindTheMatchInAnotherTournament() {
            assertThat(tournamentMatchRepository.exists(
                fixtures.createTournament().getId(),
                tournamentCompetitorA.getId(),
                tournamentCompetitorB.getId())).isFalse();
        }
    }

    @Nested
    class ExistsAny {
        @Test
        void shouldNotFindAnyWhenNoneOfThemExists() {
            createTournamentMatch();

            assertThat(tournamentMatchRepository.exists(List.of(newTournamentMatch(), newTournamentMatch()))).isFalse();
        }

        @Test
        void shouldFindOneWhenOneOfThemExists() {
            final TournamentMatch tournamentMatch = createTournamentMatch();

            assertThat(tournamentMatchRepository.exists(List.of(newTournamentMatch(), tournamentMatch))).isTrue();
        }

        @Test
        void shouldFindOneWhenTheCompetitorsAreInTheOppositeOrder() {
            createTournamentMatch();
            final TournamentMatch oppositeTournamentMatch = fixtures.newTournamentMatch(
                tournament,
                tournamentCompetitorB,
                tournamentCompetitorA);

            assertThat(tournamentMatchRepository.exists(List.of(oppositeTournamentMatch))).isTrue();
        }
    }

    @Nested
    class ReadMany {
        @Test
        void shouldReadNone() {
            final Page<TournamentMatch> page = tournamentMatchRepository.read(new Pageable(0, 1), noFilter);

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isZero();
        }

        @Test
        void shouldReadAll() {
            final List<TournamentMatch> tournamentMatches = List.of(
                createTournamentMatch(),
                fixtures.createTournamentMatch(),
                fixtures.createTournamentMatch());

            final Page<TournamentMatch> page = tournamentMatchRepository.read(new Pageable(0, 10), noFilter);

            assertSameEntity(page.content(), tournamentMatches);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadTheLastPage() {
            createTournamentMatch();
            fixtures.createTournamentMatch();
            fixtures.createTournamentMatch();

            final Page<TournamentMatch> page = tournamentMatchRepository.read(new Pageable(1, 2), noFilter);

            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadOnlyTheMatchesOfTheTournament() {
            final TournamentMatch tournamentMatch = fixtures.createTournamentMatch();
            fixtures.createTournamentMatch();

            final Page<TournamentMatch> page = tournamentMatchRepository.read(
                new Pageable(0, 10),
                new TournamentMatchInput.GetTournamentMatches(Optional.of(tournamentMatch.getTournament().getId())));

            assertSameEntity(page.content(), List.of(tournamentMatch));
            assertThat(page.totalElements()).isEqualTo(1);
        }
    }

    @Test
    void shouldDeleteATournamentMatch() {
        final TournamentMatch tournamentMatch = createTournamentMatch();

        tournamentMatchRepository.delete(tournamentMatch.getId());

        assertThat(tournamentMatchRepository.read(tournamentMatch.getId())).isEmpty();
    }

    @Nested
    class WhenTheTournamentMatchBelongsToAnotherOrganization {
        private TournamentMatch tournamentMatch;

        @BeforeEach
        void beforeEach() {
            tournamentMatch = OrganizationScope.call(anotherOrganizationId, () -> fixtures.createTournamentMatch());
        }

        @Test
        void shouldNotReadIt() {
            assertThat(tournamentMatchRepository.read(tournamentMatch.getId())).isEmpty();
            assertThat(tournamentMatchRepository.exists(
                tournamentMatch.getTournament().getId(),
                tournamentMatch.getCompetitorA().getId(),
                tournamentMatch.getCompetitorB().getId())).isFalse();
            assertThat(tournamentMatchRepository.exists(List.of(tournamentMatch))).isFalse();
            assertThat(tournamentMatchRepository.read(new Pageable(0, 1), noFilter).totalElements()).isZero();
        }

        @Test
        void shouldNotUpdateIt() {
            tournamentMatchRepository.update(tournamentMatch.toBuilder().where(UPDATED_PLACE).build());

            assertSameEntity(readInAnotherOrganization(), tournamentMatch);
        }

        @Test
        void shouldNotDeleteIt() {
            tournamentMatchRepository.delete(tournamentMatch.getId());

            assertSameEntity(readInAnotherOrganization(), tournamentMatch);
        }

        private TournamentMatch readInAnotherOrganization() {
            return OrganizationScope.call(
                anotherOrganizationId,
                () -> tournamentMatchRepository.read(tournamentMatch.getId()).orElseThrow());
        }
    }

    private TournamentMatch createTournamentMatch() {
        return fixtures.createTournamentMatch(tournament, tournamentCompetitorA, tournamentCompetitorB);
    }

    // A match between two new competitors of the tournament.
    private TournamentMatch newTournamentMatch() {
        return fixtures.newTournamentMatch(
            tournament,
            fixtures.createTournamentCompetitor(tournament),
            fixtures.createTournamentCompetitor(tournament));
    }
}
