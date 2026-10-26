package com.alvarengacarlos.unnamed.coreapi.studentenrollment;

import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertSameEntity;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.alvarengacarlos.unnamed.coreapi.clazz.Clazz;
import com.alvarengacarlos.unnamed.coreapi.common.Page;
import com.alvarengacarlos.unnamed.coreapi.common.Pageable;
import com.alvarengacarlos.unnamed.coreapi.student.Student;
import com.alvarengacarlos.unnamed.coreapi.testsupport.Fixtures;
import com.alvarengacarlos.unnamed.coreapi.testsupport.IntegrationTest;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScope;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScoped;

@IntegrationTest
public class StudentEnrollmentRepositoryIT implements OrganizationScoped {
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();
    private final StudentEnrollmentInput.GetStudentEnrollments noFilter =
        new StudentEnrollmentInput.GetStudentEnrollments(Optional.empty(), Optional.empty());

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @Test
    void shouldSaveAStudentEnrollment() {
        final StudentEnrollment studentEnrollment = fixtures.newStudentEnrollment(
            fixtures.createTuition(),
            fixtures.createStudent(),
            fixtures.createClazz());

        studentEnrollmentRepository.save(studentEnrollment);

        assertSameEntity(studentEnrollmentRepository.read(studentEnrollment.getId()).orElseThrow(), studentEnrollment);
    }

    @Test
    void shouldUpdateAStudentEnrollment() {
        final StudentEnrollment studentEnrollment = fixtures.createStudentEnrollment();
        final StudentEnrollment updatedStudentEnrollment = studentEnrollment.toBuilder()
            .isActive(!studentEnrollment.getIsActive())
            .tuition(fixtures.createTuition())
            .build();

        studentEnrollmentRepository.update(updatedStudentEnrollment);

        assertSameEntity(
            studentEnrollmentRepository.read(studentEnrollment.getId()).orElseThrow(),
            updatedStudentEnrollment);
    }

    @Test
    void shouldNotReadAStudentEnrollmentThatDoesNotExist() {
        assertThat(studentEnrollmentRepository.read(UUID.randomUUID())).isEmpty();
    }

    @Nested
    class Exists {
        @Test
        void shouldFindTheEnrollmentOfTheStudentInTheClazz() {
            final StudentEnrollment studentEnrollment = fixtures.createStudentEnrollment();

            assertThat(studentEnrollmentRepository.exists(
                studentEnrollment.getStudent().getId(),
                studentEnrollment.getClazz().getId())).isTrue();
        }

        @Test
        void shouldNotFindAnEnrollmentOfTheStudentInAnotherClazz() {
            final StudentEnrollment studentEnrollment = fixtures.createStudentEnrollment();

            assertThat(studentEnrollmentRepository.exists(
                studentEnrollment.getStudent().getId(),
                fixtures.createClazz().getId())).isFalse();
        }
    }

    @Nested
    class ReadMany {
        @Test
        void shouldReadNone() {
            final Page<StudentEnrollment> page = studentEnrollmentRepository.read(new Pageable(0, 1), noFilter);

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isZero();
        }

        @Test
        void shouldReadAll() {
            final List<StudentEnrollment> studentEnrollments = List.of(
                fixtures.createStudentEnrollment(),
                fixtures.createStudentEnrollment(),
                fixtures.createStudentEnrollment());

            final Page<StudentEnrollment> page = studentEnrollmentRepository.read(new Pageable(0, 10), noFilter);

            assertSameEntity(page.content(), studentEnrollments);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadTheLastPage() {
            fixtures.createStudentEnrollment();
            fixtures.createStudentEnrollment();
            fixtures.createStudentEnrollment();

            final Page<StudentEnrollment> page = studentEnrollmentRepository.read(new Pageable(1, 2), noFilter);

            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Nested
        class WithFilters {
            private Student student;
            private Clazz clazz;
            private StudentEnrollment enrollmentOfTheStudentInTheClazz;
            private StudentEnrollment enrollmentOfTheStudentInAnotherClazz;
            private StudentEnrollment enrollmentOfAnotherStudentInTheClazz;

            @BeforeEach
            void beforeEach() {
                student = fixtures.createStudent();
                clazz = fixtures.createClazz();
                enrollmentOfTheStudentInTheClazz = fixtures.createStudentEnrollment(
                    fixtures.createTuition(),
                    student,
                    clazz);
                enrollmentOfTheStudentInAnotherClazz = fixtures.createStudentEnrollment(
                    fixtures.createTuition(),
                    student,
                    fixtures.createClazz());
                enrollmentOfAnotherStudentInTheClazz = fixtures.createStudentEnrollment(
                    fixtures.createTuition(),
                    fixtures.createStudent(),
                    clazz);
                fixtures.createStudentEnrollment();
            }

            @Test
            void shouldReadOnlyTheEnrollmentsOfTheStudent() {
                final Page<StudentEnrollment> page = studentEnrollmentRepository.read(
                    new Pageable(0, 10),
                    new StudentEnrollmentInput.GetStudentEnrollments(Optional.of(student.getId()), Optional.empty()));

                assertSameEntity(
                    page.content(),
                    List.of(enrollmentOfTheStudentInTheClazz, enrollmentOfTheStudentInAnotherClazz));
            }

            @Test
            void shouldReadOnlyTheEnrollmentsOfTheClazz() {
                final Page<StudentEnrollment> page = studentEnrollmentRepository.read(
                    new Pageable(0, 10),
                    new StudentEnrollmentInput.GetStudentEnrollments(Optional.empty(), Optional.of(clazz.getId())));

                assertSameEntity(
                    page.content(),
                    List.of(enrollmentOfTheStudentInTheClazz, enrollmentOfAnotherStudentInTheClazz));
            }

            @Test
            void shouldReadOnlyTheEnrollmentOfTheStudentInTheClazz() {
                final Page<StudentEnrollment> page = studentEnrollmentRepository.read(
                    new Pageable(0, 10),
                    new StudentEnrollmentInput.GetStudentEnrollments(
                        Optional.of(student.getId()),
                        Optional.of(clazz.getId())));

                assertSameEntity(page.content(), List.of(enrollmentOfTheStudentInTheClazz));
            }
        }
    }

    @Test
    void shouldDeleteAStudentEnrollment() {
        final StudentEnrollment studentEnrollment = fixtures.createStudentEnrollment();

        studentEnrollmentRepository.delete(studentEnrollment.getId());

        assertThat(studentEnrollmentRepository.read(studentEnrollment.getId())).isEmpty();
    }

    @Nested
    class WhenTheStudentEnrollmentBelongsToAnotherOrganization {
        private StudentEnrollment studentEnrollment;

        @BeforeEach
        void beforeEach() {
            studentEnrollment = OrganizationScope.call(anotherOrganizationId, fixtures::createStudentEnrollment);
        }

        @Test
        void shouldNotReadIt() {
            assertThat(studentEnrollmentRepository.read(studentEnrollment.getId())).isEmpty();
            assertThat(studentEnrollmentRepository.exists(
                studentEnrollment.getStudent().getId(),
                studentEnrollment.getClazz().getId())).isFalse();
            assertThat(studentEnrollmentRepository.read(new Pageable(0, 1), noFilter).totalElements()).isZero();
        }

        @Test
        void shouldNotUpdateIt() {
            studentEnrollmentRepository.update(studentEnrollment.toBuilder()
                .isActive(!studentEnrollment.getIsActive())
                .build());

            assertSameEntity(readInAnotherOrganization(), studentEnrollment);
        }

        @Test
        void shouldNotDeleteIt() {
            studentEnrollmentRepository.delete(studentEnrollment.getId());

            assertSameEntity(readInAnotherOrganization(), studentEnrollment);
        }

        private StudentEnrollment readInAnotherOrganization() {
            return OrganizationScope.call(
                anotherOrganizationId,
                () -> studentEnrollmentRepository.read(studentEnrollment.getId()).orElseThrow());
        }
    }
}
