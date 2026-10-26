package com.alvarengacarlos.unnamed.coreapi.gradingcandidate;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvarengacarlos.unnamed.coreapi.OrganizationContext;
import com.alvarengacarlos.unnamed.coreapi.common.AuditLog;
import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.Page;
import com.alvarengacarlos.unnamed.coreapi.common.Pageable;
import com.alvarengacarlos.unnamed.coreapi.grading.Grading;
import com.alvarengacarlos.unnamed.coreapi.grading.GradingService;
import com.alvarengacarlos.unnamed.coreapi.member.Member;
import com.alvarengacarlos.unnamed.coreapi.member.MemberService;
import com.alvarengacarlos.unnamed.coreapi.rank.Rank;
import com.alvarengacarlos.unnamed.coreapi.rank.RankService;

@Service
public class GradingCandidateService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final GradingCandidateRepository gradingCandidateRepository;
    private final RankService rankService;
    private final GradingService gradingService;
    private final MemberService memberService;

    //CHECKSTYLE:OFF
    public GradingCandidateService(
            GradingCandidateRepository candidateRepository,
            RankService rankService,
            GradingService gradingService,
            MemberService memberService) {
        this.gradingCandidateRepository = candidateRepository;
        this.rankService = rankService;
        this.gradingService = gradingService;
        this.memberService = memberService;
    }
    //CHECKSTYLE:ON

    @Transactional
    public GradingCandidate createGradingCandidate(GradingCandidateInput.Create input) {
        final Rank rank = rankService.getRank(input.rankId());
        final Grading grading = gradingService.getGrading(input.gradingId());
        final Member member = memberService.getMember(input.memberId());
        checkIfGradingCandidateExists(input.gradingId(), input.memberId());

        final GradingCandidate candidate = GradingCandidate.builder()
            .paymentMethod(input.paymentMethod())
            .isPaid(input.isPaid())
            .rank(rank)
            .grading(grading)
            .member(member)
            .organizationId(OrganizationContext.require())
            .build();

        AuditLog.log("creating grading candidate {}", candidate.getId());
        gradingCandidateRepository.save(candidate);
        return candidate;
    }

    private void checkIfGradingCandidateExists(UUID gradingId, UUID memberId) {
        if (gradingCandidateRepository.exists(gradingId, memberId)) {
            throw new GradingCandidateExistsException();
        }
    }

    @Transactional
    public GradingCandidate updateGradingCandidate(UUID id, GradingCandidateInput.Update input) {
        AuditLog.log("updating grading candidate {}", id);

        final GradingCandidate gradingCandidate = getGradingCandidate(id);
        final Rank rank = rankService.getRank(input.rankId());

        final GradingCandidate updatedGradingCandidate = gradingCandidate.toBuilder()
            .paymentMethod(input.paymentMethod())
            .isPaid(input.isPaid())
            .rank(rank)
            .build();

        gradingCandidateRepository.update(updatedGradingCandidate);
        return updatedGradingCandidate;
    }

    public GradingCandidate getGradingCandidate(UUID id) {
        logger.debug("Getting grading candidate {}", id);

        return gradingCandidateRepository.read(id)
            .orElseThrow(() -> new GradingCandidateNotFoundException());
    }

    @Transactional
    public GradingCandidate deleteGradingCandidate(UUID id) {
        AuditLog.log("deleting grading candidate {}", id);

        final GradingCandidate gradingCandidate = getGradingCandidate(id);
        gradingCandidateRepository.delete(id);
        return gradingCandidate;
    }

    public Page<GradingCandidate> getGradingCandidates(
        CommonInput.PaginationInput paginationInput,
        GradingCandidateInput.GetGradingCandidates filterInput) {
        return gradingCandidateRepository.read(Pageable.from(paginationInput), filterInput);
    }
}
