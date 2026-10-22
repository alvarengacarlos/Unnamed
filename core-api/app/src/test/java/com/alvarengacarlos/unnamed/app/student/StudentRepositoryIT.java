package com.alvarengacarlos.unnamed.app.student;

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
public class StudentRepositoryIT implements OrganizationScoped {
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();
    private final StudentInput.GetStudents noFilter =
        new StudentInput.GetStudents(Optional.empty(), Optional.empty(), Optional.empty());

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private StudentRepository studentRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @Test
    void shouldSaveAStudent() {
        final Student student = fixtures.newStudent(fixtures.createMember());

        studentRepository.save(student);

        assertSameEntity(studentRepository.read(student.getId()).orElseThrow(), student);
    }

    @Test
    void shouldNotReadAStudentThatDoesNotExist() {
        assertThat(studentRepository.read(UUID.randomUUID())).isEmpty();
    }

    @Nested
    class ExistsByMemberId {
        @Test
        void shouldFindTheStudentOfTheMember() {
            final Student student = fixtures.createStudent();

            assertThat(studentRepository.existsByMemberId(student.getMember().getId())).isTrue();
        }

        @Test
        void shouldNotFindAStudentForAnotherMember() {
            fixtures.createStudent();

            assertThat(studentRepository.existsByMemberId(fixtures.createMember().getId())).isFalse();
        }
    }

    @Nested
    class ReadMany {
        @Test
        void shouldReadNone() {
            final Page<Student> page = studentRepository.read(new Pageable(0, 1), noFilter);

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isZero();
        }

        @Test
        void shouldReadAll() {
            final List<Student> students = List.of(
                fixtures.createStudent(),
                fixtures.createStudent(),
                fixtures.createStudent());

            final Page<Student> page = studentRepository.read(new Pageable(0, 10), noFilter);

            assertSameEntity(page.content(), students);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadTheLastPage() {
            fixtures.createStudent();
            fixtures.createStudent();
            fixtures.createStudent();

            final Page<Student> page = studentRepository.read(new Pageable(1, 2), noFilter);

            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadOnlyTheStudentOfTheMember() {
            final Student student = fixtures.createStudent();
            fixtures.createStudent();

            final Page<Student> page = studentRepository.read(
                new Pageable(0, 10),
                new StudentInput.GetStudents(
                    Optional.of(student.getMember().getId()),
                    Optional.empty(),
                    Optional.empty()));

            assertSameEntity(page.content(), List.of(student));
            assertThat(page.totalElements()).isEqualTo(1);
        }

        @Test
        void shouldReadOnlyTheStudentsWithTheName() {
            final Student student = fixtures.createStudent(
                fixtures.createMember(fixtures.createRank(), builder -> builder.name("João Carlos")));
            fixtures.createStudent(
                fixtures.createMember(fixtures.createRank(), builder -> builder.name("Pedro Souza")));

            final Page<Student> page = studentRepository.read(
                new Pageable(0, 10),
                new StudentInput.GetStudents(Optional.empty(), Optional.of("carlos"), Optional.empty()));

            assertSameEntity(page.content(), List.of(student));
            assertThat(page.totalElements()).isEqualTo(1);
        }

        @Test
        void shouldReadOnlyTheStudentWithTheCpf() {
            final Student student = fixtures.createStudent();
            fixtures.createStudent();

            final Page<Student> page = studentRepository.read(
                new Pageable(0, 10),
                new StudentInput.GetStudents(
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(student.getMember().getCpf())));

            assertSameEntity(page.content(), List.of(student));
            assertThat(page.totalElements()).isEqualTo(1);
        }
    }

    @Test
    void shouldDeleteAStudent() {
        final Student student = fixtures.createStudent();

        studentRepository.delete(student.getId());

        assertThat(studentRepository.read(student.getId())).isEmpty();
    }

    @Nested
    class WhenTheStudentBelongsToAnotherOrganization {
        private Student student;

        @BeforeEach
        void beforeEach() {
            student = OrganizationScope.call(anotherOrganizationId, fixtures::createStudent);
        }

        @Test
        void shouldNotReadIt() {
            assertThat(studentRepository.read(student.getId())).isEmpty();
            assertThat(studentRepository.existsByMemberId(student.getMember().getId())).isFalse();
            assertThat(studentRepository.read(new Pageable(0, 1), noFilter).totalElements()).isZero();
        }

        @Test
        void shouldNotDeleteIt() {
            studentRepository.delete(student.getId());

            final Student studentInAnotherOrganization = OrganizationScope.call(
                anotherOrganizationId,
                () -> studentRepository.read(student.getId()).orElseThrow());
            assertSameEntity(studentInAnotherOrganization, student);
        }
    }
}
