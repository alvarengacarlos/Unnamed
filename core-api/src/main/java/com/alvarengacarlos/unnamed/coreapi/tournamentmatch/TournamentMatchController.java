package com.alvarengacarlos.unnamed.coreapi.tournamentmatch;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
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

@Tag(name = "Tournament match")
@RestController
@RequestMapping("tournament-matches")
public class TournamentMatchController {
    private final TournamentMatchService tournamentMatchService;

    public TournamentMatchController(TournamentMatchService tournamentMatchService) {
        this.tournamentMatchService = tournamentMatchService;
    }

    @PostMapping
    public ResponseEntity<TournamentMatchOutput> createTournamentMatch(
        @Validated @RequestBody TournamentMatchInput.Create input) {
        final TournamentMatchOutput output = TournamentMatchOutput.from(
                tournamentMatchService.createTournamentMatch(input));
        return ResponseEntity.created(Locations.of(output.id()))
            .body(output);
    }

    @GetMapping
    public ResponseEntity<CommonOutput.PaginationOutput<TournamentMatchOutput>> getTournamentMatches(
        @Validated CommonInput.PaginationInput input,
        @Validated TournamentMatchInput.GetTournamentMatches filterInput) {
        return ResponseEntity.ok(CommonOutput.PaginationOutput.from(
            tournamentMatchService.getTournamentMatches(input, filterInput),
            TournamentMatchOutput::from));
    }

    @GetMapping("{id}")
    public ResponseEntity<TournamentMatchOutput> getTournamentMatch(
        @PathVariable UUID id) {
        final TournamentMatchOutput output = TournamentMatchOutput.from(
                tournamentMatchService.getTournamentMatch(id));
        return ResponseEntity.ok(output);
    }

    @PutMapping("{id}")
    public ResponseEntity<TournamentMatchOutput> updateTournamentMatch(
        @PathVariable UUID id,
        @Validated @RequestBody TournamentMatchInput.Update input) {
        final TournamentMatchOutput output = TournamentMatchOutput.from(
                tournamentMatchService.updateTournamentMatch(id, input));
        return ResponseEntity.ok(output);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<TournamentMatchOutput> deleteTournamentMatch(
        @PathVariable UUID id) {
        final TournamentMatchOutput output = TournamentMatchOutput.from(
                tournamentMatchService.deleteTournamentMatch(id));
        return ResponseEntity.ok(output);
    }

    @PostMapping("bulk")
    public ResponseEntity<List<TournamentMatchOutput>> createTournamentMatchesInBulk(
        @Validated @RequestBody TournamentMatchInput.CreateInBulk input) {
        final List<TournamentMatchOutput> output = tournamentMatchService.createTournamentMatchesInBulk(
            input)
            .stream()
            .map(TournamentMatchOutput::from)
            .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }
}
