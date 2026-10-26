package com.alvarengacarlos.unnamed.coreapi.rank;

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

@Tag(name = "Rank")
@RestController
@RequestMapping("ranks")
public class RankController {
    private final RankService rankService;

    public RankController(RankService rankService) {
        this.rankService = rankService;
    }

    @PostMapping
    public ResponseEntity<RankOutput> createRank(
        @Validated @RequestBody RankInput.Create input) {
        final RankOutput rankOutput = RankOutput.from(rankService.createRank(input));
        return ResponseEntity.created(Locations.of(rankOutput.id()))
            .body(rankOutput);
    }

    @GetMapping
    public ResponseEntity<CommonOutput.PaginationOutput<RankOutput>> getRanks(
        @Validated CommonInput.PaginationInput input,
        @Validated RankInput.GetRanks filterInput) {
        return ResponseEntity.ok(CommonOutput.PaginationOutput.from(
            rankService.getRanks(input, filterInput),
            RankOutput::from));
    }

    @GetMapping("{id}")
    public ResponseEntity<RankOutput> getRank(
        @PathVariable UUID id) {
        final RankOutput rankOutput = RankOutput.from(rankService.getRank(id));
        return ResponseEntity.ok(rankOutput);
    }

    @PutMapping("{id}")
    public ResponseEntity<RankOutput> updateRank(
        @PathVariable UUID id,
        @Validated @RequestBody RankInput.Update input) {
        final RankOutput rankOutput = RankOutput.from(rankService.updateRank(id, input));
        return ResponseEntity.ok(rankOutput);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<RankOutput> deleteRank(
        @PathVariable UUID id) {
        final RankOutput rankOutput = RankOutput.from(rankService.deleteRank(id));
        return ResponseEntity.ok(rankOutput);
    }
}
