import { HttpError } from "./common.service"
import { DayOfWeek, Month, PaymentMethod, type LabelValue } from "./common.type"

// The label of each property, in the order the properties are shown
export function convertToLabelValues(properties: Record<string, string>): Array<LabelValue> {
    return Object.entries(properties).map(([label, value]) => ({ label, value }))
}

// A Record of every value, so the type checker asks for the label of a new one
const dayOfWeekLabels: Record<DayOfWeek, string> = {
    [DayOfWeek.MONDAY]: "Segunda-feira",
    [DayOfWeek.TUESDAY]: "Terça-feira",
    [DayOfWeek.WEDNESDAY]: "Quarta-feira",
    [DayOfWeek.THURSDAY]: "Quinta-feira",
    [DayOfWeek.FRIDAY]: "Sexta-feira",
    [DayOfWeek.SATURDAY]: "Sábado",
    [DayOfWeek.SUNDAY]: "Domingo"
}

export function convertDayOfWeekToView(weekday: DayOfWeek): string {
    return dayOfWeekLabels[weekday]
}

const dateTimeFormat = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "medium" })
const priceFormat = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" })

// In the time zone of the browser, as 21/10/2026, 10:48:51
export function convertDateTimeToView(dateTime: Date): string {
    return dateTimeFormat.format(dateTime)
}

// As R$ 100,00
export function convertPriceToView(price: number): string {
    return priceFormat.format(price)
}

const localDateTimeFormat = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" })

// A date and time without time zone, as the API answers it (2026-10-21T10:00:00), as 21/10/2026, 10:00
export function convertLocalDateTimeToView(localDateTime: string): string {
    return localDateTimeFormat.format(new Date(localDateTime))
}

// The DateTimePicker takes and gives the date and time as 2026-10-21 10:00:00, and the API as 2026-10-21T10:00:00
export function convertLocalDateTimeToForm(localDateTime: string): string {
    return localDateTime.replace("T", " ")
}

export function convertLocalDateTimeToService(localDateTime: string): string {
    return localDateTime.replace(" ", "T")
}

// yyyy-mm-dd, as the API answers, to dd/mm/yyyy
export function convertDateToView(date: string): string {
    const [year, month, day] = date.split("-")
    return `${day}/${month}/${year}`
}

const paymentMethodLabels: Record<PaymentMethod, string> = {
    [PaymentMethod.CREDIT_CARD]: "Cartão de crédito",
    [PaymentMethod.DEBIT_CARD]: "Cartão de débito",
    [PaymentMethod.PIX]: "Pix",
    [PaymentMethod.MONEY]: "Dinheiro"
}

export function convertPaymentMethodToView(paymentMethod: PaymentMethod): string {
    return paymentMethodLabels[paymentMethod]
}

// The months in their order: the API (Jackson 3) reads and writes a month as its number, January being 1
const months = Object.values(Month)

export function convertMonthToService(month: Month): number {
    return months.indexOf(month) + 1
}

export function convertServiceToMonth(month: number): Month {
    const converted = months[month - 1]
    if (!converted) {
        throw new Error(`Month ${month} does not exist`)
    }

    return converted
}

const monthLabels: Record<Month, string> = {
    [Month.JANUARY]: "Janeiro",
    [Month.FEBRUARY]: "Fevereiro",
    [Month.MARCH]: "Março",
    [Month.APRIL]: "Abril",
    [Month.MAY]: "Maio",
    [Month.JUNE]: "Junho",
    [Month.JULY]: "Julho",
    [Month.AUGUST]: "Agosto",
    [Month.SEPTEMBER]: "Setembro",
    [Month.OCTOBER]: "Outubro",
    [Month.NOVEMBER]: "Novembro",
    [Month.DECEMBER]: "Dezembro"
}

export function convertMonthToView(month: Month): string {
    return monthLabels[month]
}

export const paymentMethodOptions = Object.values(PaymentMethod).map(paymentMethod => ({
    value: paymentMethod,
    label: convertPaymentMethodToView(paymentMethod)
}))

export const booleanOptions = [true, false].map(value => ({
    value,
    label: convertBooleanToView(value)
}))

export function convertBooleanToView(value: boolean): string {
    return value ? "Sim" : "Não"
}

export const PROBLEM_TYPE_PREFIX = "urn:unnamed:problem:"

// Messages of the problem types whose title alone does not tell the user what to do
export const problemTypeMessages: Record<string, string> = {
    "resource-in-use": "Não é possível deletar: existem outros registros ligados a este.",
    "resource-exists": "Já existe um registro com estes dados.",
    "member-exists": "Já existe um membro com este CPF.",
    "student-exists": "Este membro já é aluno.",
    "teacher-exists": "Este membro já é professor.",
    "rank-exists": "Já existe um grau com esta ordem nesta categoria.",
    "student-enrollment-exists": "Este aluno já está matriculado nesta turma.",
    "tuition-payment-exists": "Já existe um pagamento desta matrícula neste mês.",
    "grading-candidate-exists": "Este membro já está inscrito nesta graduação.",
    "tournament-competitor-exists": "Este membro já está inscrito neste torneio.",
    "tournament-match-exists": "Esta luta já existe.",
    "bulk-has-duplicated-item": "Há itens repetidos na lista.",
    "tournament-format-not-supported": "O formato deste torneio não permite buscar possíveis competidores.",
    "tournament-competitors-need-to-be-different": "Os competidores da luta precisam ser diferentes.",
    "tournament-competitors-need-to-belong-to-the-tournament": "Os dois competidores precisam estar inscritos no torneio da luta.",
    "registration-period-ends-before-it-starts": "O fim das inscrições não pode ser antes do início.",
    "tournament-ends-before-it-starts": "O fim do torneio não pode ser antes do início."
}

export function convertErrorToView(error: unknown): string {
    if (!(error instanceof HttpError)) {
        // fetch rejects only when the request does not reach the server
        return "Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente."
    }

    const type = error.problemDetail.type?.replace(PROBLEM_TYPE_PREFIX, "") ?? ""
    if (type in problemTypeMessages) {
        return problemTypeMessages[type]
    }

    if (type === "invalid-input") {
        const fields = (error.problemDetail.errors ?? []).map(e => e.field).join(", ")
        return fields ? `Dados inválidos nos campos: ${fields}.` : "Dados inválidos."
    }

    if (type.endsWith("-exists")) {
        return "Este registro já existe."
    }

    if (type.endsWith("-not-found")) {
        return "Registro não encontrado. Ele pode ter sido deletado; recarregue a página."
    }

    if (error.status === 401) {
        return "Sua sessão expirou. Recarregue a página para entrar novamente."
    }

    if (error.status >= 500) {
        return "Erro no servidor. Tente novamente mais tarde."
    }

    return "Não foi possível concluir a operação."
}

/**
 * The CPF typed in a search, with or without the dots and the dash, as 000.000.000-00. Null while it does not have
 * the 11 digits, such as while it is typed.
 */
export function convertSearchToCpf(search: string): string | null {
    const digits = search.replace(/\D/g, "")
    if (digits.length !== 11 || search.replace(/[\d.\-\s]/g, "").length > 0) {
        return null
    }

    return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6, 9)}-${digits.slice(9)}`
}

/**
 * The filter of a search of members by name or CPF: a complete CPF searches by the CPF, any other text by the name.
 * Empty while nothing is typed, so all the members are listed.
 */
export function convertSearchToMemberFilter(search: string): { name?: string, cpf?: string } {
    const cpf = convertSearchToCpf(search)
    if (cpf) {
        return { cpf }
    }

    const name = search.trim()
    return name ? { name } : {}
}
