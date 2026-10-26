package com.alvarengacarlos.unnamed.coreapi.rankcategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;

public record RankCategoryInput() {
    public static record CreateUpdate(@NotBlank @Size(max = CommonInput.TEXT_MAX_LENGTH) String name) {
    }
}
