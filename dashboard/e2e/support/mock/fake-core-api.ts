import { randomUUID } from "crypto"
import type { Page } from "@playwright/test"
import type { RankCategoryOutput, RankOutput } from "../../../src/rank/rank.type"
import type { Gender, MemberOutput, StudentOutput, TeacherOutput } from "../../../src/member/member.type"
import type { TuitionOutput } from "../../../src/tuition/tuition.type"
import type { ClazzOutput, MartialArt } from "../../../src/clazz/clazz.type"
import type { StudentEnrollmentOutput } from "../../../src/student-enrollment/student-enrollment.type"
import type { TuitionPaymentOutput } from "../../../src/tuition-payment/tuition-payment.type"
import type { GradingOutput } from "../../../src/grading/grading.type"
import type { GradingCandidateOutput } from "../../../src/grading-candidate/grading-candidate.type"
import { TournamentFormat, type TournamentOutput } from "../../../src/tournament/tournament.type"
import type { TournamentCompetitorOutput } from "../../../src/tournament-competitor/tournament-competitor.type"
import { TournamentMatchResult, type TournamentMatchOutput } from "../../../src/tournament-match/tournament-match.type"
import { DayOfWeek, PaymentMethod, type PaginationOutput } from "../../../src/common/common.type"
import type { AssistantOutput } from "../../../src/assistant/assistant.type"
import { mockApiBaseUrl } from "../config"
import { isValidCpf } from "../unique"
import { pathOfApi } from "../api-client"

const PROBLEM_TYPE_PREFIX = "urn:unnamed:problem:"
const MAX_PAGE_SIZE = 50
// The limits of the core-api (CommonInput)
const TEXT_MAX_LENGTH = 200
const BULK_MAX_SIZE = 200
export const mockOrganizationId = "00000000-0000-4000-8000-000000000001"

export interface FakeRequest {
    readonly method: string
    // Without the base URL, as /members/{id}
    readonly path: string
    readonly query: URLSearchParams
    readonly body: unknown
}

export interface FakeResponse {
    readonly status: number
    readonly body: unknown
}

/**
 * The core-api in memory, answering as the real one: the same paths, outputs, pagination (newest first), filters and
 * problem details. The outputs are typed with the ones of the app, so a change of the contract breaks these types too.
 * Each test has its own instance, so the tests do not see the data of one another.
 *
 * Only the resources the tests cover are here; any other request is answered with 501, so a missing one is evident.
 */
export class FakeCoreApi {
    private readonly rankCategories: Array<RankCategoryRow> = []
    private readonly ranks: Array<RankRow> = []
    private readonly members: Array<MemberRow> = []
    private readonly students: Array<StudentRow> = []
    private readonly teachers: Array<TeacherRow> = []
    private readonly tuitions: Array<TuitionRow> = []
    private readonly clazzes: Array<ClazzRow> = []
    private readonly studentEnrollments: Array<StudentEnrollmentRow> = []
    private readonly tuitionPayments: Array<TuitionPaymentRow> = []
    private readonly gradings: Array<GradingRow> = []
    private readonly gradingCandidates: Array<GradingCandidateRow> = []
    private readonly tournaments: Array<TournamentRow> = []
    private readonly tournamentCompetitors: Array<TournamentCompetitorRow> = []
    private readonly tournamentMatches: Array<TournamentMatchRow> = []

    // Each record a millisecond after the previous one, so the order of the creation is always the one of the listing
    private clock = Date.now()

    // Every request the app made, for the tests that check what was sent
    readonly requests: Array<FakeRequest> = []

    handle(request: FakeRequest): FakeResponse {
        this.requests.push(request)
        try {
            return this.route(request)
        } catch (e) {
            if (e instanceof Problem) {
                return { status: e.status, body: e.toProblemDetail() }
            }
            throw e
        }
    }

    private route(request: FakeRequest): FakeResponse {
        if (request.path === "/assistant" && request.method === "POST") {
            // As the real one, which does not validate the message yet
            return ok(this.reply(String(asBody(request.body).message)))
        }

        const [, resource, id, action, ...rest] = request.path.split("/")
        const handler = this.handlers()[resource]
        if (!handler || rest.length > 0) {
            throw notMocked(request)
        }

        if (id === undefined) {
            switch (request.method) {
                case "GET": return ok(this.page(handler, request.query))
                case "POST": return { status: 201, body: handler.toOutput(handler.create(asBody(request.body))) }
            }
        } else if (id === "bulk" && action === undefined) {
            if (request.method === "POST" && handler.createInBulk) {
                return { status: 201, body: handler.createInBulk(asBody(request.body)).map(row => handler.toOutput(row)) }
            }
        } else {
            const row = this.find(handler.rows, id, handler.name)
            if (action !== undefined) {
                const answer = handler.actions?.[action]
                if (request.method === "GET" && answer) {
                    return ok(answer(row, request.query))
                }
                throw notMocked(request)
            }

            switch (request.method) {
                case "GET": return ok(handler.toOutput(row))
                case "PUT":
                    if (handler.update) {
                        handler.update(row, asBody(request.body))
                        row.updatedAt = this.now()
                        return ok(handler.toOutput(row))
                    }
                    break
                case "DELETE":
                    if (handler.isInUse(row)) {
                        throw new Problem(409, "resource-in-use", "Resource in use")
                    }
                    handler.rows.splice(handler.rows.indexOf(row), 1)
                    return ok(handler.toOutput(row))
            }
        }

        throw notMocked(request)
    }

    // The model is not simulated: the reply only repeats the message, in Markdown as the real one
    private reply(message: string): AssistantOutput {
        return { reply: `**Mensagem recebida:** ${message}` }
    }

    private page<O>(handler: Handler<Row, O>, query: URLSearchParams): PaginationOutput<O> {
        const number = Number(query.get("page") ?? 0)
        const size = Number(query.get("pageSize") ?? 10)
        if (!(size >= 1 && size <= MAX_PAGE_SIZE)) {
            throw invalidInput({ pageSize: `must be between 1 and ${MAX_PAGE_SIZE}` })
        }

        // The newest first, as the core-api
        const rows = handler.rows.filter(row => handler.filter(row, query)).reverse()
        return {
            content: rows.slice(number * size, (number + 1) * size).map(row => handler.toOutput(row)),
            page: { number, size, totalElements: rows.length, totalPages: Math.ceil(rows.length / size) }
        }
    }

    private handlers(): Record<string, Handler<Row, unknown>> {
        return {
            "rank-categories": {
                name: "rank-category",
                rows: this.rankCategories,
                create: body => this.insert(this.rankCategories, { name: requireText(body, "name") }),
                update: (row, body) => Object.assign(row, { name: requireText(body, "name") }),
                filter: () => true,
                isInUse: row => this.ranks.some(r => r.categoryId === row.id),
                toOutput: row => this.rankCategoryOutput(row as RankCategoryRow)
            },
            "ranks": {
                name: "rank",
                rows: this.ranks,
                create: body => {
                    const fields = validate(body, { name: isText, order: isPositiveInteger, categoryId: isText })
                    this.find(this.rankCategories, fields.categoryId as string, "rank-category")
                    if (this.ranks.some(r => r.categoryId === fields.categoryId && r.order === fields.order)) {
                        throw new Problem(409, "rank-exists", "Rank exists")
                    }
                    return this.insert(this.ranks, fields as Omit<RankRow, keyof AuditRow>)
                },
                update: (row, body) => Object.assign(row, { name: requireText(body, "name") }),
                filter: (row, query) => matches(query, "categoryId", (row as RankRow).categoryId),
                isInUse: row => this.members.some(m => m.rankId === row.id)
                    || this.gradings.some(g => g.prices.some(p => p.rankId === row.id))
                    || this.gradingCandidates.some(gc => gc.rankId === row.id)
                    || this.tournaments.some(t => t.divisionCriterion.RANK === row.id),
                toOutput: row => this.rankOutput(row as RankRow)
            },
            "members": {
                name: "member",
                rows: this.members,
                create: body => this.insert(this.members, this.memberFields(body)),
                update: (row, body) => Object.assign(row, this.memberFields(body, row.id)),
                filter: (row, query) => matchesMember(row as MemberRow, query),
                isInUse: row => [...this.students, ...this.teachers, ...this.gradingCandidates, ...this.tournamentCompetitors]
                    .some(r => r.memberId === row.id),
                toOutput: row => this.memberOutput(row as MemberRow)
            },
            "students": this.memberRoleHandler(
                "student",
                this.students,
                row => this.studentEnrollments.some(se => se.studentId === row.id)
            ),
            "teachers": this.memberRoleHandler(
                "teacher",
                this.teachers,
                row => this.clazzes.some(c => c.teacherId === row.id)
            ),
            "tuitions": {
                name: "tuition",
                rows: this.tuitions,
                create: body => this.insert(this.tuitions, this.tuitionFields(body)),
                update: (row, body) => Object.assign(row, this.tuitionFields(body)),
                filter: () => true,
                isInUse: row => this.studentEnrollments.some(se => se.tuitionId === row.id),
                toOutput: row => this.tuitionOutput(row as TuitionRow)
            },
            "clazzes": {
                name: "clazz",
                rows: this.clazzes,
                create: body => this.insert(this.clazzes, this.clazzFields(body)),
                update: (row, body) => Object.assign(row, this.clazzFields(body)),
                filter: (row, query) => matches(query, "martialArt", (row as ClazzRow).martialArt),
                isInUse: row => this.studentEnrollments.some(se => se.clazzId === row.id),
                toOutput: row => this.clazzOutput(row as ClazzRow)
            },
            "student-enrollments": {
                name: "student-enrollment",
                rows: this.studentEnrollments,
                create: body => {
                    const fields = validate(body, { tuitionId: isText, studentId: isText, clazzId: isText })
                    this.find(this.tuitions, fields.tuitionId as string, "tuition")
                    this.find(this.students, fields.studentId as string, "student")
                    this.find(this.clazzes, fields.clazzId as string, "clazz")
                    if (this.studentEnrollments.some(se => se.studentId === fields.studentId && se.clazzId === fields.clazzId)) {
                        throw new Problem(409, "student-enrollment-exists", "Student enrollment exists")
                    }
                    return this.insert(this.studentEnrollments, { ...fields, isActive: true } as Omit<StudentEnrollmentRow, keyof AuditRow>)
                },
                update: (row, body) => {
                    const fields = validate(body, { isActive: isBoolean, tuitionId: isText })
                    this.find(this.tuitions, fields.tuitionId as string, "tuition")
                    Object.assign(row, fields)
                },
                filter: (row, query) => {
                    const studentEnrollment = row as StudentEnrollmentRow
                    return matches(query, "studentId", studentEnrollment.studentId)
                        && matches(query, "clazzId", studentEnrollment.clazzId)
                },
                isInUse: row => this.tuitionPayments.some(tp => tp.studentEnrollmentId === row.id),
                toOutput: row => this.studentEnrollmentOutput(row as StudentEnrollmentRow)
            },
            "tuition-payments": {
                name: "tuition-payment",
                rows: this.tuitionPayments,
                create: body => {
                    const studentEnrollmentId = requireText(body, "studentEnrollmentId")
                    this.find(this.studentEnrollments, studentEnrollmentId, "student-enrollment")
                    const fields = this.tuitionPaymentFields(body, studentEnrollmentId)
                    return this.insert(this.tuitionPayments, { ...fields, studentEnrollmentId })
                },
                update: (row, body) => {
                    const tuitionPayment = row as TuitionPaymentRow
                    Object.assign(row, this.tuitionPaymentFields(body, tuitionPayment.studentEnrollmentId, row.id))
                },
                filter: (row, query) => {
                    const studentEnrollment = this.find(
                        this.studentEnrollments,
                        (row as TuitionPaymentRow).studentEnrollmentId,
                        "student-enrollment"
                    )
                    return matches(query, "studentId", studentEnrollment.studentId)
                },
                isInUse: () => false,
                toOutput: row => this.tuitionPaymentOutput(row as TuitionPaymentRow)
            },
            "gradings": {
                name: "grading",
                rows: this.gradings,
                create: body => this.insert(this.gradings, this.gradingFields(body)),
                update: (row, body) => Object.assign(row, this.gradingFields(body)),
                filter: () => true,
                isInUse: row => this.gradingCandidates.some(gc => gc.gradingId === row.id),
                toOutput: row => this.gradingOutput(row as GradingRow)
            },
            "grading-candidates": {
                name: "grading-candidate",
                rows: this.gradingCandidates,
                create: body => {
                    const fields = validate(body, {
                        paymentMethod: isPaymentMethod,
                        isPaid: isBoolean,
                        rankId: isText,
                        gradingId: isText,
                        memberId: isText
                    }) as Omit<GradingCandidateRow, keyof AuditRow>
                    this.find(this.ranks, fields.rankId, "rank")
                    this.find(this.gradings, fields.gradingId, "grading")
                    this.find(this.members, fields.memberId, "member")
                    if (this.gradingCandidates.some(gc => gc.gradingId === fields.gradingId && gc.memberId === fields.memberId)) {
                        throw new Problem(409, "grading-candidate-exists", "Grading candidate exists")
                    }
                    return this.insert(this.gradingCandidates, fields)
                },
                update: (row, body) => {
                    const fields = validate(body, { paymentMethod: isPaymentMethod, isPaid: isBoolean, rankId: isText })
                    this.find(this.ranks, fields.rankId as string, "rank")
                    Object.assign(row, fields)
                },
                filter: (row, query) => matches(query, "gradingId", (row as GradingCandidateRow).gradingId),
                isInUse: () => false,
                toOutput: row => this.gradingCandidateOutput(row as GradingCandidateRow)
            },
            "tournaments": {
                name: "tournament",
                rows: this.tournaments,
                create: body => this.insert(this.tournaments, this.tournamentFields(body)),
                update: (row, body) => Object.assign(row, this.tournamentFields(body)),
                filter: () => true,
                isInUse: row => this.tournamentCompetitors.some(tc => tc.tournamentId === row.id)
                    || this.tournamentMatches.some(tm => tm.tournamentId === row.id),
                toOutput: row => this.tournamentOutput(row as TournamentRow),
                actions: {
                    "possible-competitors": (row, query) => this.possibleCompetitors(row as TournamentRow, query)
                }
            },
            "tournament-competitors": {
                name: "tournament-competitor",
                rows: this.tournamentCompetitors,
                create: body => this.insertTournamentCompetitors([body])[0],
                createInBulk: body => this.insertTournamentCompetitors(requireBulk(body)),
                update: (row, body) => Object.assign(row, validate(body, { paymentMethod: isPaymentMethod, isPaid: isBoolean })),
                filter: (row, query) => matches(query, "tournamentId", (row as TournamentCompetitorRow).tournamentId),
                isInUse: row => this.tournamentMatches.some(tm => tm.competitorAId === row.id || tm.competitorBId === row.id),
                toOutput: row => this.tournamentCompetitorOutput(row as TournamentCompetitorRow)
            },
            "tournament-matches": {
                name: "tournament-match",
                rows: this.tournamentMatches,
                create: body => this.insertTournamentMatches([body])[0],
                createInBulk: body => this.insertTournamentMatches(requireBulk(body)),
                update: (row, body) => Object.assign(row, validate(body, {
                    when: isLocalDateTime,
                    where: isText,
                    result: value => Object.values(TournamentMatchResult).includes(value as TournamentMatchResult)
                })),
                filter: (row, query) => matches(query, "tournamentId", (row as TournamentMatchRow).tournamentId),
                isInUse: () => false,
                toOutput: row => this.tournamentMatchOutput(row as TournamentMatchRow)
            }
        }
    }

    // A student and a teacher are a role of a member: the same creation, filters and output
    private memberRoleHandler(
        name: string,
        rows: Array<StudentRow>,
        isInUse: (row: Row) => boolean
    ): Handler<Row, StudentOutput | TeacherOutput> {
        return {
            name,
            rows,
            create: body => {
                const memberId = requireText(body, "memberId")
                this.find(this.members, memberId, "member")
                if (rows.some(r => r.memberId === memberId)) {
                    throw new Problem(409, `${name}-exists`, `${name} exists`)
                }
                return this.insert(rows, { memberId })
            },
            filter: (row, query) => {
                const member = this.find(this.members, (row as StudentRow).memberId, "member")
                return matches(query, "memberId", member.id) && matchesMember(member, query)
            },
            isInUse,
            toOutput: row => this.memberRoleOutput(row as StudentRow)
        }
    }

    private memberFields(body: Record<string, unknown>, excludingId?: string): Omit<MemberRow, keyof AuditRow> {
        const fields = validate(body, {
            name: isText,
            phone: isText,
            cpf: value => typeof value === "string" && isValidCpf(value),
            address: isText,
            gender: value => value === "MALE" || value === "FEMALE",
            weight: isPositiveNumber,
            height: isPositiveNumber,
            birthDate: isLocalDate,
            isPwd: isBoolean,
            isCompetitor: isBoolean,
            rankId: isText
        }) as Omit<MemberRow, keyof AuditRow>
        this.find(this.ranks, fields.rankId, "rank")
        if (this.members.some(m => m.cpf === fields.cpf && m.id !== excludingId)) {
            throw new Problem(409, "member-exists", "Member exists")
        }
        return fields
    }

    private tuitionFields(body: Record<string, unknown>): Omit<TuitionRow, keyof AuditRow> {
        return validate(body, { name: isText, price: isNonNegativeNumber }) as Omit<TuitionRow, keyof AuditRow>
    }

    private clazzFields(body: Record<string, unknown>): Omit<ClazzRow, keyof AuditRow> {
        const fields = validate(body, {
            name: isText,
            martialArt: isText,
            startTime: isLocalTime,
            endTime: isLocalTime,
            weekdays: value => Array.isArray(value) && value.length > 0
                && value.every(weekday => Object.values(DayOfWeek).includes(weekday)),
            teacherId: isText
        }) as Omit<ClazzRow, keyof AuditRow>
        this.find(this.teachers, fields.teacherId, "teacher")
        // The core-api answers the times with the seconds
        return { ...fields, startTime: withSeconds(fields.startTime), endTime: withSeconds(fields.endTime) }
    }

    private tuitionPaymentFields(
        body: Record<string, unknown>,
        studentEnrollmentId: string,
        excludingId?: string
    ): Omit<TuitionPaymentRow, keyof AuditRow | "studentEnrollmentId"> {
        const fields = validate(body, {
            month: value => Number.isInteger(value) && (value as number) >= 1 && (value as number) <= 12,
            year: isPositiveInteger,
            isPaid: isBoolean
        }) as Omit<TuitionPaymentRow, keyof AuditRow | "studentEnrollmentId">
        const exists = this.tuitionPayments.some(tp => tp.studentEnrollmentId === studentEnrollmentId
            && tp.month === fields.month && tp.year === fields.year && tp.id !== excludingId)
        if (exists) {
            throw new Problem(409, "tuition-payment-exists", "Tuition payment exists")
        }
        return fields
    }

    private gradingFields(body: Record<string, unknown>): Omit<GradingRow, keyof AuditRow> {
        const fields = validate(body, {
            name: isText,
            when: isLocalDateTime,
            where: isText,
            registrationPeriodStart: isLocalDate,
            registrationPeriodEnd: isLocalDate,
            prices: value => typeof value === "object" && value !== null && Object.keys(value).length > 0
                && Object.values(value).every(isNonNegativeNumber)
        })
        if ((fields.registrationPeriodEnd as string) < (fields.registrationPeriodStart as string)) {
            throw new Problem(400, "registration-period-ends-before-it-starts", "Registration period ends before it starts")
        }
        const prices = Object.entries(fields.prices as Record<string, number>).map(([rankId, price]) => {
            this.find(this.ranks, rankId, "rank")
            return { rankId, price }
        })
        return { ...fields, prices } as Omit<GradingRow, keyof AuditRow>
    }

    private tournamentFields(body: Record<string, unknown>): Omit<TournamentRow, keyof AuditRow> {
        const fields = validate(body, {
            name: isText,
            format: value => Object.values(TournamentFormat).includes(value as TournamentFormat),
            divisionCriterion: value => typeof value === "object" && value !== null,
            participationPrice: isNonNegativeNumber,
            startDateTime: isLocalDateTime,
            endDateTime: isLocalDateTime,
            where: isText
        }) as Omit<TournamentRow, keyof AuditRow>
        if (fields.endDateTime < fields.startDateTime) {
            throw new Problem(400, "tournament-ends-before-it-starts", "Tournament ends before it starts")
        }
        const divisionCriterion = fields.divisionCriterion
        if (divisionCriterion.RANK) {
            this.find(this.ranks, divisionCriterion.RANK, "rank")
        }
        return {
            ...fields,
            divisionCriterion: {
                GENDER: divisionCriterion.GENDER ?? null,
                WEIGHT: divisionCriterion.WEIGHT ?? null,
                HEIGHT: divisionCriterion.HEIGHT ?? null,
                AGE: divisionCriterion.AGE ?? null,
                IS_PWD: divisionCriterion.IS_PWD ?? null,
                RANK: divisionCriterion.RANK ?? null
            }
        }
    }

    // All of them or none, as the core-api creates them in one transaction
    private insertTournamentCompetitors(bodies: Array<Record<string, unknown>>): Array<TournamentCompetitorRow> {
        const fields = bodies.map(body => validate(body, {
            paymentMethod: isPaymentMethod,
            isPaid: isBoolean,
            tournamentId: isText,
            memberId: isText
        }) as Omit<TournamentCompetitorRow, keyof AuditRow | "points">)
        const keys = fields.map(f => `${f.tournamentId} ${f.memberId}`)
        if (new Set(keys).size !== keys.length) {
            throw new Problem(400, "bulk-has-duplicated-item", "Bulk has duplicated item")
        }
        for (const competitor of fields) {
            this.find(this.tournaments, competitor.tournamentId, "tournament")
            this.find(this.members, competitor.memberId, "member")
            const exists = this.tournamentCompetitors.some(tc => tc.tournamentId === competitor.tournamentId
                && tc.memberId === competitor.memberId)
            if (exists) {
                throw new Problem(409, "tournament-competitor-exists", "Tournament competitor exists")
            }
        }
        return fields.map(competitor => this.insert(this.tournamentCompetitors, { ...competitor, points: 0 }))
    }

    // All of them or none, as the core-api creates them in one transaction
    private insertTournamentMatches(bodies: Array<Record<string, unknown>>): Array<TournamentMatchRow> {
        const fields = bodies.map(body => validate(body, {
            when: isLocalDateTime,
            where: isText,
            tournamentCompetitorAId: isText,
            tournamentCompetitorBId: isText,
            tournamentId: isText
        }) as unknown as TournamentMatchInput)
        for (const match of fields) {
            if (match.tournamentCompetitorAId === match.tournamentCompetitorBId) {
                throw new Problem(400, "tournament-competitors-need-to-be-different", "Tournament competitors need to be different")
            }
        }
        // A x B and B x A are the same match
        const keys = fields.map(m => [m.tournamentId, ...[m.tournamentCompetitorAId, m.tournamentCompetitorBId].sort()].join(" "))
        if (new Set(keys).size !== keys.length) {
            throw new Problem(400, "bulk-has-duplicated-item", "Bulk has duplicated item")
        }
        for (const match of fields) {
            this.find(this.tournaments, match.tournamentId, "tournament")
            const competitors = [match.tournamentCompetitorAId, match.tournamentCompetitorBId]
                .map(id => this.find(this.tournamentCompetitors, id, "tournament-competitor"))
            if (competitors.some(c => c.tournamentId !== match.tournamentId)) {
                throw new Problem(
                    400,
                    "tournament-competitors-need-to-belong-to-the-tournament",
                    "Tournament competitors need to belong to the tournament"
                )
            }
            const exists = this.tournamentMatches.some(tm => tm.tournamentId === match.tournamentId
                && [tm.competitorAId, tm.competitorBId].sort().join(" ")
                === [match.tournamentCompetitorAId, match.tournamentCompetitorBId].sort().join(" "))
            if (exists) {
                throw new Problem(409, "tournament-match-exists", "Tournament match exists")
            }
        }
        return fields.map(match => this.insert<Omit<TournamentMatchRow, keyof AuditRow>>(this.tournamentMatches, {
            when: match.when,
            where: match.where,
            result: TournamentMatchResult.PENDING,
            competitorAId: match.tournamentCompetitorAId,
            competitorBId: match.tournamentCompetitorBId,
            tournamentId: match.tournamentId
        }))
    }

    /**
     * The competitors of the organization who meet the division criteria. As the core-api, a criterion of weight, height
     * or age only filters when its difference is given, and then by the value plus or minus the difference.
     */
    private possibleCompetitors(tournament: TournamentRow, query: URLSearchParams): Array<MemberOutput> {
        if (tournament.format === TournamentFormat.TITLE_MATCH) {
            throw new Problem(400, "tournament-format-not-supported", "Tournament format is not supported for this operation")
        }

        const criterion = tournament.divisionCriterion
        const isWithin = (value: number, center: number | null, difference: string | null) =>
            center === null || difference === null || Math.abs(value - center) <= Number(difference)
        const age = (birthDate: string) => {
            const today = new Date()
            const birth = new Date(birthDate)
            const hadBirthday = today.getMonth() > birth.getMonth()
                || (today.getMonth() === birth.getMonth() && today.getDate() >= birth.getDate())
            return today.getFullYear() - birth.getFullYear() - (hadBirthday ? 0 : 1)
        }

        return this.members
            .filter(m => m.isCompetitor)
            .filter(m => criterion.GENDER === null || m.gender === criterion.GENDER)
            .filter(m => criterion.IS_PWD === null || m.isPwd === criterion.IS_PWD)
            .filter(m => criterion.RANK === null || m.rankId === criterion.RANK)
            .filter(m => isWithin(m.weight, criterion.WEIGHT, query.get("weightDifference")))
            .filter(m => isWithin(m.height, criterion.HEIGHT, query.get("heightDifference")))
            .filter(m => isWithin(age(m.birthDate), criterion.AGE, query.get("ageDifference")))
            .map(m => this.memberOutput(m))
    }

    private rankCategoryOutput(row: RankCategoryRow): RankCategoryOutput {
        return { ...row }
    }

    private rankOutput(row: RankRow): RankOutput {
        const { categoryId, ...rank } = row
        return { ...rank, category: this.rankCategoryOutput(this.find(this.rankCategories, categoryId, "rank-category")) }
    }

    private memberOutput(row: MemberRow): MemberOutput {
        const { rankId, ...member } = row
        return { ...member, rank: this.rankOutput(this.find(this.ranks, rankId, "rank")) }
    }

    private memberRoleOutput(row: StudentRow): StudentOutput | TeacherOutput {
        const { memberId, ...role } = row
        return { ...role, member: this.memberOutput(this.find(this.members, memberId, "member")) }
    }

    private tuitionOutput(row: TuitionRow): TuitionOutput {
        return { ...row }
    }

    private clazzOutput(row: ClazzRow): ClazzOutput {
        const { teacherId, ...clazz } = row
        return { ...clazz, teacher: this.memberRoleOutput(this.find(this.teachers, teacherId, "teacher")) }
    }

    private studentEnrollmentOutput(row: StudentEnrollmentRow): StudentEnrollmentOutput {
        const { tuitionId, studentId, clazzId, ...studentEnrollment } = row
        return {
            ...studentEnrollment,
            tuition: this.tuitionOutput(this.find(this.tuitions, tuitionId, "tuition")),
            student: this.memberRoleOutput(this.find(this.students, studentId, "student")),
            clazz: this.clazzOutput(this.find(this.clazzes, clazzId, "clazz"))
        }
    }

    private tuitionPaymentOutput(row: TuitionPaymentRow): TuitionPaymentOutput {
        const { studentEnrollmentId, ...tuitionPayment } = row
        return {
            ...tuitionPayment,
            studentEnrollment: this.studentEnrollmentOutput(
                this.find(this.studentEnrollments, studentEnrollmentId, "student-enrollment")
            )
        }
    }

    private gradingOutput(row: GradingRow): GradingOutput {
        const { prices, ...grading } = row
        return {
            ...grading,
            gradingPrices: prices.map(p => ({ price: p.price, rank: this.rankOutput(this.find(this.ranks, p.rankId, "rank")) }))
        }
    }

    private gradingCandidateOutput(row: GradingCandidateRow): GradingCandidateOutput {
        const { rankId, gradingId, memberId, ...gradingCandidate } = row
        return {
            ...gradingCandidate,
            rank: this.rankOutput(this.find(this.ranks, rankId, "rank")),
            grading: this.gradingOutput(this.find(this.gradings, gradingId, "grading")),
            member: this.memberOutput(this.find(this.members, memberId, "member"))
        }
    }

    // The criteria left null are not in the output, as in the core-api
    private tournamentOutput(row: TournamentRow): TournamentOutput {
        const { divisionCriterion, ...tournament } = row
        const { RANK, ...criteria } = divisionCriterion
        const present = Object.fromEntries(Object.entries(criteria).filter(([, value]) => value !== null))
        return {
            ...tournament,
            divisionCriterion: {
                ...present,
                ...(RANK ? { RANK: this.rankOutput(this.find(this.ranks, RANK, "rank")) } : {})
            }
        }
    }

    private tournamentCompetitorOutput(row: TournamentCompetitorRow): TournamentCompetitorOutput {
        const { tournamentId, memberId, ...tournamentCompetitor } = row
        return {
            ...tournamentCompetitor,
            tournament: this.tournamentOutput(this.find(this.tournaments, tournamentId, "tournament")),
            member: this.memberOutput(this.find(this.members, memberId, "member"))
        }
    }

    private tournamentMatchOutput(row: TournamentMatchRow): TournamentMatchOutput {
        const { competitorAId, competitorBId, tournamentId, ...tournamentMatch } = row
        return {
            ...tournamentMatch,
            competitorA: this.tournamentCompetitorOutput(this.find(this.tournamentCompetitors, competitorAId, "tournament-competitor")),
            competitorB: this.tournamentCompetitorOutput(this.find(this.tournamentCompetitors, competitorBId, "tournament-competitor")),
            tournament: this.tournamentOutput(this.find(this.tournaments, tournamentId, "tournament"))
        }
    }

    private insert<T extends object>(rows: Array<T & AuditRow>, fields: T): T & AuditRow {
        const now = this.now()
        const row = { ...fields, id: randomUUID(), createdAt: now, updatedAt: now, organizationId: mockOrganizationId }
        rows.push(row)
        return row
    }

    private find<T extends AuditRow>(rows: Array<T>, id: string, name: string): T {
        const row = rows.find(r => r.id === id)
        if (!row) {
            throw new Problem(404, `${name}-not-found`, "Not found")
        }
        return row
    }

    private now(): string {
        this.clock++
        return new Date(this.clock).toISOString()
    }
}

/**
 * Makes the page talk to the fake core-api: every request to the base URL of the API is answered by it.
 */
export async function installFakeCoreApi(page: Page, fakeCoreApi: FakeCoreApi): Promise<void> {
    await page.route(url => url.href.startsWith(`${mockApiBaseUrl}/`), async route => {
        const request = route.request()
        const url = new URL(request.url())
        const response = fakeCoreApi.handle({
            method: request.method(),
            path: pathOfApi(url.href),
            query: url.searchParams,
            body: request.postData() ? request.postDataJSON() : undefined
        })
        await route.fulfill({
            status: response.status,
            contentType: response.status >= 400 ? "application/problem+json" : "application/json",
            body: JSON.stringify(response.body)
        })
    })
}

// A filter of the listing: absent, it matches every record
function matches(query: URLSearchParams, name: string, value: string): boolean {
    const filter = query.get(name)
    return filter === null || filter === value
}

// The name matches when it contains the text, ignoring the case; the CPF only when equal
function matchesMember(member: MemberRow, query: URLSearchParams): boolean {
    const name = query.get("name")?.trim()
    const cpf = query.get("cpf")
    if (cpf !== null && !isValidCpf(cpf)) {
        throw invalidInput({ cpf: "invalid Brazilian individual taxpayer registry number (CPF)" })
    }
    return (!name || member.name.toLowerCase().includes(name.toLowerCase())) && (cpf === null || member.cpf === cpf)
}

// 18:00 as 18:00:00
function withSeconds(time: string): string {
    return time.length === 5 ? `${time}:00` : time
}

class Problem extends Error {
    constructor(
        readonly status: number,
        readonly type: string,
        readonly title: string,
        readonly errors?: Array<{ field: string, message: string }>
    ) {
        super(title)
    }

    toProblemDetail(): object {
        return {
            type: PROBLEM_TYPE_PREFIX + this.type,
            title: this.title,
            status: this.status,
            detail: this.title,
            ...(this.errors ? { errors: this.errors } : {})
        }
    }
}

function ok(body: unknown): FakeResponse {
    return { status: 200, body }
}

function notMocked(request: FakeRequest): Problem {
    return new Problem(501, "not-mocked", `Not mocked in the e2e tests: ${request.method} ${request.path}`)
}

function invalidInput(errors: Record<string, string>): Problem {
    return new Problem(400, "invalid-input", "Invalid input",
        Object.entries(errors).map(([field, message]) => ({ field, message })))
}

function asBody(body: unknown): Record<string, unknown> {
    return typeof body === "object" && body !== null ? body as Record<string, unknown> : {}
}

// The items of a bulk creation, { bulk: [...] }
function requireBulk(body: Record<string, unknown>): Array<Record<string, unknown>> {
    const bulk = validate(body, {
        bulk: value => Array.isArray(value) && value.length > 0 && value.length <= BULK_MAX_SIZE
    }).bulk as Array<unknown>
    return bulk.map(asBody)
}

// The fields of the body that pass their checks; a 400 with the ones that do not, as the validation of the core-api
function validate(
    body: Record<string, unknown>,
    checks: Record<string, (value: unknown) => boolean>
): Record<string, unknown> {
    const invalid = Object.entries(checks).filter(([field, check]) => !check(body[field]))
    if (invalid.length > 0) {
        throw invalidInput(Object.fromEntries(invalid.map(([field]) => [field, "is invalid"])))
    }
    return Object.fromEntries(Object.keys(checks).map(field => [field, body[field]]))
}

function requireText(body: Record<string, unknown>, field: string): string {
    return validate(body, { [field]: isText })[field] as string
}

const isText = (value: unknown) => typeof value === "string" && value.trim().length > 0 && value.length <= TEXT_MAX_LENGTH
const isBoolean = (value: unknown) => typeof value === "boolean"
const isPositiveNumber = (value: unknown) => typeof value === "number" && value > 0
const isNonNegativeNumber = (value: unknown) => typeof value === "number" && value >= 0
const isPositiveInteger = (value: unknown) => Number.isInteger(value) && (value as number) > 0
const isLocalDate = (value: unknown) => typeof value === "string" && /^\d{4}-\d{2}-\d{2}$/.test(value)
const isLocalTime = (value: unknown) => typeof value === "string" && /^\d{2}:\d{2}(:\d{2})?$/.test(value)
const isLocalDateTime = (value: unknown) => typeof value === "string" && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/.test(value)
const isPaymentMethod = (value: unknown) => Object.values(PaymentMethod).includes(value as PaymentMethod)

interface AuditRow {
    id: string
    createdAt: string
    updatedAt: string
    organizationId: string
}

type Row = AuditRow

interface RankCategoryRow extends AuditRow {
    name: string
}

interface RankRow extends AuditRow {
    name: string
    order: number
    categoryId: string
}

interface MemberRow extends AuditRow {
    name: string
    phone: string
    cpf: string
    address: string
    gender: Gender
    weight: number
    height: number
    birthDate: string
    isPwd: boolean
    isCompetitor: boolean
    rankId: string
}

interface StudentRow extends AuditRow {
    memberId: string
}

type TeacherRow = StudentRow

interface TuitionRow extends AuditRow {
    name: string
    price: number
}

interface ClazzRow extends AuditRow {
    name: string
    martialArt: MartialArt
    startTime: string
    endTime: string
    weekdays: Array<DayOfWeek>
    teacherId: string
}

interface StudentEnrollmentRow extends AuditRow {
    isActive: boolean
    tuitionId: string
    studentId: string
    clazzId: string
}

interface TuitionPaymentRow extends AuditRow {
    // January being 1, as the core-api
    month: number
    year: number
    isPaid: boolean
    studentEnrollmentId: string
}

interface GradingRow extends AuditRow {
    name: string
    when: string
    where: string
    registrationPeriodStart: string
    registrationPeriodEnd: string
    prices: Array<{ rankId: string, price: number }>
}

interface GradingCandidateRow extends AuditRow {
    paymentMethod: PaymentMethod
    isPaid: boolean
    rankId: string
    gradingId: string
    memberId: string
}

interface TournamentRow extends AuditRow {
    name: string
    format: TournamentFormat
    // As the input: null is a criterion that does not divide; RANK is the id of the rank
    divisionCriterion: {
        GENDER: Gender | null
        WEIGHT: number | null
        HEIGHT: number | null
        AGE: number | null
        IS_PWD: boolean | null
        RANK: string | null
    }
    participationPrice: number
    startDateTime: string
    endDateTime: string
    where: string
}

interface TournamentCompetitorRow extends AuditRow {
    paymentMethod: PaymentMethod
    isPaid: boolean
    points: number
    tournamentId: string
    memberId: string
}

interface TournamentMatchRow extends AuditRow {
    when: string
    where: string
    result: TournamentMatchResult
    competitorAId: string
    competitorBId: string
    tournamentId: string
}

interface TournamentMatchInput {
    when: string
    where: string
    tournamentCompetitorAId: string
    tournamentCompetitorBId: string
    tournamentId: string
}

interface Handler<R extends Row, O> {
    // As in the type of the problems, e.g. member in member-not-found
    readonly name: string
    readonly rows: Array<R>
    create(body: Record<string, unknown>): R
    // POST /{resource}/bulk, all of the records or none
    createInBulk?(body: Record<string, unknown>): Array<R>
    update?(row: R, body: Record<string, unknown>): void
    filter(row: R, query: URLSearchParams): boolean
    // Whether other records reference it, so the database would refuse to delete it
    isInUse(row: R): boolean
    toOutput(row: R): O
    // GET /{resource}/{id}/{action}, such as the possible competitors of a tournament
    readonly actions?: Record<string, (row: R, query: URLSearchParams) => unknown>
}
