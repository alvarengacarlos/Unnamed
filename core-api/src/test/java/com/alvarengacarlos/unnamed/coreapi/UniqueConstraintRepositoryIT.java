package com.alvarengacarlos.unnamed.coreapi;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import com.alvarengacarlos.unnamed.coreapi.clazz.Clazz;
import com.alvarengacarlos.unnamed.coreapi.grading.Grading;
import com.alvarengacarlos.unnamed.coreapi.gradingcandidate.GradingCandidate;
import com.alvarengacarlos.unnamed.coreapi.gradingcandidate.GradingCandidateRepository;
import com.alvarengacarlos.unnamed.coreapi.member.Member;
import com.alvarengacarlos.unnamed.coreapi.member.MemberRepository;
import com.alvarengacarlos.unnamed.coreapi.rank.Rank;
import com.alvarengacarlos.unnamed.coreapi.rank.RankRepository;
import com.alvarengacarlos.unnamed.coreapi.student.Student;
import com.alvarengacarlos.unnamed.coreapi.student.StudentRepository;
import com.alvarengacarlos.unnamed.coreapi.studentenrollment.StudentEnrollment;
import com.alvarengacarlos.unnamed.coreapi.studentenrollment.StudentEnrollmentRepository;
import com.alvarengacarlos.unnamed.coreapi.teacher.TeacherRepository;
import com.alvarengacarlos.unnamed.coreapi.testsupport.Fixtures;
import com.alvarengacarlos.unnamed.coreapi.testsupport.IntegrationTest;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScope;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScoped;
import com.alvarengacarlos.unnamed.coreapi.tournament.Tournament;
import com.alvarengacarlos.unnamed.coreapi.tournamentcompetitor.TournamentCompetitor;
import com.alvarengacarlos.unnamed.coreapi.tournamentcompetitor.TournamentCompetitorRepository;
import com.alvarengacarlos.unnamed.coreapi.tournamentmatch.TournamentMatch;
import com.alvarengacarlos.unnamed.coreapi.tournamentmatch.TournamentMatchRepository;
import com.alvarengacarlos.unnamed.coreapi.tuitionpayment.TuitionPayment;
import com.alvarengacarlos.unnamed.coreapi.tuitionpayment.TuitionPaymentRepository;

/**
 * The services check for duplicates before saving, but only the database stops two concurrent requests that pass
 * that check together. Each test saves what such a request would.
 */
@IntegrationTest
public class UniqueConstraintRepositoryIT implements OrganizationScoped {
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private RankRepository rankRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private TuitionPaymentRepository tuitionPaymentRepository;

    @Autowired
    private GradingCandidateRepository gradingCandidateRepository;

    @Autowired
    private TournamentCompetitorRepository tournamentCompetitorRepository;

    @Autowired
    private TournamentMatchRepository tournamentMatchRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @Test
    void shouldNotSaveTwoRanksWithTheSameOrderInACategory() {
        final Rank rank = fixtures.createRank();

        assertThatThrownBy(() -> rankRepository.save(fixtures.newRank(rank.getCategory())
            .toBuilder()
            .order(rank.getOrder())
            .build()))
            .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void shouldNotSaveTwoMembersWithTheSameCpf() {
        final Member member = fixtures.createMember();

        assertThatThrownBy(() -> memberRepository.save(fixtures.newMember(member.getRank())
            .toBuilder()
            .cpf(member.getCpf())
            .build()))
            .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void shouldSaveMembersWithTheSameCpfInDifferentOrganizations() {
        final Member member = fixtures.createMember();

        assertThatCode(() -> OrganizationScope.run(
            anotherOrganizationId,
            () -> memberRepository.save(fixtures.newMember(fixtures.createRank())
                .toBuilder()
                .cpf(member.getCpf())
                .build())))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldNotSaveTwoTeachersForAMember() {
        final Member member = fixtures.createMember();
        fixtures.createTeacher(member);

        assertThatThrownBy(() -> teacherRepository.save(fixtures.newTeacher(member)))
            .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void shouldNotSaveTwoStudentsForAMember() {
        final Member member = fixtures.createMember();
        fixtures.createStudent(member);

        assertThatThrownBy(() -> studentRepository.save(fixtures.newStudent(member)))
            .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void shouldNotEnrollAStudentTwiceInAClazz() {
        final Student student = fixtures.createStudent();
        final Clazz clazz = fixtures.createClazz();
        fixtures.createStudentEnrollment(fixtures.createTuition(), student, clazz);

        assertThatThrownBy(() -> studentEnrollmentRepository.save(
            fixtures.newStudentEnrollment(fixtures.createTuition(), student, clazz)))
            .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void shouldNotSaveTwoTuitionPaymentsForTheSameMonth() {
        final TuitionPayment tuitionPayment = fixtures.createTuitionPayment();

        assertThatThrownBy(() -> tuitionPaymentRepository.save(
            fixtures.newTuitionPayment(tuitionPayment.getStudentEnrollment())
                .toBuilder()
                .month(tuitionPayment.getMonth())
                .year(tuitionPayment.getYear())
                .build()))
            .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void shouldNotUpdateATuitionPaymentToAMonthThatIsRegistered() {
        final StudentEnrollment studentEnrollment = fixtures.createStudentEnrollment();
        final TuitionPayment tuitionPayment = fixtures.createTuitionPayment(studentEnrollment);
        final TuitionPayment anotherTuitionPayment = fixtures.createTuitionPayment(studentEnrollment);

        assertThatThrownBy(() -> tuitionPaymentRepository.update(anotherTuitionPayment.toBuilder()
            .month(tuitionPayment.getMonth())
            .year(tuitionPayment.getYear())
            .build()))
            .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void shouldNotSaveTwoCandidatesForAMemberInAGrading() {
        final GradingCandidate gradingCandidate = fixtures.createGradingCandidate();
        final Grading grading = gradingCandidate.getGrading();

        assertThatThrownBy(() -> gradingCandidateRepository.save(
            fixtures.newGradingCandidate(grading, gradingCandidate.getMember())))
            .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void shouldNotSaveTwoCompetitorsForAMemberInATournament() {
        final Tournament tournament = fixtures.createTournament();
        final TournamentCompetitor tournamentCompetitor = fixtures.createTournamentCompetitor(tournament);

        assertThatThrownBy(() -> tournamentCompetitorRepository.save(
            fixtures.newTournamentCompetitor(tournament, tournamentCompetitor.getMember())))
            .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void shouldNotSaveTheSameMatchTwice() {
        final TournamentMatch tournamentMatch = fixtures.createTournamentMatch();

        assertThatThrownBy(() -> tournamentMatchRepository.save(fixtures.newTournamentMatch(
            tournamentMatch.getTournament(),
            tournamentMatch.getCompetitorA(),
            tournamentMatch.getCompetitorB())))
            .isInstanceOf(DuplicateKeyException.class);
    }
}
