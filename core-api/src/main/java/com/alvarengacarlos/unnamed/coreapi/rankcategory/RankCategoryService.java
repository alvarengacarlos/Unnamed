package com.alvarengacarlos.unnamed.coreapi.rankcategory;

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

@Service
public class RankCategoryService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RankCategoryRepository rankCategoryRepository;
    
    public RankCategoryService(
        RankCategoryRepository rankCategoryRepository) {
        this.rankCategoryRepository = rankCategoryRepository;
    }

    @Transactional
    public RankCategory createRankCategory(RankCategoryInput.CreateUpdate input) {
        final RankCategory rankCategory = RankCategory.builder()
            .name(input.name())
            .organizationId(OrganizationContext.require())
            .build();
        AuditLog.log("creating rank category {}", rankCategory.getId());
        rankCategoryRepository.save(rankCategory);
        return rankCategory;
    }

    @Transactional
    public RankCategory updateRankCategory(UUID id, RankCategoryInput.CreateUpdate input) {
        AuditLog.log("updating rank category {}", id);

        final RankCategory rankCategory = getRankCategory(id);
        final RankCategory updatedRankCategory = rankCategory.toBuilder()
            .name(input.name())
            .build();
        rankCategoryRepository.update(updatedRankCategory);
        return updatedRankCategory;
    }
    
    public RankCategory getRankCategory(UUID id) {
        logger.debug("Getting rank category {}", id);

        return rankCategoryRepository.read(id)
            .orElseThrow(() -> new RankCategoryNotFoundException());
    }

    @Transactional
    public RankCategory deleteRankCategory(UUID id) {
        AuditLog.log("deleting rank category {}", id);

        final RankCategory rankCategory = getRankCategory(id);
        rankCategoryRepository.delete(id);
        return rankCategory;
    }
    
    public Page<RankCategory> getRankCategories(CommonInput.PaginationInput paginationInput) {
        return rankCategoryRepository.read(Pageable.from(paginationInput));
    }
}
