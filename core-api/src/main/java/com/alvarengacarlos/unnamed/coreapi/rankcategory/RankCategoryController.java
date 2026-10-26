package com.alvarengacarlos.unnamed.coreapi.rankcategory;

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

@Tag(name = "Rank category")
@RestController
@RequestMapping("rank-categories")
public class RankCategoryController {
    private final RankCategoryService rankCategoryService;

    public RankCategoryController(RankCategoryService rankCategoryService) {
        this.rankCategoryService = rankCategoryService;
    }

    @PostMapping
    public ResponseEntity<RankCategoryOutput> createRankCategory(
        @Validated @RequestBody RankCategoryInput.CreateUpdate input) {
        final RankCategoryOutput rankCategoryOutput = RankCategoryOutput.from(
            rankCategoryService.createRankCategory(input));
        return ResponseEntity.created(Locations.of(rankCategoryOutput.id()))
            .body(rankCategoryOutput);
    }

    @GetMapping
    public ResponseEntity<CommonOutput.PaginationOutput<RankCategoryOutput>> getRankCategories(
        @Validated CommonInput.PaginationInput input) {
        return ResponseEntity.ok(CommonOutput.PaginationOutput.from(
            rankCategoryService.getRankCategories(input),
            RankCategoryOutput::from));
    }

    @GetMapping("{id}")
    public ResponseEntity<RankCategoryOutput> getRankCategory(
        @PathVariable UUID id) {
        final RankCategoryOutput rankCategoryOutput = RankCategoryOutput.from(
            rankCategoryService.getRankCategory(id));
        return ResponseEntity.ok(rankCategoryOutput);
    }

    @PutMapping("{id}")
    public ResponseEntity<RankCategoryOutput> updateRankCategory(
        @PathVariable UUID id, 
        @Validated @RequestBody RankCategoryInput.CreateUpdate input) {
        final RankCategoryOutput rankCategoryOutput = RankCategoryOutput.from(
            rankCategoryService.updateRankCategory(id, input));
        return ResponseEntity.ok(rankCategoryOutput);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<RankCategoryOutput> deleteRankCategory(
        @PathVariable UUID id) {
        final RankCategoryOutput rankCategoryOutput = RankCategoryOutput.from(
            rankCategoryService.deleteRankCategory(id));
        return ResponseEntity.ok(rankCategoryOutput);
    }
}
