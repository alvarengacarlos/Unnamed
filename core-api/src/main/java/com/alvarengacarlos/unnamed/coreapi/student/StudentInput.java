package com.alvarengacarlos.unnamed.coreapi.student;

import java.util.Optional;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

public record StudentInput() {
    public static record Create(
        @NotNull UUID memberId) {
    }

    public static record GetStudents(
        Optional<UUID> memberId,
        Optional<String> name,
        Optional<@CPF String> cpf) {
        public GetStudents {
            if (memberId == null) {
                memberId = Optional.empty();
            }
            // A blank name is no filter, as nothing typed in a search
            name = name == null ? Optional.empty() : name.map(String::strip).filter(n -> !n.isEmpty());
            if (cpf == null) {
                cpf = Optional.empty();
            }
        }
    }
}
