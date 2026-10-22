package com.alvarengacarlos.unnamed.app.grading;

import java.math.BigDecimal;

import com.alvarengacarlos.unnamed.app.rank.Rank;

public class GradingPrice {
    private BigDecimal price;
    private Rank rank;

    private GradingPrice(Builder builder) {
        price = builder.price;
        rank = builder.rank;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Rank getRank() {
        return rank;
    }

    public static class Builder {
        private BigDecimal price;
        private Rank rank;

        public Builder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder rank(Rank rank) {
            this.rank = rank;
            return this;
        }
        
        public GradingPrice build() {
            return new GradingPrice(this);
        }
    }
}
