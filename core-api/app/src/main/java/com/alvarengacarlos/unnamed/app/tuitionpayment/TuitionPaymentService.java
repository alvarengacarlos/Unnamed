package com.alvarengacarlos.unnamed.app.tuitionpayment;

import java.time.Instant;
import java.time.Month;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvarengacarlos.unnamed.app.OrganizationContext;
import com.alvarengacarlos.unnamed.app.common.AuditLog;
import com.alvarengacarlos.unnamed.app.common.CommonInput;
import com.alvarengacarlos.unnamed.app.common.Page;
import com.alvarengacarlos.unnamed.app.common.Pageable;
import com.alvarengacarlos.unnamed.app.studentenrollment.StudentEnrollment;
import com.alvarengacarlos.unnamed.app.studentenrollment.StudentEnrollmentService;

@Service
public class TuitionPaymentService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TuitionPaymentRepository tuitionPaymentRepository;
    private final StudentEnrollmentService studentEnrollmentService;

    public TuitionPaymentService(
        TuitionPaymentRepository tuitionPaymentRepository,
        StudentEnrollmentService studentEnrollmentService) {
        this.tuitionPaymentRepository = tuitionPaymentRepository;
        this.studentEnrollmentService = studentEnrollmentService;
    }

    /**
     * Adds the unpaid tuition payment of the month to each active enrollment of every organization, so nobody has
     * to add them by hand. Running it again for the same month adds nothing and changes nothing.
     */
    @Transactional
    public int createMonthlyTuitionPayments(YearMonth yearMonth) {
        final int created = tuitionPaymentRepository.createUnpaidForActiveEnrollments(yearMonth, Instant.now());
        logger.info("Created {} unpaid tuition payments of {}", created, yearMonth);
        return created;
    }

    @Transactional
    public TuitionPayment createTuitionPayment(TuitionPaymentInput.Create input) {
        final StudentEnrollment studentEnrollment = studentEnrollmentService.getStudentEnrollment(
            input.studentEnrollmentId());
        checkIfTuitionPaymentExistsForCreate(
            input.studentEnrollmentId(),
            input.month(),
            input.year());

        final TuitionPayment tuitionPayment = TuitionPayment.builder()
            .month(input.month())
            .year(input.year())
            .isPaid(input.isPaid())
            .studentEnrollment(studentEnrollment)
            .organizationId(OrganizationContext.require())
            .build();
        AuditLog.log("creating tuition payment {}", tuitionPayment.getId());
        tuitionPaymentRepository.save(tuitionPayment);
        return tuitionPayment;
    }

    private void checkIfTuitionPaymentExistsForCreate(UUID studentEnrollmentId, Month month, Short year) {
        if (tuitionPaymentRepository.exists(studentEnrollmentId, month, year, Optional.empty())) {
            throw new TuitionPaymentExistsException();
        }
    }

    @Transactional
    public TuitionPayment updateTuitionPayment(UUID id, TuitionPaymentInput.Update input) {
        AuditLog.log("updating tuition payment {}", id);

        final TuitionPayment tuitionPayment = getTuitionPayment(id);
        checkIfTuitionPaymentExistsForUpdate(
            id,
            tuitionPayment.getStudentEnrollment().getId(),
            input.month(),
            input.year());

        final TuitionPayment updatedTuitionPayment = tuitionPayment.toBuilder()
            .month(input.month())
            .year(input.year())
            .isPaid(input.isPaid())
            .build();
        tuitionPaymentRepository.update(updatedTuitionPayment);
        return updatedTuitionPayment;
    }

    public TuitionPayment getTuitionPayment(UUID id) {
        logger.debug("Getting tuition payment {}", id);
        
        return tuitionPaymentRepository.read(id)
            .orElseThrow(() -> new TuitionPaymentNotFoundException());
    }

    private void checkIfTuitionPaymentExistsForUpdate(
        UUID id,
        UUID studentEnrollmentId,
        Month month,
        Short year) {
        if (tuitionPaymentRepository.exists(studentEnrollmentId, month, year, Optional.of(id))) {
            throw new TuitionPaymentExistsException();
        }
    }

    @Transactional
    public TuitionPayment deleteTuitionPayment(UUID id) {
        AuditLog.log("deleting tuition payment {}", id);

        final TuitionPayment tuitionPayment = getTuitionPayment(id);
        tuitionPaymentRepository.delete(id);
        return tuitionPayment;
    }

    public Page<TuitionPayment> getTuitionPayments(
        CommonInput.PaginationInput paginationInput,
        TuitionPaymentInput.GetTuitionPayments filterInput) {
        return tuitionPaymentRepository.read(Pageable.from(paginationInput), filterInput);
    }
}
