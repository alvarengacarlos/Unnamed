package com.alvarengacarlos.unnamed.coreapi.rank;

import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertSameEntity;
import static org.assertj.core.api.Assertions.assertThat;

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
import com.alvarengacarlos.unnamed.coreapi.testsupport.Fixtures;
import com.alvarengacarlos.unnamed.coreapi.testsupport.IntegrationTest;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScope;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScoped;

@IntegrationTest
public class RankRepositoryIT implements OrganizationScoped {
    private static final String UPDATED_NAME = "Updated name";
    private final RankInput.GetRanks noFilter = new RankInput.GetRanks(Optional.empty());
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private RankRepository rankRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @Test
    void shouldSaveARank() {
        final Rank rank = fixtures.newRank(fixtures.createRankCategory());

        rankRepository.save(rank);

        assertSameEntity(rankRepository.read(rank.getId()).orElseThrow(), rank);
    }

    @Test
    void shouldUpdateTheNameOfARank() {
        final Rank rank = fixtures.createRank();
        final Rank updatedRank = rank.toBuilder()
            .name(UPDATED_NAME)
            .build();

        rankRepository.update(updatedRank);

        assertSameEntity(rankRepository.read(rank.getId()).orElseThrow(), updatedRank);
    }

    @Test
    void shouldNotReadARankThatDoesNotExist() {
        assertThat(rankRepository.read(UUID.randomUUID())).isEmpty();
    }

    @Nested
    class ReadByIds {
        @Test
        void shouldReadNoneWhenThereAreNoIds() {
            fixtures.createRank();

            assertThat(rankRepository.read(Set.of())).isEmpty();
        }

        @Test
        void shouldReadOnlyTheGivenRanks() {
            final Rank rankA = fixtures.createRank();
            final Rank rankB = fixtures.createRank();
            fixtures.createRank();

            final List<Rank> ranks = rankRepository.read(Set.of(rankA.getId(), rankB.getId(), UUID.randomUUID()));

            assertSameEntity(ranks, List.of(rankA, rankB));
        }
    }

    @Nested
    class Exists {
        @Test
        void shouldFindARankWithTheSameCategoryAndOrder() {
            final Rank rank = fixtures.createRank();

            assertThat(rankRepository.exists(rank.getCategory().getId(), rank.getOrder())).isTrue();
        }

        @Test
        void shouldNotFindARankWithAnotherOrder() {
            final Rank rank = fixtures.createRank();

            assertThat(rankRepository.exists(rank.getCategory().getId(), (byte) (rank.getOrder() + 1))).isFalse();
        }

        @Test
        void shouldNotFindARankInAnotherCategory() {
            final Rank rank = fixtures.createRank();

            assertThat(rankRepository.exists(UUID.randomUUID(), rank.getOrder())).isFalse();
        }
    }

    @Nested
    class ReadMany {
        @Test
        void shouldReadNone() {
            final Page<Rank> page = rankRepository.read(new Pageable(0, 1), noFilter);

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isZero();
        }

        @Test
        void shouldReadAll() {
            final List<Rank> ranks = List.of(fixtures.createRank(), fixtures.createRank(), fixtures.createRank());

            final Page<Rank> page = rankRepository.read(new Pageable(0, 10), noFilter);

            assertSameEntity(page.content(), ranks);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadTheLastPage() {
            fixtures.createRank();
            fixtures.createRank();
            fixtures.createRank();

            final Page<Rank> page = rankRepository.read(new Pageable(1, 2), noFilter);

            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadOnlyTheRanksOfTheCategory() {
            final Rank rank = fixtures.createRank();
            fixtures.createRank();

            final Page<Rank> page = rankRepository.read(
                new Pageable(0, 10),
                new RankInput.GetRanks(Optional.of(rank.getCategory().getId())));

            assertSameEntity(page.content(), List.of(rank));
            assertThat(page.totalElements()).isEqualTo(1);
        }
    }

    @Test
    void shouldDeleteARank() {
        final Rank rank = fixtures.createRank();

        rankRepository.delete(rank.getId());

        assertThat(rankRepository.read(rank.getId())).isEmpty();
    }

    @Nested
    class WhenTheRankBelongsToAnotherOrganization {
        private Rank rank;

        @BeforeEach
        void beforeEach() {
            rank = OrganizationScope.call(anotherOrganizationId, fixtures::createRank);
        }

        @Test
        void shouldNotReadIt() {
            assertThat(rankRepository.read(rank.getId())).isEmpty();
            assertThat(rankRepository.read(Set.of(rank.getId()))).isEmpty();
            assertThat(rankRepository.read(new Pageable(0, 1), noFilter).totalElements()).isZero();
            assertThat(rankRepository.exists(rank.getCategory().getId(), rank.getOrder())).isFalse();
        }

        @Test
        void shouldNotUpdateIt() {
            rankRepository.update(rank.toBuilder().name(UPDATED_NAME).build());

            assertSameEntity(readInAnotherOrganization(), rank);
        }

        @Test
        void shouldNotDeleteIt() {
            rankRepository.delete(rank.getId());

            assertSameEntity(readInAnotherOrganization(), rank);
        }

        private Rank readInAnotherOrganization() {
            return OrganizationScope.call(anotherOrganizationId, () -> rankRepository.read(rank.getId()).orElseThrow());
        }
    }
}
