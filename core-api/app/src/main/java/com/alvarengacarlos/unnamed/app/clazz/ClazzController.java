package com.alvarengacarlos.unnamed.app.clazz;

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

@Tag(name = "Clazz")
@RestController
@RequestMapping("clazzes")
public class ClazzController {
    private final ClazzService clazzService;

    public ClazzController(ClazzService clazzService) {
        this.clazzService = clazzService;
    }

    @PostMapping
    public ResponseEntity<ClazzOutput> createClazz(
        @Validated @RequestBody ClazzInput.CreateUpdate input) {
        final ClazzOutput clazzOutput = ClazzOutput.from(clazzService.createClazz(input));
        return ResponseEntity.created(Locations.of(clazzOutput.id()))
            .body(clazzOutput);
    }

    @GetMapping
    public ResponseEntity<CommonOutput.PaginationOutput<ClazzOutput>> getClazzes(
        @Validated CommonInput.PaginationInput input,
        @Validated ClazzInput.GetClazzes filterInput) {
        return ResponseEntity.ok(CommonOutput.PaginationOutput.from(
            clazzService.getClazzes(input, filterInput),
            ClazzOutput::from));
    }

    @GetMapping("{id}")
    public ResponseEntity<ClazzOutput> getClazz(
        @PathVariable UUID id) {
        final ClazzOutput clazzOutput = ClazzOutput.from(clazzService.getClazz(id));
        return ResponseEntity.ok(clazzOutput);
    }

    @PutMapping("{id}")
    public ResponseEntity<ClazzOutput> updateClazz(
        @PathVariable UUID id, 
        @Validated @RequestBody ClazzInput.CreateUpdate input) {
        final ClazzOutput clazzOutput = ClazzOutput.from(clazzService.updateClazz(id, input));
        return ResponseEntity.ok(clazzOutput);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<ClazzOutput> deleteClazz(
        @PathVariable UUID id) {
        final ClazzOutput clazzOutput = ClazzOutput.from(clazzService.deleteClazz(id));
        return ResponseEntity.ok(clazzOutput);
    }
}
