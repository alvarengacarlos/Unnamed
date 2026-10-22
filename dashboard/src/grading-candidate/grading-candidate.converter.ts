import { convertBooleanToView, convertDateTimeToView, convertPaymentMethodToView, convertToLabelValues } from "../common/common.converter"
import type { LabelValue } from "../common/common.type"
import type { GradingCandidate } from "./grading-candidate.type"

export function convertGradingCandidateToView(gradingCandidate: GradingCandidate): Array<LabelValue> {
    return convertToLabelValues({
        "Id": gradingCandidate.id,
        "Membro": gradingCandidate.member.name,
        "CPF": gradingCandidate.member.cpf,
        "Graduação": gradingCandidate.grading.name,
        "Grau atual": `${gradingCandidate.member.rank.name} - ${gradingCandidate.member.rank.category.name}`,
        "Grau pretendido": `${gradingCandidate.rank.name} - ${gradingCandidate.rank.category.name}`,
        "Forma de pagamento": convertPaymentMethodToView(gradingCandidate.paymentMethod),
        "Pago": convertBooleanToView(gradingCandidate.isPaid),
        "Criado em": convertDateTimeToView(gradingCandidate.createdAt),
        "Atualizado em": convertDateTimeToView(gradingCandidate.updatedAt),
        "Organização": gradingCandidate.organizationId
    })
}
