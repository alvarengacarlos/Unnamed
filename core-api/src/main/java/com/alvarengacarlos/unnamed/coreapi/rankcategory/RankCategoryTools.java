package com.alvarengacarlos.unnamed.coreapi.rankcategory;

import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.alvarengacarlos.unnamed.coreapi.assistant.AssistantTools;
import com.alvarengacarlos.unnamed.coreapi.assistant.ToolParams;
import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;

/**
 * The operations on the rank categories that the assistant may call. A rank category is a group of ranks (e.g. the
 * belts of adults or of kids).
 */
@Component
public class RankCategoryTools implements AssistantTools {
    private static final String ID = "Id of the rank category";
    private final RankCategoryService rankCategoryService;

    public RankCategoryTools(RankCategoryService rankCategoryService) {
        this.rankCategoryService = rankCategoryService;
    }

    @Tool(description = "Creates a rank category")
    public RankCategoryOutput createRankCategory(
        @ToolParam(description = "The rank category to create") RankCategoryInput.CreateUpdate input) {
        return RankCategoryOutput.from(rankCategoryService.createRankCategory(input));
    }

    @Tool(description = "Lists the rank categories, one page at a time")
    public CommonOutput.PaginationOutput<RankCategoryOutput> getRankCategories(
        @ToolParam(description = ToolParams.PAGE, required = false) Integer page,
        @ToolParam(description = ToolParams.PAGE_SIZE, required = false) Integer pageSize) {
        final CommonInput.PaginationInput paginationInput = new CommonInput.PaginationInput(page, pageSize);
        return CommonOutput.PaginationOutput.from(
            rankCategoryService.getRankCategories(paginationInput),
            RankCategoryOutput::from);
    }

    @Tool(description = "Gets a rank category by its id")
    public RankCategoryOutput getRankCategory(
        @ToolParam(description = ID) UUID id) {
        return RankCategoryOutput.from(rankCategoryService.getRankCategory(id));
    }

    @Tool(description = "Updates a rank category, replacing all of its data")
    public RankCategoryOutput updateRankCategory(
        @ToolParam(description = ID) UUID id,
        @ToolParam(description = "The new data of the rank category") RankCategoryInput.CreateUpdate input) {
        return RankCategoryOutput.from(rankCategoryService.updateRankCategory(id, input));
    }

    @Tool(description = "Deletes a rank category")
    public RankCategoryOutput deleteRankCategory(
        @ToolParam(description = ID) UUID id) {
        return RankCategoryOutput.from(rankCategoryService.deleteRankCategory(id));
    }
}
