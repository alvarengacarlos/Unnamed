package com.alvarengacarlos.unnamed.coreapi.tuition;

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
public class TuitionService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TuitionRepository tuitionRepository;

    public TuitionService(
        TuitionRepository tuitionRepository) {
        this.tuitionRepository = tuitionRepository;
    }

    @Transactional
    public Tuition createTuition(TuitionInput.CreateUpdate input) {
        final Tuition tuition = Tuition.builder()
            .name(input.name())
            .price(input.price())
            .organizationId(OrganizationContext.require())
            .build();
        AuditLog.log("creating tuition {}", tuition.getId());
        tuitionRepository.save(tuition);
        return tuition;
    }

    @Transactional
    public Tuition updateTuition(UUID id, TuitionInput.CreateUpdate input) {
        AuditLog.log("updating tuition {}", id);

        final Tuition tuition = getTuition(id);
        final Tuition updatedTuition = tuition.toBuilder()
            .name(input.name())
            .price(input.price())
            .build();
        tuitionRepository.update(updatedTuition);
        return updatedTuition;
    }
   
    public Tuition getTuition(UUID id) {
        logger.debug("Getting tuition {}", id);

        return tuitionRepository.read(id)
            .orElseThrow(() -> new TuitionNotFoundException());
    }
    
    @Transactional
    public Tuition deleteTuition(UUID id) {
        AuditLog.log("deleting tuition {}", id);

        final Tuition tuition = getTuition(id);
        tuitionRepository.delete(id);
        return tuition;
    }

    public Page<Tuition> getTuitions(CommonInput.PaginationInput paginationInput) {
        return tuitionRepository.read(Pageable.from(paginationInput));
    }
}
