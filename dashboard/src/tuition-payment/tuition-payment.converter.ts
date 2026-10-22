import { convertBooleanToView, convertDateTimeToView, convertMonthToView, convertToLabelValues } from "../common/common.converter"
import type { LabelValue } from "../common/common.type"
import type { TuitionPayment } from "./tuition-payment.type"

export function convertTuitionPaymentToView(tuitionPayment: TuitionPayment): Array<LabelValue> {
    return convertToLabelValues({
        "Id": tuitionPayment.id,
        "Aluno": tuitionPayment.studentEnrollment.student.member.name,
        "Turma": tuitionPayment.studentEnrollment.clazz.name,
        "Mensalidade": tuitionPayment.studentEnrollment.tuition.name,
        "Referente a": convertReferenceToView(tuitionPayment),
        "Pago": convertBooleanToView(tuitionPayment.isPaid),
        "Criado em": convertDateTimeToView(tuitionPayment.createdAt),
        "Atualizado em": convertDateTimeToView(tuitionPayment.updatedAt),
        "Organização": tuitionPayment.organizationId
    })
}

// The month the payment is for, as Outubro/2026
export function convertReferenceToView(tuitionPayment: TuitionPayment): string {
    return `${convertMonthToView(tuitionPayment.month)}/${tuitionPayment.year}`
}
