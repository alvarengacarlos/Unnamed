package com.alvarengacarlos.unnamed.coreapi.grading;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;

public record GradingInput() {
    public static record CreateUpdate(
        @NotBlank @Size(max = CommonInput.TEXT_MAX_LENGTH) String name,
        @NotNull LocalDateTime when,
        @NotBlank @Size(max = CommonInput.TEXT_MAX_LENGTH) String where,
        @NotNull LocalDate registrationPeriodStart,
        @NotNull LocalDate registrationPeriodEnd,
        @NotEmpty Map<@NotNull UUID, @NotNull @PositiveOrZero BigDecimal> prices
    ) {}
}
