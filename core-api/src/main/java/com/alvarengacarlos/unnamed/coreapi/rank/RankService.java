package com.alvarengacarlos.unnamed.coreapi.rank;

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
import com.alvarengacarlos.unnamed.coreapi.rankcategory.RankCategory;
import com.alvarengacarlos.unnamed.coreapi.rankcategory.RankCategoryService;

@Service
public class RankService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RankRepository rankRepository;
    private final RankCategoryService rankCategoryService;
    
    public RankService(
        RankRepository rankRepository, 
        RankCategoryService rankCategoryService) {
        this.rankRepository = rankRepository;
        this.rankCategoryService = rankCategoryService;
    }

    @Transactional
    public Rank createRank(RankInput.Create input) { 
        final RankCategory rankCategory = rankCategoryService.getRankCategory(input.categoryId());
        checkIfRankExists(input.categoryId(), input.order());

        final Rank rank = Rank.builder()
            .name(input.name())
            .order(input.order())
            .category(rankCategory)
            .organizationId(OrganizationContext.require())
            .build();
        AuditLog.log("creating rank {}", rank.getId());
        rankRepository.save(rank);

        return rank;
    }

    private void checkIfRankExists(UUID rankCategoryId, Byte order) {
        if (rankRepository.exists(rankCategoryId, order)) {
            throw new RankExistsException();
        }
    }

    @Transactional
    public Rank updateRank(UUID id, RankInput.Update input) {
        AuditLog.log("updating rank {}", id);

        final Rank rank = getRank(id);

        final Rank updatedRank = rank.toBuilder()
            .name(input.name())
            .build();
        rankRepository.update(updatedRank);

        return updatedRank;
    }

    public Rank getRank(UUID rankId) {
        logger.debug("Getting rank {}", rankId);

        return rankRepository.read(rankId)
            .orElseThrow(() -> new RankNotFoundException());
    }
    
    @Transactional
    public Rank deleteRank(UUID id) {
        AuditLog.log("deleting rank {}", id);

        final Rank rank = getRank(id);
        rankRepository.delete(id);
        return rank;
    }

    public Page<Rank> getRanks(
        CommonInput.PaginationInput paginationInput,
        RankInput.GetRanks filterInput) {
        return rankRepository.read(Pageable.from(paginationInput), filterInput);
    }
}
