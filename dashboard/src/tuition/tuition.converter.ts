import { convertDateTimeToView, convertPriceToView, convertToLabelValues } from "../common/common.converter"
import type { LabelValue } from "../common/common.type"
import type { Tuition } from "./tuition.type"

export function convertTuitionToView(tuition: Tuition): Array<LabelValue> {
    return convertToLabelValues({
        "Id": tuition.id,
        "Nome": tuition.name,
        "Preço": convertPriceToView(tuition.price),
        "Criado em": convertDateTimeToView(tuition.createdAt),
        "Atualizado em": convertDateTimeToView(tuition.updatedAt),
        "Organização": tuition.organizationId
    })
}
