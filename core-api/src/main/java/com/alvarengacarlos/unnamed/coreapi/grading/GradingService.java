package com.alvarengacarlos.unnamed.coreapi.grading;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
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
public class GradingService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final GradingRepository gradingRepository;
    private final RankService rankService;

    public GradingService(
        GradingRepository gradingRepository, 
        RankService rankService) {
        this.gradingRepository = gradingRepository;
        this.rankService = rankService;
    }

    @Transactional
    public Grading createGrading(GradingInput.CreateUpdate input) {
        checkRegistrationPeriod(input);
        final Set<GradingPrice> gradingPrices = buildGradingPrices(input);
        final Grading grading = Grading.builder()
            .name(input.name())
            .when(input.when())
            .where(input.where())
            .registrationPeriodStart(input.registrationPeriodStart())
            .registrationPeriodEnd(input.registrationPeriodEnd())
            .gradingPrices(gradingPrices)
            .organizationId(OrganizationContext.require())
            .build();

        AuditLog.log("creating grading {}", grading.getId());
        gradingRepository.save(grading);
        return grading;
    }

    // The registrations may open and close on the same day, but not close before they open.
    private void checkRegistrationPeriod(GradingInput.CreateUpdate input) {
        if (input.registrationPeriodEnd().isBefore(input.registrationPeriodStart())) {
            throw new RegistrationPeriodEndsBeforeItStartsException();
        }
    }

    private Set<GradingPrice> buildGradingPrices(GradingInput.CreateUpdate input) {
        return input.prices()
            .entrySet()
            .stream()
            .map(entry -> {
                final UUID rankId = entry.getKey();
                final BigDecimal price = entry.getValue();

                final Rank rank = rankService.getRank(rankId);                
                final GradingPrice gradingPrice = new GradingPrice.Builder()
                    .price(price)
                    .rank(rank)
                    .build();

                return gradingPrice;
            }).collect(Collectors.toSet());
    }

    @Transactional
    public Grading updateGrading(UUID id, GradingInput.CreateUpdate input) {
        AuditLog.log("updating grading {}", id);

        checkRegistrationPeriod(input);
        final Grading grading = getGrading(id);
        final Set<GradingPrice> gradingPrices = buildGradingPrices(input);
        
        final Grading updatedGrading = grading.toBuilder()
            .name(input.name())
            .when(input.when())
            .where(input.where())
            .registrationPeriodStart(input.registrationPeriodStart())
            .registrationPeriodEnd(input.registrationPeriodEnd())
            .gradingPrices(gradingPrices)
            .build();

        gradingRepository.update(updatedGrading);
        return updatedGrading;
    }
    
    public Grading getGrading(UUID id) {
        logger.debug("Getting grading {}", id);

        return gradingRepository.read(id)
            .orElseThrow(() -> new GradingNotFoundException());
    }

    @Transactional
    public Grading deleteGrading(UUID id) {
        AuditLog.log("deleting grading {}", id);

        final Grading grading = getGrading(id);
        gradingRepository.delete(id);
        return grading;
    }

    public Page<Grading> getGradings(CommonInput.PaginationInput paginationInput) {
        return gradingRepository.read(Pageable.from(paginationInput));
    }
}
