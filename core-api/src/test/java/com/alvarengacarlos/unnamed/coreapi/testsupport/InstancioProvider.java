package com.alvarengacarlos.unnamed.coreapi.testsupport;

import java.math.BigDecimal;

import org.instancio.spi.InstancioServiceProvider;

public class InstancioProvider implements InstancioServiceProvider {
    // Keeps generated BigDecimals within NUMERIC(5,2) columns (e.g. members.weight/height)
    private static final BigDecimal MIN_BIG_DECIMAL = new BigDecimal("0.01");
    private static final BigDecimal MAX_BIG_DECIMAL = new BigDecimal("999.99");
    private static final int BIG_DECIMAL_SCALE = 2;

    @Override
    public GeneratorProvider getGeneratorProvider() {
        return (node, generators) -> {
            if (node.getTargetClass() == BigDecimal.class) {
                return generators.math()
                    .bigDecimal()
                    .range(MIN_BIG_DECIMAL, MAX_BIG_DECIMAL)
                    .scale(BIG_DECIMAL_SCALE);
            }

            return null;
        };
    }
}
