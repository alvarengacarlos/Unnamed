import { test, expect } from "../fixtures"
import { isApiMocked } from "../support/config"
import type { RefusedRequest } from "../support/api-client"
import { PROBLEM_TYPE_PREFIX, problemTypeMessages } from "../../src/common/common.converter"
import { TournamentFormat } from "../../src/tournament/tournament.type"
import { PaymentMethod } from "../../src/common/common.type"

// Answered only when two requests pass the check of a service together, which a test cannot cause on purpose. The
// GlobalExceptionHandlerTest of the core-api covers it.
const TYPES_WITHOUT_SCENARIO = ["resource-exists"]

/**
 * The dashboard has a message for some problem types of the core-api, named by hand in problemTypeMessages. Each one
 * is caused here against the real core-api, so a type renamed or removed there fails the test.
 */
test.describe("Tipos de problema do core-api", () => {
    test.skip(isApiMocked, "The types are the ones of the real core-api; the fake one only imitates what it knows")

    test("as mensagens do dashboard são de tipos que o core-api responde", async ({ api, data }) => {
        const match = (tournamentId: string, competitorAId: string, competitorBId: string) => ({
            when: "2030-02-01T10:00:00",
            where: "Tatame 1",
            tournamentCompetitorAId: competitorAId,
            tournamentCompetitorBId: competitorBId,
            tournamentId
        })

        // How to cause each type, by the name after the prefix
        const scenarios: Record<string, () => Promise<RefusedRequest>> = {
            "resource-in-use": async () => {
                const member = await data.member()
                return api.refused("DELETE", `/ranks/${member.rank.id}`)
            },
            "member-exists": async () => {
                const member = await data.member()
                return api.refused("POST", "/members", {
                    name: member.name, phone: member.phone, cpf: member.cpf, address: member.address,
                    gender: member.gender, weight: member.weight, height: member.height, birthDate: member.birthDate,
                    isPwd: member.isPwd, isCompetitor: member.isCompetitor, rankId: member.rank.id
                })
            },
            "student-exists": async () => {
                const student = await data.student()
                return api.refused("POST", "/students", { memberId: student.member.id })
            },
            "teacher-exists": async () => {
                const teacher = await data.teacher()
                return api.refused("POST", "/teachers", { memberId: teacher.member.id })
            },
            "rank-exists": async () => {
                const rank = await data.rank()
                return api.refused("POST", "/ranks", { name: "Outro", order: rank.order, categoryId: rank.category.id })
            },
            "student-enrollment-exists": async () => {
                const studentEnrollment = await data.studentEnrollment()
                return api.refused("POST", "/student-enrollments", {
                    studentId: studentEnrollment.student.id,
                    clazzId: studentEnrollment.clazz.id,
                    tuitionId: studentEnrollment.tuition.id
                })
            },
            "tuition-payment-exists": async () => {
                const studentEnrollment = await data.studentEnrollment()
                const payment = { month: 1, year: 2030, isPaid: false, studentEnrollmentId: studentEnrollment.id }
                await api.post("/tuition-payments", payment)
                return api.refused("POST", "/tuition-payments", payment)
            },
            "grading-candidate-exists": async () => {
                const grading = await data.grading()
                const member = await data.member()
                const candidate = {
                    paymentMethod: PaymentMethod.PIX, isPaid: true, memberId: member.id,
                    rankId: member.rank.id, gradingId: grading.id
                }
                await api.post("/grading-candidates", candidate)
                return api.refused("POST", "/grading-candidates", candidate)
            },
            "tournament-competitor-exists": async () => {
                const competitor = await data.tournamentCompetitor()
                return api.refused("POST", "/tournament-competitors", {
                    paymentMethod: PaymentMethod.PIX, isPaid: true,
                    tournamentId: competitor.tournament.id, memberId: competitor.member.id
                })
            },
            "tournament-match-exists": async () => {
                const tournament = await data.tournament()
                const a = await data.tournamentCompetitor({ tournament })
                const b = await data.tournamentCompetitor({ tournament })
                await api.post("/tournament-matches", match(tournament.id, a.id, b.id))
                // B×A is the same match as A×B
                return api.refused("POST", "/tournament-matches", match(tournament.id, b.id, a.id))
            },
            "bulk-has-duplicated-item": async () => {
                const tournament = await data.tournament()
                const a = await data.tournamentCompetitor({ tournament })
                const b = await data.tournamentCompetitor({ tournament })
                return api.refused("POST", "/tournament-matches/bulk", {
                    bulk: [match(tournament.id, a.id, b.id), match(tournament.id, b.id, a.id)]
                })
            },
            "tournament-format-not-supported": async () => {
                const tournament = await data.tournament({ format: TournamentFormat.TITLE_MATCH })
                return api.refused("GET", `/tournaments/${tournament.id}/possible-competitors`)
            },
            "tournament-competitors-need-to-be-different": async () => {
                const competitor = await data.tournamentCompetitor()
                return api.refused("POST", "/tournament-matches", match(competitor.tournament.id, competitor.id, competitor.id))
            },
            "tournament-competitors-need-to-belong-to-the-tournament": async () => {
                const competitor = await data.tournamentCompetitor()
                const competitorOfAnotherTournament = await data.tournamentCompetitor()
                return api.refused(
                    "POST",
                    "/tournament-matches",
                    match(competitor.tournament.id, competitor.id, competitorOfAnotherTournament.id)
                )
            },
            "registration-period-ends-before-it-starts": async () => {
                const rank = await data.rank()
                return api.refused("POST", "/gradings", {
                    name: "Graduação", when: "2030-01-10T10:00:00", where: "Ginásio",
                    registrationPeriodStart: "2029-12-20", registrationPeriodEnd: "2029-12-19",
                    prices: { [rank.id]: 100 }
                })
            },
            "tournament-ends-before-it-starts": async () => {
                return api.refused("POST", "/tournaments", {
                    name: "Torneio", format: TournamentFormat.SINGLE_ELIMINATION,
                    divisionCriterion: { GENDER: null, WEIGHT: null, HEIGHT: null, AGE: null, IS_PWD: null, RANK: null },
                    participationPrice: 50, startDateTime: "2030-02-01T09:00:00", endDateTime: "2030-02-01T08:59:00",
                    where: "Ginásio"
                })
            }
        }

        // A message added to the dashboard needs its scenario here, and a scenario needs its message
        expect(Object.keys(scenarios).sort())
            .toEqual(Object.keys(problemTypeMessages).filter(type => !TYPES_WITHOUT_SCENARIO.includes(type)).sort())

        for (const [type, cause] of Object.entries(scenarios)) {
            const refused = await cause()
            expect.soft(refused.problemDetail.type, `${type} (${refused.status})`).toBe(PROBLEM_TYPE_PREFIX + type)
        }
    })
})
