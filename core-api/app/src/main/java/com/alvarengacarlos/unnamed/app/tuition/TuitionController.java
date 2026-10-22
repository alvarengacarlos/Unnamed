package com.alvarengacarlos.unnamed.app.tuition;

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

@Tag(name = "Tuition")
@RestController
@RequestMapping("tuitions")
public class TuitionController {
    private final TuitionService tuitionService;

    public TuitionController(TuitionService tuitionService) {
        this.tuitionService = tuitionService;
    }

    @PostMapping
    public ResponseEntity<TuitionOutput> createTuition(
        @Validated @RequestBody TuitionInput.CreateUpdate input) {
        final TuitionOutput tuitionOutput = TuitionOutput.from(tuitionService.createTuition(input));
        return ResponseEntity.created(Locations.of(tuitionOutput.id()))
            .body(tuitionOutput);
    }

    @GetMapping
    public ResponseEntity<CommonOutput.PaginationOutput<TuitionOutput>> getTuitions(
        @Validated CommonInput.PaginationInput input) {
        return ResponseEntity.ok(CommonOutput.PaginationOutput.from(
            tuitionService.getTuitions(input),
            TuitionOutput::from));
    }

    @GetMapping("{id}")
    public ResponseEntity<TuitionOutput> getTuition(
        @PathVariable UUID id) {
        final TuitionOutput tuitionOutput = TuitionOutput.from(tuitionService.getTuition(id));
        return ResponseEntity.ok(tuitionOutput);
    }

    @PutMapping("{id}")
    public ResponseEntity<TuitionOutput> updateTuition(
        @PathVariable UUID id, 
        @Validated @RequestBody TuitionInput.CreateUpdate input) {
        final TuitionOutput tuitionOutput = TuitionOutput.from(
            tuitionService.updateTuition(id, input));
        return ResponseEntity.ok(tuitionOutput);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<TuitionOutput> deleteTuition(
        @PathVariable UUID id) {
        final TuitionOutput tuitionOutput = TuitionOutput.from(
            tuitionService.deleteTuition(id));
        return ResponseEntity.ok(tuitionOutput);
    }
}
