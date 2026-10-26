package com.alvarengacarlos.unnamed.coreapi.tournament;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.alvarengacarlos.unnamed.coreapi.assistant.AssistantTools;
import com.alvarengacarlos.unnamed.coreapi.assistant.ToolParams;
import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;
import com.alvarengacarlos.unnamed.coreapi.member.MemberOutput;

/**
 * The operations on the tournaments that the assistant may call. A tournament is a competition of the organization,
 * with a period, a format and the criteria that divide the competitors.
 */
@Component
public class TournamentTools implements AssistantTools {
    private static final String ID = "Id of the tournament";
    private final TournamentService tournamentService;

    public TournamentTools(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @Tool(description = "Creates a tournament")
    public TournamentOutput createTournament(
        @ToolParam(description = "The tournament to create") TournamentInput.CreateUpdate input) {
        return TournamentOutput.from(tournamentService.createTournament(input));
    }

    @Tool(description = "Lists the tournaments, one page at a time")
    public CommonOutput.PaginationOutput<TournamentOutput> getTournaments(
        @ToolParam(description = ToolParams.PAGE, required = false) Integer page,
        @ToolParam(description = ToolParams.PAGE_SIZE, required = false) Integer pageSize) {
        final CommonInput.PaginationInput paginationInput = new CommonInput.PaginationInput(page, pageSize);
        return CommonOutput.PaginationOutput.from(
            tournamentService.getTournaments(paginationInput),
            TournamentOutput::from);
    }

    @Tool(description = "Gets a tournament by its id")
    public TournamentOutput getTournament(
        @ToolParam(description = ID) UUID id) {
        return TournamentOutput.from(tournamentService.getTournament(id));
    }

    @Tool(description = "Updates a tournament, replacing all of its data")
    public TournamentOutput updateTournament(
        @ToolParam(description = ID) UUID id,
        @ToolParam(description = "The new data of the tournament") TournamentInput.CreateUpdate input) {
        return TournamentOutput.from(tournamentService.updateTournament(id, input));
    }

    @Tool(description = "Deletes a tournament")
    public TournamentOutput deleteTournament(
        @ToolParam(description = ID) UUID id) {
        return TournamentOutput.from(tournamentService.deleteTournament(id));
    }

    @Tool(description = "Lists the members who may compete in a tournament, by the criteria given")
    public List<MemberOutput> getPossibleCompetitors(
        @ToolParam(description = ID) UUID id,
        @ToolParam(description = "Criteria of the competitors") TournamentInput.GetPossibleCompetitors input) {
        return tournamentService.getPossibleCompetitors(id, input)
            .stream()
            .map(MemberOutput::from)
            .toList();
    }
}
