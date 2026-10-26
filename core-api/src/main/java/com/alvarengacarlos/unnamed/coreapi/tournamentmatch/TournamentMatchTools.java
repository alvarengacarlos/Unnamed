package com.alvarengacarlos.unnamed.coreapi.tournamentmatch;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.alvarengacarlos.unnamed.coreapi.assistant.AssistantTools;
import com.alvarengacarlos.unnamed.coreapi.assistant.ToolParams;
import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;

/**
 * The operations on the tournament matches that the assistant may call. A tournament match is a match between two
 * competitors of a tournament, with its result.
 */
@Component
public class TournamentMatchTools implements AssistantTools {
    private static final String ID = "Id of the tournament match";
    private final TournamentMatchService tournamentMatchService;

    public TournamentMatchTools(TournamentMatchService tournamentMatchService) {
        this.tournamentMatchService = tournamentMatchService;
    }

    @Tool(description = "Creates a tournament match")
    public TournamentMatchOutput createTournamentMatch(
        @ToolParam(description = "The tournament match to create") TournamentMatchInput.Create input) {
        return TournamentMatchOutput.from(tournamentMatchService.createTournamentMatch(input));
    }

    @Tool(description = "Lists the tournament matches, one page at a time")
    public CommonOutput.PaginationOutput<TournamentMatchOutput> getTournamentMatches(
        @ToolParam(description = ToolParams.PAGE, required = false) Integer page,
        @ToolParam(description = ToolParams.PAGE_SIZE, required = false) Integer pageSize,
        @ToolParam(description = "Filters of the tournament matches")
            TournamentMatchInput.GetTournamentMatches filterInput) {
        final CommonInput.PaginationInput paginationInput = new CommonInput.PaginationInput(page, pageSize);
        return CommonOutput.PaginationOutput.from(
            tournamentMatchService.getTournamentMatches(paginationInput, filterInput),
            TournamentMatchOutput::from);
    }

    @Tool(description = "Gets a tournament match by its id")
    public TournamentMatchOutput getTournamentMatch(
        @ToolParam(description = ID) UUID id) {
        return TournamentMatchOutput.from(tournamentMatchService.getTournamentMatch(id));
    }

    @Tool(description = "Updates a tournament match, replacing all of its data")
    public TournamentMatchOutput updateTournamentMatch(
        @ToolParam(description = ID) UUID id,
        @ToolParam(description = "The new data of the tournament match") TournamentMatchInput.Update input) {
        return TournamentMatchOutput.from(tournamentMatchService.updateTournamentMatch(id, input));
    }

    @Tool(description = "Deletes a tournament match")
    public TournamentMatchOutput deleteTournamentMatch(
        @ToolParam(description = ID) UUID id) {
        return TournamentMatchOutput.from(tournamentMatchService.deleteTournamentMatch(id));
    }

    @Tool(description = "Creates several tournament matches at once")
    public List<TournamentMatchOutput> createTournamentMatchesInBulk(
        @ToolParam(description = "The tournament matches to create") TournamentMatchInput.CreateInBulk input) {
        return tournamentMatchService.createTournamentMatchesInBulk(input)
            .stream()
            .map(TournamentMatchOutput::from)
            .toList();
    }
}
