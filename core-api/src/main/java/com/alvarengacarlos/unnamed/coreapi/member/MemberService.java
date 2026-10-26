package com.alvarengacarlos.unnamed.coreapi.member;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvarengacarlos.unnamed.coreapi.OrganizationContext;
import com.alvarengacarlos.unnamed.coreapi.common.AuditLog;
import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.Page;
import com.alvarengacarlos.unnamed.coreapi.common.Pageable;
import com.alvarengacarlos.unnamed.coreapi.rank.Rank;
import com.alvarengacarlos.unnamed.coreapi.rank.RankService;

@Service
public class MemberService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final MemberRepository memberRepository;
    private final RankService rankService;

    public MemberService(
        MemberRepository memberRepository, 
        RankService rankService) {
        this.memberRepository = memberRepository;
        this.rankService = rankService;
    }

    @Transactional
    public Member createMember(MemberInput.CreateUpdate input) {
        final Rank rank = rankService.getRank(input.rankId());
        checkIfMemberExistsForCreate(input.cpf());

        final Member member = Member.builder()
            .name(input.name())
            .phone(input.phone())
            .cpf(input.cpf())
            .address(input.address())
            .gender(input.gender())
            .weight(input.weight())
            .height(input.height())
            .birthDate(input.birthDate())
            .isPwd(input.isPwd())
            .isCompetitor(input.isCompetitor())
            .rank(rank)
            .organizationId(OrganizationContext.require())
            .build();

        AuditLog.log("creating member {}", member.getId());
        memberRepository.save(member);
        return member;
    }

    private void checkIfMemberExistsForCreate(String cpf) {
        if (memberRepository.existsByCpf(cpf, Optional.empty())) {
            throw new MemberExistsException();
        }
    }

    @Transactional
    public Member updateMember(UUID id, MemberInput.CreateUpdate input) {
        AuditLog.log("updating member {}", id);
        
        final Member member = getMember(id);
        final Rank rank = rankService.getRank(input.rankId());
        checkIfMemberExistsForUpdate(id, input.cpf());

        final Member updatedMember = member.toBuilder()
            .name(input.name())
            .phone(input.phone())
            .cpf(input.cpf())
            .address(input.address())
            .gender(input.gender())
            .weight(input.weight())
            .height(input.height())
            .birthDate(input.birthDate())
            .isPwd(input.isPwd())
            .isCompetitor(input.isCompetitor())
            .rank(rank)
            .build();

        memberRepository.update(updatedMember);
        return updatedMember;
    }

    public Map<UUID, Member> getMembers(Set<UUID> ids) {
        logger.debug("Getting members {}", ids);

        final Map<UUID, Member> members = memberRepository.read(ids)
            .stream()
            .collect(Collectors.toMap(Member::getId, Function.identity()));
        if (members.size() != ids.size()) {
            throw new MemberNotFoundException();
        }
        return members;
    }

    public Member getMember(UUID id) {
        logger.debug("Getting member {}", id);

        return memberRepository.read(id)
            .orElseThrow(() -> new MemberNotFoundException());
    }

    private void checkIfMemberExistsForUpdate(UUID id, String cpf) {
        if (memberRepository.existsByCpf(cpf, Optional.of(id))) {
            throw new MemberExistsException();
        }
    }

    @Transactional
    public Member deleteMember(UUID id) {
        AuditLog.log("deleting member {}", id);

        final Member member = getMember(id);
        memberRepository.delete(id);
        return member;
    }

    public Page<Member> getMembers(
        CommonInput.PaginationInput paginationInput, 
        MemberInput.GetMembers filterInput) {
        return memberRepository.read(Pageable.from(paginationInput), filterInput);
    }

    public List<Member> getPossibleCompetitors(MemberInput.GetPossibleCompetitors input) {
        return memberRepository.read(input);
    }
}
