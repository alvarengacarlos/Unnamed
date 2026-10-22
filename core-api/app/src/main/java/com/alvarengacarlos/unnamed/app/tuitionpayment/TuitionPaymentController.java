package com.alvarengacarlos.unnamed.app.tuitionpayment;

import java.util.UUID;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvarengacarlos.unnamed.app.common.CommonInput;
import com.alvarengacarlos.unnamed.app.common.CommonOutput;
import com.alvarengacarlos.unnamed.app.common.Locations;

@Tag(name = "Tuition payment")
@RestController
@RequestMapping("tuition-payments")
public class TuitionPaymentController {
    private final TuitionPaymentService tuitionPaymentService;

    public TuitionPaymentController(TuitionPaymentService tuitionPaymentService) {
        this.tuitionPaymentService = tuitionPaymentService;
    }

    @PostMapping
    public ResponseEntity<TuitionPaymentOutput> createTuitionPayment(
        @Validated @RequestBody TuitionPaymentInput.Create input) {
        final TuitionPaymentOutput tuitionPaymentOutput = TuitionPaymentOutput.from(
            tuitionPaymentService.createTuitionPayment(input));
        return ResponseEntity.created(Locations.of(tuitionPaymentOutput.id()))
            .body(tuitionPaymentOutput);
    }

    @GetMapping
    public ResponseEntity<CommonOutput.PaginationOutput<TuitionPaymentOutput>> getTuitionPayments(
        @Validated CommonInput.PaginationInput input,
        @Validated TuitionPaymentInput.GetTuitionPayments filterInput) {
        return ResponseEntity.ok(CommonOutput.PaginationOutput.from(
            tuitionPaymentService.getTuitionPayments(input, filterInput),
            TuitionPaymentOutput::from));
    }

    @GetMapping("{id}")
    public ResponseEntity<TuitionPaymentOutput> getTuitionPayment(
        @PathVariable UUID id) {
        final TuitionPaymentOutput tuitionPaymentOutput = TuitionPaymentOutput.from(
            tuitionPaymentService.getTuitionPayment(id));
        return ResponseEntity.ok(tuitionPaymentOutput);
    }

    @PutMapping("{id}")
    public ResponseEntity<TuitionPaymentOutput> updateTuitionPayment(
        @PathVariable UUID id, @Validated @RequestBody TuitionPaymentInput.Update input) {
        final TuitionPaymentOutput tuitionPaymentOutput = TuitionPaymentOutput.from(
            tuitionPaymentService.updateTuitionPayment(id, input));
        return ResponseEntity.ok(tuitionPaymentOutput);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<TuitionPaymentOutput> deleteTuitionPayment(
        @PathVariable UUID id) {
        final TuitionPaymentOutput tuitionPaymentOutput = TuitionPaymentOutput.from(
            tuitionPaymentService.deleteTuitionPayment(id));
        return ResponseEntity.ok(tuitionPaymentOutput);
    }
}
