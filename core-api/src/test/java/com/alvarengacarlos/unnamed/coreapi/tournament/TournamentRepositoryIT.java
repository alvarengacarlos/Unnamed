package com.alvarengacarlos.unnamed.coreapi.tournament;

import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertSameEntity;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.alvarengacarlos.unnamed.coreapi.common.Page;
import com.alvarengacarlos.unnamed.coreapi.common.Pageable;
import com.alvarengacarlos.unnamed.coreapi.member.Member;
import com.alvarengacarlos.unnamed.coreapi.testsupport.Fixtures;
import com.alvarengacarlos.unnamed.coreapi.testsupport.IntegrationTest;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScope;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScoped;

@IntegrationTest
public class TournamentRepositoryIT implements OrganizationScoped {
    private static final String UPDATED_NAME = "Updated name";
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @Nested
    class Save {
        @Test
        void shouldSaveATournamentWithoutARankInTheDivisionCriterion() {
            final Tournament tournament = fixtures.newTournament();

            tournamentRepository.save(tournament);

            assertSameEntity(tournamentRepository.read(tournament.getId()).orElseThrow(), tournament);
        }

        @Test
        void shouldSaveATournamentWithARankInTheDivisionCriterion() {
            final Tournament tournament = fixtures.newTournament()
                .toBuilder()
                .tournamentDivisionCriterion(new TournamentDivisionCriterion.Builder()
                    .rank(Optional.of(fixtures.createRank()))
                    .build())
                .build();

            tournamentRepository.save(tournament);

            assertSameEntity(tournamentRepository.read(tournament.getId()).orElseThrow(), tournament);
        }
    }

    @Test
    void shouldUpdateATournament() {
        final Tournament tournament = fixtures.createTournament();
        final Tournament updatedTournament = tournament.toBuilder()
            .name(UPDATED_NAME)
            .format(Tournament.TournamentFormat.SINGLE_ELIMINATION)
            .tournamentDivisionCriterion(new TournamentDivisionCriterion.Builder()
                .gender(Optional.of(Member.Gender.FEMALE))
                .weight(Optional.of(new BigDecimal("60.00")))
                .rank(Optional.of(fixtures.createRank()))
                .build())
            .participationPrice(new BigDecimal("75.00"))
            .startDateTime(LocalDateTime.of(2030, 5, 1, 9, 0))
            .endDateTime(LocalDateTime.of(2030, 5, 1, 18, 0))
            .where("Updated place")
            .build();

        tournamentRepository.update(updatedTournament);

        assertSameEntity(tournamentRepository.read(tournament.getId()).orElseThrow(), updatedTournament);
    }

    @Test
    void shouldNotReadATournamentThatDoesNotExist() {
        assertThat(tournamentRepository.read(UUID.randomUUID())).isEmpty();
    }

    @Test
    void shouldReadOnlyTheTournamentsOfTheGivenIds() {
        final Tournament tournamentA = fixtures.createTournament();
        final Tournament tournamentB = fixtures.createTournament();
        fixtures.createTournament();

        final List<Tournament> tournaments = tournamentRepository.read(
            Set.of(tournamentA.getId(), tournamentB.getId(), UUID.randomUUID()));

        assertSameEntity(tournaments, List.of(tournamentA, tournamentB));
    }

    @Nested
    class ReadMany {
        @Test
        void shouldReadNone() {
            final Page<Tournament> page = tournamentRepository.read(new Pageable(0, 1));

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isZero();
        }

        @Test
        void shouldReadAll() {
            final List<Tournament> tournaments = List.of(
                fixtures.createTournament(),
                fixtures.createTournament(),
                fixtures.createTournament());

            final Page<Tournament> page = tournamentRepository.read(new Pageable(0, 10));

            assertSameEntity(page.content(), tournaments);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadTheLastPage() {
            fixtures.createTournament();
            fixtures.createTournament();
            fixtures.createTournament();

            final Page<Tournament> page = tournamentRepository.read(new Pageable(1, 2));

            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(3);
        }
    }

    @Test
    void shouldDeleteATournament() {
        final Tournament tournament = fixtures.createTournament();

        tournamentRepository.delete(tournament.getId());

        assertThat(tournamentRepository.read(tournament.getId())).isEmpty();
    }

    @Nested
    class WhenTheTournamentBelongsToAnotherOrganization {
        private Tournament tournament;

        @BeforeEach
        void beforeEach() {
            tournament = OrganizationScope.call(anotherOrganizationId, () -> fixtures.createTournament());
        }

        @Test
        void shouldNotReadIt() {
            assertThat(tournamentRepository.read(tournament.getId())).isEmpty();
            assertThat(tournamentRepository.read(Set.of(tournament.getId()))).isEmpty();
            assertThat(tournamentRepository.read(new Pageable(0, 1)).totalElements()).isZero();
        }

        @Test
        void shouldNotUpdateIt() {
            tournamentRepository.update(tournament.toBuilder().name(UPDATED_NAME).build());

            assertSameEntity(readInAnotherOrganization(), tournament);
        }

        @Test
        void shouldNotDeleteIt() {
            tournamentRepository.delete(tournament.getId());

            assertSameEntity(readInAnotherOrganization(), tournament);
        }

        private Tournament readInAnotherOrganization() {
            return OrganizationScope.call(
                anotherOrganizationId,
                () -> tournamentRepository.read(tournament.getId()).orElseThrow());
        }
    }
}
