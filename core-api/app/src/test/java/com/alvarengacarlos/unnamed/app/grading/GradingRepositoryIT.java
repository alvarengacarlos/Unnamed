package com.alvarengacarlos.unnamed.app.grading;

import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertSameEntity;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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
public class GradingRepositoryIT implements OrganizationScoped {
    private static final String UPDATED_NAME = "Updated name";
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private GradingRepository gradingRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @Test
    void shouldSaveAGrading() {
        final Grading grading = fixtures.newGrading(fixtures.createRank());

        gradingRepository.save(grading);

        assertSameEntity(gradingRepository.read(grading.getId()).orElseThrow(), grading);
    }

    @Test
    void shouldUpdateAGrading() {
        final Grading grading = fixtures.createGrading();
        final Set<GradingPrice> gradingPrices = Set.of(
            new GradingPrice.Builder().price(new BigDecimal("150.00")).rank(fixtures.createRank()).build(),
            new GradingPrice.Builder().price(new BigDecimal("180.00")).rank(fixtures.createRank()).build());
        final Grading updatedGrading = grading.toBuilder()
            .name(UPDATED_NAME)
            .when(LocalDateTime.of(2030, 1, 1, 10, 0))
            .where("Updated place")
            .registrationPeriodStart(LocalDate.of(2029, 12, 1))
            .registrationPeriodEnd(LocalDate.of(2029, 12, 20))
            .gradingPrices(gradingPrices)
            .build();

        gradingRepository.update(updatedGrading);

        assertSameEntity(gradingRepository.read(grading.getId()).orElseThrow(), updatedGrading);
    }

    @Test
    void shouldNotReadAGradingThatDoesNotExist() {
        assertThat(gradingRepository.read(UUID.randomUUID())).isEmpty();
    }

    @Nested
    class ReadMany {
        @Test
        void shouldReadNone() {
            final Page<Grading> page = gradingRepository.read(new Pageable(0, 1));

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isZero();
        }

        @Test
        void shouldReadAll() {
            final List<Grading> gradings = List.of(
                fixtures.createGrading(),
                fixtures.createGrading(),
                fixtures.createGrading());

            final Page<Grading> page = gradingRepository.read(new Pageable(0, 10));

            assertSameEntity(page.content(), gradings);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadTheLastPage() {
            fixtures.createGrading();
            fixtures.createGrading();
            fixtures.createGrading();

            final Page<Grading> page = gradingRepository.read(new Pageable(1, 2));

            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(3);
        }
    }

    @Test
    void shouldDeleteAGrading() {
        final Grading grading = fixtures.createGrading();

        gradingRepository.delete(grading.getId());

        assertThat(gradingRepository.read(grading.getId())).isEmpty();
    }

    @Nested
    class WhenTheGradingBelongsToAnotherOrganization {
        private Grading grading;

        @BeforeEach
        void beforeEach() {
            grading = OrganizationScope.call(anotherOrganizationId, fixtures::createGrading);
        }

        @Test
        void shouldNotReadIt() {
            assertThat(gradingRepository.read(grading.getId())).isEmpty();
            assertThat(gradingRepository.read(new Pageable(0, 1)).totalElements()).isZero();
        }

        @Test
        void shouldNotUpdateIt() {
            gradingRepository.update(grading.toBuilder().name(UPDATED_NAME).build());

            assertSameEntity(readInAnotherOrganization(), grading);
        }

        @Test
        void shouldNotDeleteIt() {
            gradingRepository.delete(grading.getId());

            assertSameEntity(readInAnotherOrganization(), grading);
        }

        private Grading readInAnotherOrganization() {
            return OrganizationScope.call(
                anotherOrganizationId,
                () -> gradingRepository.read(grading.getId()).orElseThrow());
        }
    }
}
