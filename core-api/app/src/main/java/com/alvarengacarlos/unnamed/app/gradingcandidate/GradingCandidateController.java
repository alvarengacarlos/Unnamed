package com.alvarengacarlos.unnamed.app.gradingcandidate;

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

@Tag(name = "Grading candidate")
@RestController
@RequestMapping("grading-candidates")
public class GradingCandidateController {
    private final GradingCandidateService gradingCandidateService;

    public GradingCandidateController(GradingCandidateService gradingCandidateService) {
        this.gradingCandidateService = gradingCandidateService;
    }

    @PostMapping
    public ResponseEntity<GradingCandidateOutput> createCandidate(
        @Validated @RequestBody GradingCandidateInput.Create input) {
        final GradingCandidateOutput output = GradingCandidateOutput.from(
            gradingCandidateService.createGradingCandidate(input));
        return ResponseEntity.created(Locations.of(output.id()))
            .body(output);
    }

    @GetMapping
    public ResponseEntity<CommonOutput.PaginationOutput<GradingCandidateOutput>> getCandidates(
        @Validated CommonInput.PaginationInput input,
        @Validated GradingCandidateInput.GetGradingCandidates filterInput) {
        return ResponseEntity.ok(CommonOutput.PaginationOutput.from(
            gradingCandidateService.getGradingCandidates(input, filterInput),
            GradingCandidateOutput::from));
    }

    @GetMapping("{id}")
    public ResponseEntity<GradingCandidateOutput> getCandidate(
        @PathVariable UUID id) {
        final GradingCandidateOutput output = GradingCandidateOutput.from(
            gradingCandidateService.getGradingCandidate(id));
        return ResponseEntity.ok(output);
    }

    @PutMapping("{id}")
    public ResponseEntity<GradingCandidateOutput> updateCandidate(
        @PathVariable UUID id,
        @Validated @RequestBody GradingCandidateInput.Update input) {
        final GradingCandidateOutput output = GradingCandidateOutput.from(
            gradingCandidateService.updateGradingCandidate(id, input));
        return ResponseEntity.ok(output);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<GradingCandidateOutput> deleteCandidate(
        @PathVariable UUID id) {
        final GradingCandidateOutput output = GradingCandidateOutput.from(
            gradingCandidateService.deleteGradingCandidate(id));
        return ResponseEntity.ok(output);
    }
}
