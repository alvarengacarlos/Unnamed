package com.alvarengacarlos.unnamed.app.member;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.alvarengacarlos.unnamed.app.member.Member.Gender;
import com.alvarengacarlos.unnamed.app.rank.RankOutput;

public record MemberOutput(
    UUID id,
    String name,
    String phone,
    String cpf,
    String address,
    Gender gender,
    BigDecimal weight,
    BigDecimal height,
    LocalDate birthDate,
    Boolean isPwd,
    Boolean isCompetitor,
    Instant createdAt,
    Instant updatedAt,
    RankOutput rank,
    UUID organizationId) {
    public static MemberOutput from(Member member) {
        return new MemberOutput(
            member.getId(),
            member.getName(),
            member.getPhone(),
            member.getCpf(),
            member.getAddress(),
            member.getGender(),
            member.getWeight(),
            member.getHeight(),
            member.getBirthDate(),
            member.getIsPwd(),
            member.getIsCompetitor(),
            member.getCreatedAt(),
            member.getUpdatedAt(),
            RankOutput.from(member.getRank()),
            member.getOrganizationId()
        );
    }
}
