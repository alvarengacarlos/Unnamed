package com.alvarengacarlos.unnamed.coreapi.tournament;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.member.Member;

public record TournamentInput() {
    public static record CreateUpdate(
        @NotBlank @Size(max = CommonInput.TEXT_MAX_LENGTH) String name,
        @NotNull Tournament.TournamentFormat format,
        @NotNull @Valid DivisionCriterion divisionCriterion,
        @NotNull @PositiveOrZero BigDecimal participationPrice,
        @NotNull LocalDateTime startDateTime,
        @NotNull LocalDateTime endDateTime,
        @NotBlank @Size(max = CommonInput.TEXT_MAX_LENGTH) String where
    ) {}

    // The JSON keys keep the names of TournamentDivisionCriterion.DivisionCriterion.
    public static record DivisionCriterion(
        @JsonProperty("GENDER") Member.Gender gender,
        @JsonProperty("WEIGHT") @Positive BigDecimal weight,
        @JsonProperty("HEIGHT") @Positive BigDecimal height,
        @JsonProperty("AGE") @Positive Byte age,
        @JsonProperty("IS_PWD") Boolean isPwd,
        @JsonProperty("RANK") UUID rankId
    ) {}

    public static record GetPossibleCompetitors(
        BigDecimal weightDifference,
        BigDecimal heightDifference,
        Byte ageDifference
    ) {}
}
