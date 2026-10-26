package com.alvarengacarlos.unnamed.coreapi.tuition;

import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertSameEntity;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
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
public class TuitionRepositoryIT implements OrganizationScoped {
    private static final String UPDATED_NAME = "Updated name";
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private TuitionRepository tuitionRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @Test
    void shouldSaveATuition() {
        final Tuition tuition = fixtures.newTuition();

        tuitionRepository.save(tuition);

        assertSameEntity(tuitionRepository.read(tuition.getId()).orElseThrow(), tuition);
    }

    @Test
    void shouldUpdateATuition() {
        final Tuition tuition = fixtures.createTuition();
        final Tuition updatedTuition = tuition.toBuilder()
            .name(UPDATED_NAME)
            .price(new BigDecimal("199.90"))
            .build();

        tuitionRepository.update(updatedTuition);

        assertSameEntity(tuitionRepository.read(tuition.getId()).orElseThrow(), updatedTuition);
    }

    @Test
    void shouldNotReadATuitionThatDoesNotExist() {
        assertThat(tuitionRepository.read(UUID.randomUUID())).isEmpty();
    }

    @Nested
    class ReadMany {
        @Test
        void shouldReadNone() {
            final Page<Tuition> page = tuitionRepository.read(new Pageable(0, 1));

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isZero();
        }

        @Test
        void shouldReadAll() {
            final List<Tuition> tuitions = List.of(
                fixtures.createTuition(),
                fixtures.createTuition(),
                fixtures.createTuition());

            final Page<Tuition> page = tuitionRepository.read(new Pageable(0, 10));

            assertSameEntity(page.content(), tuitions);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadTheLastPage() {
            fixtures.createTuition();
            fixtures.createTuition();
            fixtures.createTuition();

            final Page<Tuition> page = tuitionRepository.read(new Pageable(1, 2));

            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(3);
        }
    }

    @Test
    void shouldDeleteATuition() {
        final Tuition tuition = fixtures.createTuition();

        tuitionRepository.delete(tuition.getId());

        assertThat(tuitionRepository.read(tuition.getId())).isEmpty();
    }

    @Nested
    class WhenTheTuitionBelongsToAnotherOrganization {
        private Tuition tuition;

        @BeforeEach
        void beforeEach() {
            tuition = OrganizationScope.call(anotherOrganizationId, fixtures::createTuition);
        }

        @Test
        void shouldNotReadIt() {
            assertThat(tuitionRepository.read(tuition.getId())).isEmpty();
            assertThat(tuitionRepository.read(new Pageable(0, 1)).totalElements()).isZero();
        }

        @Test
        void shouldNotUpdateIt() {
            tuitionRepository.update(tuition.toBuilder().name(UPDATED_NAME).build());

            assertSameEntity(readInAnotherOrganization(), tuition);
        }

        @Test
        void shouldNotDeleteIt() {
            tuitionRepository.delete(tuition.getId());

            assertSameEntity(readInAnotherOrganization(), tuition);
        }

        private Tuition readInAnotherOrganization() {
            return OrganizationScope.call(
                anotherOrganizationId,
                () -> tuitionRepository.read(tuition.getId()).orElseThrow());
        }
    }
}
