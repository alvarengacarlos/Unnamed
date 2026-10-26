package com.alvarengacarlos.unnamed.coreapi.tuition;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;

public record TuitionInput() {
    public static record CreateUpdate(
        @NotBlank @Size(max = CommonInput.TEXT_MAX_LENGTH) String name, 
        @NotNull @Min(0) BigDecimal price) {
    }
}
