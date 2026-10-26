package com.alvarengacarlos.unnamed.coreapi.tournamentcompetitor;

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
 * The operations on the tournament competitors that the assistant may call. A tournament competitor is a member who
 * competes in a tournament.
 */
@Component
public class TournamentCompetitorTools implements AssistantTools {
    private static final String ID = "Id of the tournament competitor";
    private final TournamentCompetitorService tournamentCompetitorService;

    public TournamentCompetitorTools(TournamentCompetitorService tournamentCompetitorService) {
        this.tournamentCompetitorService = tournamentCompetitorService;
    }

    @Tool(description = "Creates a tournament competitor")
    public TournamentCompetitorOutput createTournamentCompetitor(
        @ToolParam(description = "The tournament competitor to create") TournamentCompetitorInput.Create input) {
        return TournamentCompetitorOutput.from(tournamentCompetitorService.createTournamentCompetitor(input));
    }

    @Tool(description = "Lists the tournament competitors, one page at a time")
    public CommonOutput.PaginationOutput<TournamentCompetitorOutput> getTournamentCompetitors(
        @ToolParam(description = ToolParams.PAGE, required = false) Integer page,
        @ToolParam(description = ToolParams.PAGE_SIZE, required = false) Integer pageSize,
        @ToolParam(description = "Filters of the tournament competitors")
            TournamentCompetitorInput.GetTournamentCompetitors filterInput) {
        final CommonInput.PaginationInput paginationInput = new CommonInput.PaginationInput(page, pageSize);
        return CommonOutput.PaginationOutput.from(
            tournamentCompetitorService.getTournamentCompetitors(paginationInput, filterInput),
            TournamentCompetitorOutput::from);
    }

    @Tool(description = "Gets a tournament competitor by its id")
    public TournamentCompetitorOutput getTournamentCompetitor(
        @ToolParam(description = ID) UUID id) {
        return TournamentCompetitorOutput.from(tournamentCompetitorService.getTournamentCompetitor(id));
    }

    @Tool(description = "Updates a tournament competitor, replacing all of its data")
    public TournamentCompetitorOutput updateTournamentCompetitor(
        @ToolParam(description = ID) UUID id,
        @ToolParam(description = "The new data of the tournament competitor") TournamentCompetitorInput.Update input) {
        return TournamentCompetitorOutput.from(tournamentCompetitorService.updateTournamentCompetitor(id, input));
    }

    @Tool(description = "Deletes a tournament competitor")
    public TournamentCompetitorOutput deleteTournamentCompetitor(
        @ToolParam(description = ID) UUID id) {
        return TournamentCompetitorOutput.from(tournamentCompetitorService.deleteTournamentCompetitor(id));
    }

    @Tool(description = "Creates several tournament competitors at once")
    public List<TournamentCompetitorOutput> createTournamentCompetitorsInBulk(
        @ToolParam(description = "The tournament competitors to create") TournamentCompetitorInput.CreateInBulk input) {
        return tournamentCompetitorService.createTournamentCompetitorsInBulk(input)
            .stream()
            .map(TournamentCompetitorOutput::from)
            .toList();
    }
}
