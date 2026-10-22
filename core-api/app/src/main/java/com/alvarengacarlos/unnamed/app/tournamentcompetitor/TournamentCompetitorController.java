package com.alvarengacarlos.unnamed.app.tournamentcompetitor;

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

import com.alvarengacarlos.unnamed.app.common.CommonInput;
import com.alvarengacarlos.unnamed.app.common.CommonOutput;
import com.alvarengacarlos.unnamed.app.common.Locations;

@Tag(name = "Tournament competitor")
@RestController
@RequestMapping("tournament-competitors")
public class TournamentCompetitorController {
    private final TournamentCompetitorService tournamentCompetitorService;

    public TournamentCompetitorController(TournamentCompetitorService tournamentCompetitorService) {
        this.tournamentCompetitorService = tournamentCompetitorService;
    }

    @PostMapping
    public ResponseEntity<TournamentCompetitorOutput> createTournamentCompetitor(
            @Validated @RequestBody TournamentCompetitorInput.Create input) {
        final TournamentCompetitorOutput output = TournamentCompetitorOutput.from(
                tournamentCompetitorService.createTournamentCompetitor(input));
        return ResponseEntity.created(Locations.of(output.id()))
            .body(output);
    }

    @GetMapping
    public ResponseEntity<CommonOutput.PaginationOutput<TournamentCompetitorOutput>> getTournamentCompetitors(
            @Validated CommonInput.PaginationInput input,
        @Validated TournamentCompetitorInput.GetTournamentCompetitors filterInput) {
        return ResponseEntity.ok(CommonOutput.PaginationOutput.from(
            tournamentCompetitorService.getTournamentCompetitors(input, filterInput),
            TournamentCompetitorOutput::from));
    }

    @GetMapping("{id}")
    public ResponseEntity<TournamentCompetitorOutput> getTournamentCompetitor(
        @PathVariable UUID id) {
        final TournamentCompetitorOutput output = TournamentCompetitorOutput.from(
                tournamentCompetitorService.getTournamentCompetitor(id));
        return ResponseEntity.ok(output);
    }

    @PutMapping("{id}")
    public ResponseEntity<TournamentCompetitorOutput> updateTournamentCompetitor(
            @PathVariable UUID id,
            @Validated @RequestBody TournamentCompetitorInput.Update input) {
        final TournamentCompetitorOutput output = TournamentCompetitorOutput.from(
                tournamentCompetitorService.updateTournamentCompetitor(id, input));
        return ResponseEntity.ok(output);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<TournamentCompetitorOutput> deleteTournamentCompetitor(
        @PathVariable UUID id) {
        final TournamentCompetitorOutput output = TournamentCompetitorOutput.from(
                tournamentCompetitorService.deleteTournamentCompetitor(id));
        return ResponseEntity.ok(output);
    }

    @PostMapping("bulk")
    public ResponseEntity<List<TournamentCompetitorOutput>> createTournamentCompetitorsInBulk(
            @Validated @RequestBody TournamentCompetitorInput.CreateInBulk input) {
        final List<TournamentCompetitorOutput> output = 
            tournamentCompetitorService.createTournamentCompetitorsInBulk(input)
                .stream()
                .map(TournamentCompetitorOutput::from)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }
}
