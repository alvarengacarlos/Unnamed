package com.alvarengacarlos.unnamed.app.teacher;

import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertSameEntity;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
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
public class TeacherRepositoryIT implements OrganizationScoped {
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();
    private final TeacherInput.GetTeachers noFilter = new TeacherInput.GetTeachers(Optional.empty(), Optional.empty());

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private TeacherRepository teacherRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @Test
    void shouldSaveATeacher() {
        final Teacher teacher = fixtures.newTeacher(fixtures.createMember());

        teacherRepository.save(teacher);

        assertSameEntity(teacherRepository.read(teacher.getId()).orElseThrow(), teacher);
    }

    @Test
    void shouldNotReadATeacherThatDoesNotExist() {
        assertThat(teacherRepository.read(UUID.randomUUID())).isEmpty();
    }

    @Nested
    class ExistsByMemberId {
        @Test
        void shouldFindTheTeacherOfTheMember() {
            final Teacher teacher = fixtures.createTeacher();

            assertThat(teacherRepository.existsByMemberId(teacher.getMember().getId())).isTrue();
        }

        @Test
        void shouldNotFindATeacherForAnotherMember() {
            fixtures.createTeacher();

            assertThat(teacherRepository.existsByMemberId(fixtures.createMember().getId())).isFalse();
        }
    }

    @Nested
    class ReadMany {
        @Test
        void shouldReadNone() {
            final Page<Teacher> page = teacherRepository.read(new Pageable(0, 1), noFilter);

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isZero();
        }

        @Test
        void shouldReadAll() {
            final List<Teacher> teachers = List.of(
                fixtures.createTeacher(),
                fixtures.createTeacher(),
                fixtures.createTeacher());

            final Page<Teacher> page = teacherRepository.read(new Pageable(0, 10), noFilter);

            assertSameEntity(page.content(), teachers);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadTheLastPage() {
            fixtures.createTeacher();
            fixtures.createTeacher();
            fixtures.createTeacher();

            final Page<Teacher> page = teacherRepository.read(new Pageable(1, 2), noFilter);

            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadOnlyTheTeachersWithTheName() {
            final Teacher teacher = fixtures.createTeacher(
                fixtures.createMember(fixtures.createRank(), builder -> builder.name("João Carlos")));
            fixtures.createTeacher(
                fixtures.createMember(fixtures.createRank(), builder -> builder.name("Pedro Souza")));

            final Page<Teacher> page = teacherRepository.read(
                new Pageable(0, 10),
                new TeacherInput.GetTeachers(Optional.of("carlos"), Optional.empty()));

            assertSameEntity(page.content(), List.of(teacher));
            assertThat(page.totalElements()).isEqualTo(1);
        }

        @Test
        void shouldReadOnlyTheTeacherWithTheCpf() {
            final Teacher teacher = fixtures.createTeacher();
            fixtures.createTeacher();

            final Page<Teacher> page = teacherRepository.read(
                new Pageable(0, 10),
                new TeacherInput.GetTeachers(Optional.empty(), Optional.of(teacher.getMember().getCpf())));

            assertSameEntity(page.content(), List.of(teacher));
            assertThat(page.totalElements()).isEqualTo(1);
        }
    }

    @Test
    void shouldDeleteATeacher() {
        final Teacher teacher = fixtures.createTeacher();

        teacherRepository.delete(teacher.getId());

        assertThat(teacherRepository.read(teacher.getId())).isEmpty();
    }

    @Nested
    class WhenTheTeacherBelongsToAnotherOrganization {
        private Teacher teacher;

        @BeforeEach
        void beforeEach() {
            teacher = OrganizationScope.call(anotherOrganizationId, fixtures::createTeacher);
        }

        @Test
        void shouldNotReadIt() {
            assertThat(teacherRepository.read(teacher.getId())).isEmpty();
            assertThat(teacherRepository.existsByMemberId(teacher.getMember().getId())).isFalse();
            assertThat(teacherRepository.read(new Pageable(0, 1), noFilter).totalElements()).isZero();
        }

        @Test
        void shouldNotDeleteIt() {
            teacherRepository.delete(teacher.getId());

            final Teacher teacherInAnotherOrganization = OrganizationScope.call(
                anotherOrganizationId,
                () -> teacherRepository.read(teacher.getId()).orElseThrow());
            assertSameEntity(teacherInAnotherOrganization, teacher);
        }
    }
}
