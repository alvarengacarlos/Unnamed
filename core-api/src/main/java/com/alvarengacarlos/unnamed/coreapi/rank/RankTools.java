package com.alvarengacarlos.unnamed.coreapi.rank;

import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.alvarengacarlos.unnamed.coreapi.assistant.AssistantTools;
import com.alvarengacarlos.unnamed.coreapi.assistant.ToolParams;
import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;

/**
 * The operations on the ranks that the assistant may call. A rank is a level (e.g. a belt) of a rank category, ordered
 * inside the category.
 */
@Component
public class RankTools implements AssistantTools {
    private static final String ID = "Id of the rank";
    private final RankService rankService;

    public RankTools(RankService rankService) {
        this.rankService = rankService;
    }

    @Tool(description = "Creates a rank")
    public RankOutput createRank(
        @ToolParam(description = "The rank to create") RankInput.Create input) {
        return RankOutput.from(rankService.createRank(input));
    }

    @Tool(description = "Lists the ranks, one page at a time")
    public CommonOutput.PaginationOutput<RankOutput> getRanks(
        @ToolParam(description = ToolParams.PAGE, required = false) Integer page,
        @ToolParam(description = ToolParams.PAGE_SIZE, required = false) Integer pageSize,
        @ToolParam(description = "Filters of the ranks") RankInput.GetRanks filterInput) {
        final CommonInput.PaginationInput paginationInput = new CommonInput.PaginationInput(page, pageSize);
        return CommonOutput.PaginationOutput.from(
            rankService.getRanks(paginationInput, filterInput),
            RankOutput::from);
    }

    @Tool(description = "Gets a rank by its id")
    public RankOutput getRank(
        @ToolParam(description = ID) UUID id) {
        return RankOutput.from(rankService.getRank(id));
    }

    @Tool(description = "Updates a rank, replacing all of its data")
    public RankOutput updateRank(
        @ToolParam(description = ID) UUID id,
        @ToolParam(description = "The new data of the rank") RankInput.Update input) {
        return RankOutput.from(rankService.updateRank(id, input));
    }

    @Tool(description = "Deletes a rank")
    public RankOutput deleteRank(
        @ToolParam(description = ID) UUID id) {
        return RankOutput.from(rankService.deleteRank(id));
    }
}
