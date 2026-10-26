package com.alvarengacarlos.unnamed.coreapi.grading;

import java.math.BigDecimal;

import com.alvarengacarlos.unnamed.coreapi.rank.RankOutput;

public record GradingPriceOutput(
    BigDecimal price,
    RankOutput rank
) {
    public static GradingPriceOutput from(GradingPrice gradingPrice) {
        return new GradingPriceOutput(
            gradingPrice.getPrice(),
            RankOutput.from(gradingPrice.getRank())
        );
    }
}
