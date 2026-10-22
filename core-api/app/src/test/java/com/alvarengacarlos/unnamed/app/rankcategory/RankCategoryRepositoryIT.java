package com.alvarengacarlos.unnamed.app.rankcategory;

import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertSameEntity;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.alvarengacarlos.unnamed.app.common.Page;
import com.alvarengacarlos.unnamed.app.common.Pageable;
import com.alvarengacarlos.unnamed.app.testsupport.Fixtures;
import com.alvarengacarlos.unnamed.app.testsupport.IntegrationTest;
import com.alvarengacarlos.unnamed.app.testsupport.OrganizationScope;
import com.alvarengacarlos.unnamed.app.testsupport.OrganizationScoped;

@IntegrationTest
public class RankCategoryRepositoryIT implements OrganizationScoped {
    private static final String UPDATED_NAME = "Updated name";
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private RankCategoryRepository rankCategoryRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @Test
    void shouldSaveARankCategory() {
        final RankCategory rankCategory = fixtures.newRankCategory();

        rankCategoryRepository.save(rankCategory);

        assertSameEntity(rankCategoryRepository.read(rankCategory.getId()).orElseThrow(), rankCategory);
    }

    @Test
    void shouldUpdateARankCategory() {
        final RankCategory rankCategory = fixtures.createRankCategory();
        final RankCategory updatedRankCategory = rankCategory.toBuilder()
            .name(UPDATED_NAME)
            .build();

        rankCategoryRepository.update(updatedRankCategory);

        assertSameEntity(rankCategoryRepository.read(rankCategory.getId()).orElseThrow(), updatedRankCategory);
    }

    @Test
    void shouldNotReadARankCategoryThatDoesNotExist() {
        assertThat(rankCategoryRepository.read(UUID.randomUUID())).isEmpty();
    }

    @Nested
    class ReadMany {
        @Test
        void shouldReadNone() {
            final Page<RankCategory> page = rankCategoryRepository.read(new Pageable(0, 1));

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isZero();
        }

        @Test
        void shouldReadAll() {
            final List<RankCategory> rankCategories = List.of(
                fixtures.createRankCategory(),
                fixtures.createRankCategory(),
                fixtures.createRankCategory());

            final Page<RankCategory> page = rankCategoryRepository.read(new Pageable(0, 10));

            assertSameEntity(page.content(), rankCategories);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadTheLastPage() {
            fixtures.createRankCategory();
            fixtures.createRankCategory();
            fixtures.createRankCategory();

            final Page<RankCategory> page = rankCategoryRepository.read(new Pageable(1, 2));

            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadThePageAfterTheLastOneWithTheTotal() {
            fixtures.createRankCategory();
            fixtures.createRankCategory();
            fixtures.createRankCategory();

            final Page<RankCategory> page = rankCategoryRepository.read(new Pageable(5, 2));

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isEqualTo(3);
        }
    }

    @Test
    void shouldDeleteARankCategory() {
        final RankCategory rankCategory = fixtures.createRankCategory();

        rankCategoryRepository.delete(rankCategory.getId());

        assertThat(rankCategoryRepository.read(rankCategory.getId())).isEmpty();
    }

    @Nested
    class WhenTheRankCategoryBelongsToAnotherOrganization {
        private RankCategory rankCategory;

        @BeforeEach
        void beforeEach() {
            rankCategory = OrganizationScope.call(anotherOrganizationId, fixtures::createRankCategory);
        }

        @Test
        void shouldNotReadIt() {
            assertThat(rankCategoryRepository.read(rankCategory.getId())).isEmpty();
            assertThat(rankCategoryRepository.read(new Pageable(0, 1)).totalElements()).isZero();
        }

        @Test
        void shouldNotUpdateIt() {
            rankCategoryRepository.update(rankCategory.toBuilder().name(UPDATED_NAME).build());

            assertSameEntity(readInAnotherOrganization(), rankCategory);
        }

        @Test
        void shouldNotDeleteIt() {
            rankCategoryRepository.delete(rankCategory.getId());

            assertSameEntity(readInAnotherOrganization(), rankCategory);
        }

        private RankCategory readInAnotherOrganization() {
            return OrganizationScope.call(
                anotherOrganizationId,
                () -> rankCategoryRepository.read(rankCategory.getId()).orElseThrow());
        }
    }
}
