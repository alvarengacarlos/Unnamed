package com.alvarengacarlos.unnamed.app.clazz;

import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertSameEntity;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
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
import com.alvarengacarlos.unnamed.app.teacher.Teacher;
import com.alvarengacarlos.unnamed.app.testsupport.Fixtures;
import com.alvarengacarlos.unnamed.app.testsupport.IntegrationTest;
import com.alvarengacarlos.unnamed.app.testsupport.OrganizationScope;
import com.alvarengacarlos.unnamed.app.testsupport.OrganizationScoped;

@IntegrationTest
public class ClazzRepositoryIT implements OrganizationScoped {
    private static final String UPDATED_NAME = "Updated name";
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();
    private final ClazzInput.GetClazzes noFilter = new ClazzInput.GetClazzes(Optional.empty());

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private ClazzRepository clazzRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @Test
    void shouldSaveAClazz() {
        final Clazz clazz = fixtures.newClazz(fixtures.createTeacher());

        clazzRepository.save(clazz);

        assertSameEntity(clazzRepository.read(clazz.getId()).orElseThrow(), clazz);
    }

    @Test
    void shouldUpdateAClazz() {
        final Clazz clazz = fixtures.createClazz();
        final Clazz updatedClazz = clazz.toBuilder()
            .name(UPDATED_NAME)
            .martialArt(clazz.getMartialArt() == Clazz.MartialArt.JUDO
                ? Clazz.MartialArt.KARATE
                : Clazz.MartialArt.JUDO)
            .startTime(LocalTime.of(8, 0))
            .endTime(LocalTime.of(9, 30))
            .weekdays(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
            .teacher(fixtures.createTeacher())
            .build();

        clazzRepository.update(updatedClazz);

        assertSameEntity(clazzRepository.read(clazz.getId()).orElseThrow(), updatedClazz);
    }

    @Test
    void shouldNotReadAClazzThatDoesNotExist() {
        assertThat(clazzRepository.read(UUID.randomUUID())).isEmpty();
    }

    @Nested
    class ReadMany {
        @Test
        void shouldReadNone() {
            final Page<Clazz> page = clazzRepository.read(new Pageable(0, 1), noFilter);

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isZero();
        }

        @Test
        void shouldReadAll() {
            final List<Clazz> clazzes = List.of(fixtures.createClazz(), fixtures.createClazz(), fixtures.createClazz());

            final Page<Clazz> page = clazzRepository.read(new Pageable(0, 10), noFilter);

            assertSameEntity(page.content(), clazzes);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadTheLastPage() {
            fixtures.createClazz();
            fixtures.createClazz();
            fixtures.createClazz();

            final Page<Clazz> page = clazzRepository.read(new Pageable(1, 2), noFilter);

            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadOnlyTheClazzesOfTheMartialArt() {
            final Teacher teacher = fixtures.createTeacher();
            final Clazz clazz = fixtures.newClazz(teacher).toBuilder().martialArt(Clazz.MartialArt.JUDO).build();
            clazzRepository.save(clazz);
            clazzRepository.save(fixtures.newClazz(teacher).toBuilder().martialArt(Clazz.MartialArt.KARATE).build());

            final Page<Clazz> page = clazzRepository.read(
                new Pageable(0, 10),
                new ClazzInput.GetClazzes(Optional.of(Clazz.MartialArt.JUDO)));

            assertSameEntity(page.content(), List.of(clazz));
            assertThat(page.totalElements()).isEqualTo(1);
        }
    }

    @Test
    void shouldDeleteAClazz() {
        final Clazz clazz = fixtures.createClazz();

        clazzRepository.delete(clazz.getId());

        assertThat(clazzRepository.read(clazz.getId())).isEmpty();
    }

    @Nested
    class WhenTheClazzBelongsToAnotherOrganization {
        private Clazz clazz;

        @BeforeEach
        void beforeEach() {
            clazz = OrganizationScope.call(anotherOrganizationId, fixtures::createClazz);
        }

        @Test
        void shouldNotReadIt() {
            assertThat(clazzRepository.read(clazz.getId())).isEmpty();
            assertThat(clazzRepository.read(new Pageable(0, 1), noFilter).totalElements()).isZero();
        }

        @Test
        void shouldNotUpdateIt() {
            clazzRepository.update(clazz.toBuilder().name(UPDATED_NAME).build());

            assertSameEntity(readInAnotherOrganization(), clazz);
        }

        @Test
        void shouldNotDeleteIt() {
            clazzRepository.delete(clazz.getId());

            assertSameEntity(readInAnotherOrganization(), clazz);
        }

        private Clazz readInAnotherOrganization() {
            return OrganizationScope.call(
                anotherOrganizationId,
                () -> clazzRepository.read(clazz.getId()).orElseThrow());
        }
    }
}
