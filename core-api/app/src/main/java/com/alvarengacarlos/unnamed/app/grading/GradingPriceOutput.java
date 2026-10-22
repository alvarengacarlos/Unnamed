package com.alvarengacarlos.unnamed.app.grading;

import java.math.BigDecimal;

import com.alvarengacarlos.unnamed.app.rank.RankOutput;

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
