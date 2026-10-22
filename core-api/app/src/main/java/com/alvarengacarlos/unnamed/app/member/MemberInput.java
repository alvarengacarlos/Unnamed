package com.alvarengacarlos.unnamed.app.member;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.br.CPF;

import com.alvarengacarlos.unnamed.app.common.CommonInput;
import com.alvarengacarlos.unnamed.app.member.Member.Gender;

public record MemberInput() {
    public static record CreateUpdate(
        @NotBlank @Size(max = CommonInput.TEXT_MAX_LENGTH) String name,
        @NotBlank @Length(min = 14, max = 15) String phone,
        @NotBlank @CPF String cpf,
        @NotBlank @Size(max = CommonInput.TEXT_MAX_LENGTH) String address,
        @NotNull Member.Gender gender,
        @NotNull @Positive @Digits(integer = 3, fraction = 2) BigDecimal weight,
        @NotNull @Positive @Digits(integer = 3, fraction = 2) BigDecimal height,
        @NotNull LocalDate birthDate,
        @NotNull Boolean isPwd,
        @NotNull Boolean isCompetitor,
        @NotNull UUID rankId
    ) {}

    public static record GetMembers(
        Optional<String> name,
        Optional<@CPF String> cpf) {
        public GetMembers {
            // A blank name is no filter, as nothing typed in a search
            name = name == null ? Optional.empty() : name.map(String::strip).filter(n -> !n.isEmpty());
            if (cpf == null) {
                cpf = Optional.empty();
            }
        }
    }

    public static record GetPossibleCompetitors(
        Optional<Gender> gender,
        Optional<BigDecimal> minWeight,
        Optional<BigDecimal> maxWeight,
        Optional<BigDecimal> minHeight,
        Optional<BigDecimal> maxHeight,
        Optional<LocalDate> minBirthdate,
        Optional<LocalDate> maxBirthdate,
        Optional<Boolean> isPwd,
        Optional<UUID> rankId
    ) {
        public GetPossibleCompetitors {
            final RuntimeException e = new RuntimeException("Both should be inputted");
            if (isWeightValid(minWeight, maxWeight)) {
                throw e;
            }

            if (isHeightValid(minHeight, maxHeight)) {
                throw e;
            }

            if (isBirthdateValid(minBirthdate, maxBirthdate)) {
                throw e;
            }
        }

        private Boolean isWeightValid(Optional<BigDecimal> minWeight, Optional<BigDecimal> maxWeight) {
            return minWeight.isEmpty() && maxWeight.isPresent() || 
                minWeight.isPresent() && maxWeight.isEmpty();
        }

        private Boolean isHeightValid(Optional<BigDecimal> minHeight, Optional<BigDecimal> maxHeight) {
            return minHeight.isEmpty() && maxHeight.isPresent() || 
                minHeight.isPresent() && maxHeight.isEmpty();
        }

        private Boolean isBirthdateValid(Optional<LocalDate> minBirthdate, Optional<LocalDate> maxBirthdate) {
            return minBirthdate.isEmpty() && maxBirthdate.isPresent() || 
                minBirthdate.isPresent() && maxBirthdate.isEmpty();
        }
    }
}
