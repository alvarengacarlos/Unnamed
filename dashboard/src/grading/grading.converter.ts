import { convertDateTimeToView, convertDateToView, convertLocalDateTimeToView, convertPriceToView, convertToLabelValues } from "../common/common.converter"
import type { LabelValue } from "../common/common.type"
import type { Grading } from "./grading.type"

export function convertGradingToView(grading: Grading): Array<LabelValue> {
    return convertToLabelValues({
        "Id": grading.id,
        "Nome": grading.name,
        "Data": convertLocalDateTimeToView(grading.when),
        "Local": grading.where,
        "Inscrições": convertRegistrationPeriodToView(grading),
        "Preços": grading.gradingPrices
            .map(gp => `${gp.rank.name} - ${gp.rank.category.name}: ${convertPriceToView(gp.price)}`)
            .join("; "),
        "Criado em": convertDateTimeToView(grading.createdAt),
        "Atualizado em": convertDateTimeToView(grading.updatedAt),
        "Organização": grading.organizationId
    })
}

// As 01/10/2026 a 15/10/2026
export function convertRegistrationPeriodToView(grading: Grading): string {
    return `${convertDateToView(grading.registrationPeriodStart)} a ${convertDateToView(grading.registrationPeriodEnd)}`
}
