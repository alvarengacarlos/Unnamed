package com.alvarengacarlos.unnamed.app.tournament;

import java.math.BigDecimal;
import java.util.Optional;

import com.alvarengacarlos.unnamed.app.member.Member;
import com.alvarengacarlos.unnamed.app.rank.Rank;

public class TournamentDivisionCriterion {
    private Optional<Member.Gender> gender;
    private Optional<BigDecimal> weight;
    private Optional<BigDecimal> height;
    private Optional<Byte> age;
    private Optional<Boolean> isPwd;
    private Optional<Rank> rank;

    private TournamentDivisionCriterion(Builder builder) {
        gender = builder.gender;
        weight = builder.weight;
        height = builder.height;
        age = builder.age;
        isPwd = builder.isPwd;
        rank = builder.rank;
    }

    public Optional<Member.Gender> getGender() {
        return gender;
    }

    public Optional<BigDecimal> getWeight() {
        return weight;
    }

    public Optional<BigDecimal> getHeight() {
        return height;
    }

    public Optional<Byte> getAge() {
        return age;
    }

    public Optional<Boolean> getIsPwd() {
        return isPwd;
    }

    public Optional<Rank> getRank() {
        return rank;
    }

    public static enum DivisionCriterion {
        GENDER,
        WEIGHT,
        HEIGHT,
        AGE,
        IS_PWD,
        RANK
    }

    public static class Builder {
        private Optional<Member.Gender> gender;
        private Optional<BigDecimal> weight;
        private Optional<BigDecimal> height;
        private Optional<Byte> age;
        private Optional<Boolean> isPwd;
        private Optional<Rank> rank;
        
        public Builder() {
            gender = Optional.empty();
            weight = Optional.empty();
            height = Optional.empty();
            age = Optional.empty();
            isPwd = Optional.empty();
            rank = Optional.empty();
        }

        public Builder gender(Optional<Member.Gender> gender) {
            this.gender = gender;
            return this;
        }
        
        public Builder weight(Optional<BigDecimal> weight) {
            this.weight = weight;
            return this;
        }
        
        public Builder height(Optional<BigDecimal> height) {
            this.height = height;
            return this;
        }

        public Builder age(Optional<Byte> age) {
            this.age = age;
            return this;
        }
        
        public Builder isPwd(Optional<Boolean> isPwd) {
            this.isPwd = isPwd;
            return this;
        }

        public Builder rank(Optional<Rank> rank) {
            this.rank = rank;
            return this;
        }

        public TournamentDivisionCriterion build() {
            return new TournamentDivisionCriterion(this);
        }
    }
}
