package com.alvarengacarlos.unnamed.coreapi.grading;

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

import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;
import com.alvarengacarlos.unnamed.coreapi.common.Locations;

@Tag(name = "Grading")
@RestController
@RequestMapping("gradings")
public class GradingController {
    private final GradingService gradingService;

    public GradingController(GradingService gradingService) {
        this.gradingService = gradingService;
    }

    @PostMapping
    public ResponseEntity<GradingOutput> createGrading(
        @Validated @RequestBody GradingInput.CreateUpdate input) {
        final GradingOutput gradingOutput = GradingOutput.from(gradingService.createGrading(input));
        return ResponseEntity.created(Locations.of(gradingOutput.id()))
            .body(gradingOutput);
    }

    @GetMapping
    public ResponseEntity<CommonOutput.PaginationOutput<GradingOutput>> getGradings(
        @Validated CommonInput.PaginationInput input) {
        return ResponseEntity.ok(CommonOutput.PaginationOutput.from(
            gradingService.getGradings(input),
            GradingOutput::from));
    }

    @GetMapping("{id}")
    public ResponseEntity<GradingOutput> getGrading(
        @PathVariable UUID id) {
        final GradingOutput gradingOutput = GradingOutput.from(gradingService.getGrading(id));
        return ResponseEntity.ok(gradingOutput);
    }

    @PutMapping("{id}")
    public ResponseEntity<GradingOutput> updateGrading(
        @PathVariable UUID id, 
        @Validated @RequestBody GradingInput.CreateUpdate input) {
        final GradingOutput gradingOutput = GradingOutput.from(gradingService.updateGrading(id, input));
        return ResponseEntity.ok(gradingOutput);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<GradingOutput> deleteGrading(
        @PathVariable UUID id) {
        final GradingOutput gradingOutput = GradingOutput.from(gradingService.deleteGrading(id));
        return ResponseEntity.ok(gradingOutput);
    }
}
