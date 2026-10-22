import { Rank, RankCategory, type RankCategoryOutput, type RankOutput } from "../../src/rank/rank.type"
import { Gender, Member, Student, Teacher, type MemberOutput, type StudentOutput, type TeacherOutput } from "../../src/member/member.type"
import { Tuition, type TuitionOutput } from "../../src/tuition/tuition.type"
import { Clazz, MartialArt, type ClazzOutput } from "../../src/clazz/clazz.type"
import { StudentEnrollment, type StudentEnrollmentOutput } from "../../src/student-enrollment/student-enrollment.type"
import { Grading, type GradingOutput } from "../../src/grading/grading.type"
import { GradingCandidate, type GradingCandidateOutput } from "../../src/grading-candidate/grading-candidate.type"
import { TuitionPayment, type TuitionPaymentOutput } from "../../src/tuition-payment/tuition-payment.type"
import { Tournament, TournamentFormat, type TournamentOutput } from "../../src/tournament/tournament.type"
import { TournamentCompetitor, type TournamentCompetitorOutput } from "../../src/tournament-competitor/tournament-competitor.type"
import { TournamentMatch, type TournamentMatchOutput } from "../../src/tournament-match/tournament-match.type"
import { DayOfWeek, PaymentMethod } from "../../src/common/common.type"
import type { ApiClient } from "./api-client"
import { uniqueCpf, uniqueName } from "./unique"

/**
 * The data a test needs before it starts, created by the API (fake or real). Each record has unique values, and what
 * it references is created too when not given.
 */
export class TestData {
    constructor(private readonly api: ApiClient) {}

    async rankCategory(fields: Partial<{ name: string }> = {}): Promise<RankCategory> {
        return RankCategory.from(await this.api.post<RankCategoryOutput>("/rank-categories", {
            name: uniqueName("Categoria"),
            ...fields
        }))
    }

    async rank(fields: Partial<{ name: string, order: number, category: RankCategory }> = {}): Promise<Rank> {
        const category = fields.category ?? await this.rankCategory()
        return Rank.from(await this.api.post<RankOutput>("/ranks", {
            name: fields.name ?? uniqueName("Faixa"),
            order: fields.order ?? 1,
            categoryId: category.id
        }))
    }

    async member(fields: Partial<{ name: string, cpf: string, rank: Rank, isCompetitor: boolean }> = {}): Promise<Member> {
        const rank = fields.rank ?? await this.rank()
        return Member.from(await this.api.post<MemberOutput>("/members", {
            name: fields.name ?? uniqueName("Membro"),
            phone: "(11) 91234-5678",
            cpf: fields.cpf ?? uniqueCpf(),
            address: "Rua A, 1",
            gender: Gender.MALE,
            weight: 80,
            height: 1.8,
            birthDate: "2000-01-01",
            isPwd: false,
            isCompetitor: fields.isCompetitor ?? true,
            rankId: rank.id
        }))
    }

    async student(fields: Partial<{ member: Member }> = {}): Promise<Student> {
        const member = fields.member ?? await this.member()
        return Student.from(await this.api.post<StudentOutput>("/students", { memberId: member.id }))
    }

    async teacher(fields: Partial<{ member: Member }> = {}): Promise<Teacher> {
        const member = fields.member ?? await this.member()
        return Teacher.from(await this.api.post<TeacherOutput>("/teachers", { memberId: member.id }))
    }

    async tuition(): Promise<Tuition> {
        return Tuition.from(await this.api.post<TuitionOutput>("/tuitions", {
            name: uniqueName("Mensalidade"),
            price: 100
        }))
    }

    async clazz(fields: Partial<{ teacher: Teacher, martialArt: MartialArt }> = {}): Promise<Clazz> {
        const teacher = fields.teacher ?? await this.teacher()
        return Clazz.from(await this.api.post<ClazzOutput>("/clazzes", {
            name: uniqueName("Turma"),
            martialArt: fields.martialArt ?? MartialArt.JUDO,
            startTime: "18:00",
            endTime: "19:00",
            weekdays: [DayOfWeek.MONDAY],
            teacherId: teacher.id
        }))
    }

    async studentEnrollment(fields: Partial<{ student: Student, clazz: Clazz, tuition: Tuition }> = {}): Promise<StudentEnrollment> {
        const student = fields.student ?? await this.student()
        const clazz = fields.clazz ?? await this.clazz()
        const tuition = fields.tuition ?? await this.tuition()
        return StudentEnrollment.from(await this.api.post<StudentEnrollmentOutput>("/student-enrollments", {
            studentId: student.id,
            clazzId: clazz.id,
            tuitionId: tuition.id
        }))
    }

    // The month as the API takes it, January being 1
    async tuitionPayment(fields: Partial<{ studentEnrollment: StudentEnrollment, month: number, year: number }> = {}): Promise<TuitionPayment> {
        const studentEnrollment = fields.studentEnrollment ?? await this.studentEnrollment()
        return TuitionPayment.from(await this.api.post<TuitionPaymentOutput>("/tuition-payments", {
            month: fields.month ?? 1,
            year: fields.year ?? 2030,
            isPaid: false,
            studentEnrollmentId: studentEnrollment.id
        }))
    }

    async grading(fields: Partial<{ rank: Rank }> = {}): Promise<Grading> {
        const rank = fields.rank ?? await this.rank()
        return Grading.from(await this.api.post<GradingOutput>("/gradings", {
            name: uniqueName("Graduação"),
            when: "2030-01-10T10:00:00",
            where: "Ginásio",
            registrationPeriodStart: "2029-12-01",
            registrationPeriodEnd: "2029-12-20",
            prices: { [rank.id]: 100 }
        }))
    }

    async gradingCandidate(fields: Partial<{ grading: Grading, member: Member }> = {}): Promise<GradingCandidate> {
        const grading = fields.grading ?? await this.grading()
        const member = fields.member ?? await this.member()
        return GradingCandidate.from(await this.api.post<GradingCandidateOutput>("/grading-candidates", {
            paymentMethod: PaymentMethod.PIX,
            isPaid: false,
            rankId: grading.gradingPrices[0].rank.id,
            gradingId: grading.id,
            memberId: member.id
        }))
    }

    // The rank, as a criterion of division, limits the possible competitors to the members of the test
    async tournament(fields: Partial<{ format: TournamentFormat, rank: Rank }> = {}): Promise<Tournament> {
        return Tournament.from(await this.api.post<TournamentOutput>("/tournaments", {
            name: uniqueName("Torneio"),
            format: fields.format ?? TournamentFormat.SINGLE_ELIMINATION,
            divisionCriterion: { GENDER: null, WEIGHT: null, HEIGHT: null, AGE: null, IS_PWD: null, RANK: fields.rank?.id ?? null },
            participationPrice: 50,
            startDateTime: "2030-02-01T09:00:00",
            endDateTime: "2030-02-01T18:00:00",
            where: "Ginásio"
        }))
    }

    async tournamentCompetitor(fields: Partial<{ tournament: Tournament, member: Member }> = {}): Promise<TournamentCompetitor> {
        const tournament = fields.tournament ?? await this.tournament()
        const member = fields.member ?? await this.member()
        return TournamentCompetitor.from(await this.api.post<TournamentCompetitorOutput>("/tournament-competitors", {
            paymentMethod: PaymentMethod.PIX,
            isPaid: true,
            tournamentId: tournament.id,
            memberId: member.id
        }))
    }

    async tournamentMatch(fields: Partial<{ tournament: Tournament, competitorA: TournamentCompetitor, competitorB: TournamentCompetitor }> = {}): Promise<TournamentMatch> {
        const tournament = fields.tournament ?? fields.competitorA?.tournament ?? await this.tournament()
        const competitorA = fields.competitorA ?? await this.tournamentCompetitor({ tournament })
        const competitorB = fields.competitorB ?? await this.tournamentCompetitor({ tournament })
        return TournamentMatch.from(await this.api.post<TournamentMatchOutput>("/tournament-matches", {
            when: "2030-02-01T10:00:00",
            where: "Tatame 1",
            tournamentCompetitorAId: competitorA.id,
            tournamentCompetitorBId: competitorB.id,
            tournamentId: tournament.id
        }))
    }
}
