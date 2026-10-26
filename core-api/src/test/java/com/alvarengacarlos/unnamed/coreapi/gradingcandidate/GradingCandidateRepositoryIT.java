package com.alvarengacarlos.unnamed.coreapi.gradingcandidate;

import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertSameEntity;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.alvarengacarlos.unnamed.coreapi.common.Page;
import com.alvarengacarlos.unnamed.coreapi.common.Pageable;
import com.alvarengacarlos.unnamed.coreapi.common.PaymentMethod;
import com.alvarengacarlos.unnamed.coreapi.member.Member;
import com.alvarengacarlos.unnamed.coreapi.testsupport.Fixtures;
import com.alvarengacarlos.unnamed.coreapi.testsupport.IntegrationTest;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScope;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScoped;

@IntegrationTest
public class GradingCandidateRepositoryIT implements OrganizationScoped {
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();
    private final GradingCandidateInput.GetGradingCandidates noFilter =
        new GradingCandidateInput.GetGradingCandidates(Optional.empty());

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private GradingCandidateRepository gradingCandidateRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @Test
    void shouldSaveAGradingCandidate() {
        final Member member = fixtures.createMember();
        final GradingCandidate gradingCandidate = fixtures.newGradingCandidate(
            fixtures.createGrading(member.getRank()),
            member);

        gradingCandidateRepository.save(gradingCandidate);

        assertSameEntity(gradingCandidateRepository.read(gradingCandidate.getId()).orElseThrow(), gradingCandidate);
    }

    @Test
    void shouldUpdateAGradingCandidate() {
        final GradingCandidate gradingCandidate = fixtures.createGradingCandidate();
        final GradingCandidate updatedGradingCandidate = gradingCandidate.toBuilder()
            .paymentMethod(gradingCandidate.getPaymentMethod() == PaymentMethod.PIX
                ? PaymentMethod.MONEY
                : PaymentMethod.PIX)
            .isPaid(!gradingCandidate.getIsPaid())
            .rank(fixtures.createRank())
            .build();

        gradingCandidateRepository.update(updatedGradingCandidate);

        assertSameEntity(
            gradingCandidateRepository.read(gradingCandidate.getId()).orElseThrow(),
            updatedGradingCandidate);
    }

    @Test
    void shouldNotReadAGradingCandidateThatDoesNotExist() {
        assertThat(gradingCandidateRepository.read(UUID.randomUUID())).isEmpty();
    }

    @Nested
    class Exists {
        @Test
        void shouldFindTheCandidacyOfTheMemberInTheGrading() {
            final GradingCandidate gradingCandidate = fixtures.createGradingCandidate();

            assertThat(gradingCandidateRepository.exists(
                gradingCandidate.getGrading().getId(),
                gradingCandidate.getMember().getId())).isTrue();
        }

        @Test
        void shouldNotFindACandidacyOfTheMemberInAnotherGrading() {
            final GradingCandidate gradingCandidate = fixtures.createGradingCandidate();

            assertThat(gradingCandidateRepository.exists(
                fixtures.createGrading().getId(),
                gradingCandidate.getMember().getId())).isFalse();
        }
    }

    @Nested
    class ReadMany {
        @Test
        void shouldReadNone() {
            final Page<GradingCandidate> page = gradingCandidateRepository.read(new Pageable(0, 1), noFilter);

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isZero();
        }

        @Test
        void shouldReadAll() {
            final List<GradingCandidate> gradingCandidates = List.of(
                fixtures.createGradingCandidate(),
                fixtures.createGradingCandidate(),
                fixtures.createGradingCandidate());

            final Page<GradingCandidate> page = gradingCandidateRepository.read(new Pageable(0, 10), noFilter);

            assertSameEntity(page.content(), gradingCandidates);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadTheLastPage() {
            fixtures.createGradingCandidate();
            fixtures.createGradingCandidate();
            fixtures.createGradingCandidate();

            final Page<GradingCandidate> page = gradingCandidateRepository.read(new Pageable(1, 2), noFilter);

            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadOnlyTheCandidatesOfTheGrading() {
            final GradingCandidate gradingCandidate = fixtures.createGradingCandidate();
            fixtures.createGradingCandidate();

            final Page<GradingCandidate> page = gradingCandidateRepository.read(
                new Pageable(0, 10),
                new GradingCandidateInput.GetGradingCandidates(Optional.of(gradingCandidate.getGrading().getId())));

            assertSameEntity(page.content(), List.of(gradingCandidate));
            assertThat(page.totalElements()).isEqualTo(1);
        }
    }

    @Test
    void shouldDeleteAGradingCandidate() {
        final GradingCandidate gradingCandidate = fixtures.createGradingCandidate();

        gradingCandidateRepository.delete(gradingCandidate.getId());

        assertThat(gradingCandidateRepository.read(gradingCandidate.getId())).isEmpty();
    }

    @Nested
    class WhenTheGradingCandidateBelongsToAnotherOrganization {
        private GradingCandidate gradingCandidate;

        @BeforeEach
        void beforeEach() {
            gradingCandidate = OrganizationScope.call(anotherOrganizationId, fixtures::createGradingCandidate);
        }

        @Test
        void shouldNotReadIt() {
            assertThat(gradingCandidateRepository.read(gradingCandidate.getId())).isEmpty();
            assertThat(gradingCandidateRepository.exists(
                gradingCandidate.getGrading().getId(),
                gradingCandidate.getMember().getId())).isFalse();
            assertThat(gradingCandidateRepository.read(new Pageable(0, 1), noFilter).totalElements()).isZero();
        }

        @Test
        void shouldNotUpdateIt() {
            gradingCandidateRepository.update(gradingCandidate.toBuilder()
                .isPaid(!gradingCandidate.getIsPaid())
                .build());

            assertSameEntity(readInAnotherOrganization(), gradingCandidate);
        }

        @Test
        void shouldNotDeleteIt() {
            gradingCandidateRepository.delete(gradingCandidate.getId());

            assertSameEntity(readInAnotherOrganization(), gradingCandidate);
        }

        private GradingCandidate readInAnotherOrganization() {
            return OrganizationScope.call(
                anotherOrganizationId,
                () -> gradingCandidateRepository.read(gradingCandidate.getId()).orElseThrow());
        }
    }
}
