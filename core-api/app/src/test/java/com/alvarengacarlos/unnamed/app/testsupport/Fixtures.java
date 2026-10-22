package com.alvarengacarlos.unnamed.app.testsupport;

import static org.instancio.Instancio.of;
import static org.instancio.Select.field;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import com.alvarengacarlos.unnamed.app.OrganizationContext;
import com.alvarengacarlos.unnamed.app.clazz.Clazz;
import com.alvarengacarlos.unnamed.app.clazz.ClazzRepository;
import com.alvarengacarlos.unnamed.app.grading.Grading;
import com.alvarengacarlos.unnamed.app.grading.GradingPrice;
import com.alvarengacarlos.unnamed.app.grading.GradingRepository;
import com.alvarengacarlos.unnamed.app.gradingcandidate.GradingCandidate;
import com.alvarengacarlos.unnamed.app.gradingcandidate.GradingCandidateRepository;
import com.alvarengacarlos.unnamed.app.member.Member;
import com.alvarengacarlos.unnamed.app.member.MemberRepository;
import com.alvarengacarlos.unnamed.app.rank.Rank;
import com.alvarengacarlos.unnamed.app.rank.RankRepository;
import com.alvarengacarlos.unnamed.app.rankcategory.RankCategory;
import com.alvarengacarlos.unnamed.app.rankcategory.RankCategoryRepository;
import com.alvarengacarlos.unnamed.app.student.Student;
import com.alvarengacarlos.unnamed.app.student.StudentRepository;
import com.alvarengacarlos.unnamed.app.studentenrollment.StudentEnrollment;
import com.alvarengacarlos.unnamed.app.studentenrollment.StudentEnrollmentRepository;
import com.alvarengacarlos.unnamed.app.teacher.Teacher;
import com.alvarengacarlos.unnamed.app.teacher.TeacherRepository;
import com.alvarengacarlos.unnamed.app.tournament.Tournament;
import com.alvarengacarlos.unnamed.app.tournament.TournamentDivisionCriterion;
import com.alvarengacarlos.unnamed.app.tournament.TournamentRepository;
import com.alvarengacarlos.unnamed.app.tournamentcompetitor.TournamentCompetitor;
import com.alvarengacarlos.unnamed.app.tournamentcompetitor.TournamentCompetitorRepository;
import com.alvarengacarlos.unnamed.app.tournamentmatch.TournamentMatch;
import com.alvarengacarlos.unnamed.app.tournamentmatch.TournamentMatchRepository;
import com.alvarengacarlos.unnamed.app.tuition.Tuition;
import com.alvarengacarlos.unnamed.app.tuition.TuitionRepository;
import com.alvarengacarlos.unnamed.app.tuitionpayment.TuitionPayment;
import com.alvarengacarlos.unnamed.app.tuitionpayment.TuitionPaymentRepository;

/**
 * Builds valid entities of the organization in context.
 * The new* methods only build them; the create* methods also save them, together with what they depend on
 * when it is not given.
 */
@TestComponent
public class Fixtures {
    private static final int CPF_BASE_DIGITS = 9;
    private static final int CPF_CHECK_DIGIT_MODULUS = 11;
    private static final int CPF_FIRST_CHECK_DIGIT_WEIGHT = 10;
    private static final int PHONE_NUMBER_DIGITS = 9;
    private static final String TOURNAMENT_COMPETITOR_A_FIELD = "tournamentCompetitorA";
    private static final String TOURNAMENT_COMPETITOR_B_FIELD = "tournamentCompetitorB";
    // The order of a rank is unique within its category, so it is not random.
    private static final AtomicInteger RANK_ORDER = new AtomicInteger();

    @Autowired
    private RankCategoryRepository rankCategoryRepository;

    @Autowired
    private RankRepository rankRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ClazzRepository clazzRepository;

    @Autowired
    private TuitionRepository tuitionRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private TuitionPaymentRepository tuitionPaymentRepository;

    @Autowired
    private GradingRepository gradingRepository;

    @Autowired
    private GradingCandidateRepository gradingCandidateRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TournamentCompetitorRepository tournamentCompetitorRepository;

    @Autowired
    private TournamentMatchRepository tournamentMatchRepository;

    /**
     * A valid CPF, formatted as 000.000.000-00.
     */
    public static String randomCpf() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final int[] digits = new int[CPF_BASE_DIGITS + 2];
        do {
            for (int i = 0; i < CPF_BASE_DIGITS; i++) {
                digits[i] = random.nextInt(10);
            }
        } while (IntStream.range(0, CPF_BASE_DIGITS).allMatch(i -> digits[i] == digits[0]));
        digits[CPF_BASE_DIGITS] = cpfCheckDigit(digits, CPF_BASE_DIGITS);
        digits[CPF_BASE_DIGITS + 1] = cpfCheckDigit(digits, CPF_BASE_DIGITS + 1);

        final StringBuilder cpf = new StringBuilder();
        for (int i = 0; i < digits.length; i++) {
            cpf.append(digits[i]);
            if (i == 2 || i == 5) {
                cpf.append('.');
            } else if (i == CPF_BASE_DIGITS - 1) {
                cpf.append('-');
            }
        }
        return cpf.toString();
    }

    private static int cpfCheckDigit(int[] digits, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += digits[i] * (CPF_FIRST_CHECK_DIGIT_WEIGHT + length - CPF_BASE_DIGITS - i);
        }
        final int remainder = sum * 10 % CPF_CHECK_DIGIT_MODULUS;
        return remainder == 10 ? 0 : remainder;
    }

    /**
     * A phone formatted as (00) 00000-0000.
     */
    public static String randomPhone() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final StringBuilder number = new StringBuilder();
        for (int i = 0; i < PHONE_NUMBER_DIGITS; i++) {
            number.append(random.nextInt(10));
        }
        return String.format("(%02d) %s-%s", random.nextInt(100), number.substring(0, 5), number.substring(5));
    }

    public RankCategory newRankCategory() {
        return of(RankCategory.class)
            .set(field(RankCategory::getOrganizationId), OrganizationContext.require())
            .create();
    }

    public RankCategory createRankCategory() {
        final RankCategory rankCategory = newRankCategory();
        rankCategoryRepository.save(rankCategory);
        return rankCategory;
    }

    public Rank newRank(RankCategory rankCategory) {
        return of(Rank.class)
            .set(field(Rank::getCategory), rankCategory)
            .set(field(Rank::getOrder), (byte) (RANK_ORDER.getAndIncrement() % Byte.MAX_VALUE + 1))
            .set(field(Rank::getOrganizationId), OrganizationContext.require())
            .create();
    }

    public Rank createRank() {
        return createRank(createRankCategory());
    }

    public Rank createRank(RankCategory rankCategory) {
        final Rank rank = newRank(rankCategory);
        rankRepository.save(rank);
        return rank;
    }

    public Member newMember(Rank rank) {
        return of(Member.class)
            .set(field(Member::getCpf), randomCpf())
            .set(field(Member::getPhone), randomPhone())
            .set(field(Member::getRank), rank)
            .set(field(Member::getOrganizationId), OrganizationContext.require())
            .create();
    }

    public Member createMember() {
        return createMember(createRank());
    }

    public Member createMember(Rank rank) {
        return createMember(rank, UnaryOperator.identity());
    }

    public Member createMember(Rank rank, UnaryOperator<Member.Builder> customizer) {
        final Member member = customizer.apply(newMember(rank).toBuilder()).build();
        memberRepository.save(member);
        return member;
    }

    public Teacher newTeacher(Member member) {
        return of(Teacher.class)
            .set(field(Teacher::getMember), member)
            .set(field(Teacher::getOrganizationId), OrganizationContext.require())
            .create();
    }

    public Teacher createTeacher() {
        return createTeacher(createMember());
    }

    public Teacher createTeacher(Member member) {
        final Teacher teacher = newTeacher(member);
        teacherRepository.save(teacher);
        return teacher;
    }

    public Student newStudent(Member member) {
        return of(Student.class)
            .set(field(Student::getMember), member)
            .set(field(Student::getOrganizationId), OrganizationContext.require())
            .create();
    }

    public Student createStudent() {
        return createStudent(createMember());
    }

    public Student createStudent(Member member) {
        final Student student = newStudent(member);
        studentRepository.save(student);
        return student;
    }

    public Clazz newClazz(Teacher teacher) {
        return of(Clazz.class)
            .set(field(Clazz::getTeacher), teacher)
            .set(field(Clazz::getOrganizationId), OrganizationContext.require())
            .create();
    }

    public Clazz createClazz() {
        return createClazz(createTeacher());
    }

    public Clazz createClazz(Teacher teacher) {
        final Clazz clazz = newClazz(teacher);
        clazzRepository.save(clazz);
        return clazz;
    }

    public Tuition newTuition() {
        return of(Tuition.class)
            .set(field(Tuition::getOrganizationId), OrganizationContext.require())
            .create();
    }

    public Tuition createTuition() {
        final Tuition tuition = newTuition();
        tuitionRepository.save(tuition);
        return tuition;
    }

    public StudentEnrollment newStudentEnrollment(Tuition tuition, Student student, Clazz clazz) {
        return of(StudentEnrollment.class)
            .set(field(StudentEnrollment::getTuition), tuition)
            .set(field(StudentEnrollment::getStudent), student)
            .set(field(StudentEnrollment::getClazz), clazz)
            .set(field(StudentEnrollment::getOrganizationId), OrganizationContext.require())
            .create();
    }

    public StudentEnrollment createStudentEnrollment() {
        return createStudentEnrollment(createTuition(), createStudent(), createClazz());
    }

    public StudentEnrollment createStudentEnrollment(Tuition tuition, Student student, Clazz clazz) {
        final StudentEnrollment studentEnrollment = newStudentEnrollment(tuition, student, clazz);
        studentEnrollmentRepository.save(studentEnrollment);
        return studentEnrollment;
    }

    public TuitionPayment newTuitionPayment(StudentEnrollment studentEnrollment) {
        return of(TuitionPayment.class)
            .set(field(TuitionPayment::getStudentEnrollment), studentEnrollment)
            .set(field(TuitionPayment::getOrganizationId), OrganizationContext.require())
            .create();
    }

    public TuitionPayment createTuitionPayment() {
        return createTuitionPayment(createStudentEnrollment());
    }

    public TuitionPayment createTuitionPayment(StudentEnrollment studentEnrollment) {
        final TuitionPayment tuitionPayment = newTuitionPayment(studentEnrollment);
        tuitionPaymentRepository.save(tuitionPayment);
        return tuitionPayment;
    }

    public Grading newGrading(Rank rank) {
        final GradingPrice gradingPrice = of(GradingPrice.class)
            .set(field(GradingPrice::getRank), rank)
            .create();
        return of(Grading.class)
            .set(field(Grading::getGradingPrices), Set.of(gradingPrice))
            .set(field(Grading::getOrganizationId), OrganizationContext.require())
            .create();
    }

    public Grading createGrading() {
        return createGrading(createRank());
    }

    public Grading createGrading(Rank rank) {
        final Grading grading = newGrading(rank);
        gradingRepository.save(grading);
        return grading;
    }

    public GradingCandidate newGradingCandidate(Grading grading, Member member) {
        return of(GradingCandidate.class)
            .set(field(GradingCandidate::getGrading), grading)
            .set(field(GradingCandidate::getRank), member.getRank())
            .set(field(GradingCandidate::getMember), member)
            .set(field(GradingCandidate::getOrganizationId), OrganizationContext.require())
            .create();
    }

    public GradingCandidate createGradingCandidate() {
        final Member member = createMember();
        return createGradingCandidate(createGrading(member.getRank()), member);
    }

    public GradingCandidate createGradingCandidate(Grading grading, Member member) {
        final GradingCandidate gradingCandidate = newGradingCandidate(grading, member);
        gradingCandidateRepository.save(gradingCandidate);
        return gradingCandidate;
    }

    /**
     * A ROUND_ROBIN tournament whose division criterion has every value but the rank.
     */
    public Tournament newTournament() {
        final TournamentDivisionCriterion tournamentDivisionCriterion = of(TournamentDivisionCriterion.class)
            .set(field(TournamentDivisionCriterion::getRank), Optional.empty())
            .create();
        return of(Tournament.class)
            .set(field(Tournament::getFormat), Tournament.TournamentFormat.ROUND_ROBIN)
            .set(field(Tournament::getTournamentDivisionCriterion), tournamentDivisionCriterion)
            .set(field(Tournament::getOrganizationId), OrganizationContext.require())
            .create();
    }

    public Tournament createTournament() {
        return createTournament(UnaryOperator.identity());
    }

    public Tournament createTournament(UnaryOperator<Tournament.Builder> customizer) {
        final Tournament tournament = customizer.apply(newTournament().toBuilder()).build();
        tournamentRepository.save(tournament);
        return tournament;
    }

    public TournamentCompetitor newTournamentCompetitor(Tournament tournament, Member member) {
        return of(TournamentCompetitor.class)
            .set(field(TournamentCompetitor::getTournament), tournament)
            .set(field(TournamentCompetitor::getMember), member)
            .set(field(TournamentCompetitor::getOrganizationId), OrganizationContext.require())
            .create();
    }

    public TournamentCompetitor createTournamentCompetitor(Tournament tournament) {
        return createTournamentCompetitor(tournament, createMember());
    }

    public TournamentCompetitor createTournamentCompetitor(Tournament tournament, Member member) {
        final TournamentCompetitor tournamentCompetitor = newTournamentCompetitor(tournament, member);
        tournamentCompetitorRepository.save(tournamentCompetitor);
        return tournamentCompetitor;
    }

    public TournamentMatch newTournamentMatch(
        Tournament tournament,
        TournamentCompetitor tournamentCompetitorA,
        TournamentCompetitor tournamentCompetitorB) {
        return of(TournamentMatch.class)
            .set(field(TournamentMatch::getTournament), tournament)
            .set(field(TournamentMatch.class, TOURNAMENT_COMPETITOR_A_FIELD), tournamentCompetitorA)
            .set(field(TournamentMatch.class, TOURNAMENT_COMPETITOR_B_FIELD), tournamentCompetitorB)
            .set(field(TournamentMatch::getOrganizationId), OrganizationContext.require())
            .create();
    }

    public TournamentMatch createTournamentMatch() {
        final Tournament tournament = createTournament();
        return createTournamentMatch(
            tournament,
            createTournamentCompetitor(tournament),
            createTournamentCompetitor(tournament));
    }

    public TournamentMatch createTournamentMatch(
        Tournament tournament,
        TournamentCompetitor tournamentCompetitorA,
        TournamentCompetitor tournamentCompetitorB) {
        final TournamentMatch tournamentMatch = newTournamentMatch(
            tournament,
            tournamentCompetitorA,
            tournamentCompetitorB);
        tournamentMatchRepository.save(tournamentMatch);
        return tournamentMatch;
    }
}
