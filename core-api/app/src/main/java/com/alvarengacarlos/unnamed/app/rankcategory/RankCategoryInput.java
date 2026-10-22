package com.alvarengacarlos.unnamed.app.rankcategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.alvarengacarlos.unnamed.app.common.CommonInput;

public record RankCategoryInput() {
    public static record CreateUpdate(@NotBlank @Size(max = CommonInput.TEXT_MAX_LENGTH) String name) {
    }
}
