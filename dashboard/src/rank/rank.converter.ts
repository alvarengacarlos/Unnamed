import { convertDateTimeToView, convertToLabelValues } from "../common/common.converter"
import type { LabelValue } from "../common/common.type"
import type { Rank, RankCategory } from "./rank.type"

export function convertRankCategoryToView(rankCategory: RankCategory): Array<LabelValue> { 
    return convertToLabelValues({
        "Id": rankCategory.id,
        "Nome": rankCategory.name,
        "Criado em": convertDateTimeToView(rankCategory.createdAt),
        "Atualizado em": convertDateTimeToView(rankCategory.updatedAt),
        "Organização": rankCategory.organizationId
    })
}

export function convertRankToView(rank: Rank): Array<LabelValue> {
    return convertToLabelValues({
        "Id": rank.id,
        "Nome": rank.name,
        "Ordem": String(rank.order),
        "Categoria": rank.category.name,
        "Criado em": convertDateTimeToView(rank.createdAt),
        "Atualizado em": convertDateTimeToView(rank.updatedAt),
        "Organização": rank.organizationId
    })
}
