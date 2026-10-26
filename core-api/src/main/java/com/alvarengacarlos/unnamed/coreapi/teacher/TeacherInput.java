package com.alvarengacarlos.unnamed.coreapi.teacher;

import java.util.Optional;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

public record TeacherInput() {
    public static record Create(
        @NotNull UUID memberId) {
    }

    public static record GetTeachers(
        Optional<String> name,
        Optional<@CPF String> cpf) {
        public GetTeachers {
            // A blank name is no filter, as nothing typed in a search
            name = name == null ? Optional.empty() : name.map(String::strip).filter(n -> !n.isEmpty());
            if (cpf == null) {
                cpf = Optional.empty();
            }
        }
    }
}
