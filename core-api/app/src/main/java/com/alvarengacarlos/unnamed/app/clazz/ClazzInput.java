package com.alvarengacarlos.unnamed.app.clazz;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;

import com.alvarengacarlos.unnamed.app.common.CommonInput;

public record ClazzInput() {
    public static record CreateUpdate(
        @NotBlank @Size(max = CommonInput.TEXT_MAX_LENGTH) String name,
        @NotNull Clazz.MartialArt martialArt,
        @NotNull LocalTime startTime, 
        @NotNull LocalTime endTime, 
        @UniqueElements @NotEmpty Set<@NotNull DayOfWeek> weekdays, 
        @NotNull UUID teacherId) {
    }

    public static record GetClazzes(
        Optional<Clazz.MartialArt> martialArt) {
        public GetClazzes {
            if (martialArt == null) {
                martialArt = Optional.empty();
            }
        }
    }
}
