package com.alvarengacarlos.unnamed.coreapi.rank;

import java.util.Optional;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;

public record RankInput() {
    public static record Create(
        @NotBlank @Size(max = CommonInput.TEXT_MAX_LENGTH) String name, 
        @NotNull @Positive Byte order, 
        @NotNull UUID categoryId) {
    }

    public static record Update(
        @NotBlank @Size(max = CommonInput.TEXT_MAX_LENGTH) String name) {
    }

    public static record GetRanks(
        Optional<UUID> categoryId) {
        public GetRanks {
            if (categoryId == null) {
                categoryId = Optional.empty();
            }
        }
    }
}
