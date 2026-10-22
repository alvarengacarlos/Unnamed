package com.alvarengacarlos.unnamed.app.tournament;

import java.util.List;
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
import com.alvarengacarlos.unnamed.app.member.MemberOutput;

@Tag(name = "Tournament")
@RestController
@RequestMapping("tournaments")
public class TournamentController {
    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @PostMapping
    public ResponseEntity<TournamentOutput> createTournament(
        @Validated @RequestBody TournamentInput.CreateUpdate input) {
        final TournamentOutput tournamentOutput = TournamentOutput.from(
            tournamentService.createTournament(input));
        return ResponseEntity.created(Locations.of(tournamentOutput.id()))
            .body(tournamentOutput);
    }

    @GetMapping
    public ResponseEntity<CommonOutput.PaginationOutput<TournamentOutput>> getTournaments(
        @Validated CommonInput.PaginationInput input) {
        return ResponseEntity.ok(CommonOutput.PaginationOutput.from(
            tournamentService.getTournaments(input),
            TournamentOutput::from));
    }

    @GetMapping("{id}")
    public ResponseEntity<TournamentOutput> getTournament(
        @PathVariable UUID id) {
        final TournamentOutput tournamentOutput = TournamentOutput.from(tournamentService.getTournament(
            id));
        return ResponseEntity.ok(tournamentOutput);
    }

    @PutMapping("{id}")
    public ResponseEntity<TournamentOutput> updateTournament(
        @PathVariable UUID id, 
        @Validated @RequestBody TournamentInput.CreateUpdate input) {
        final TournamentOutput tournamentOutput = TournamentOutput.from(
            tournamentService.updateTournament(id, input));
        return ResponseEntity.ok(tournamentOutput);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<TournamentOutput> deleteTournament(
        @PathVariable UUID id) {
        final TournamentOutput tournamentOutput = TournamentOutput.from(
            tournamentService.deleteTournament(id));
        return ResponseEntity.ok(tournamentOutput);
    }

    @GetMapping("{id}/possible-competitors")
    public ResponseEntity<List<MemberOutput>> getPossibleCompetitors(
        @PathVariable UUID id, 
        @Validated TournamentInput.GetPossibleCompetitors input) {
        final List<MemberOutput> output = tournamentService.getPossibleCompetitors(id, input)
            .stream()
            .map(MemberOutput::from)
            .toList();
        return ResponseEntity.ok(output);
    }
}
