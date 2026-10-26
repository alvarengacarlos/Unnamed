package com.alvarengacarlos.unnamed.coreapi.grading;

import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.alvarengacarlos.unnamed.coreapi.assistant.AssistantTools;
import com.alvarengacarlos.unnamed.coreapi.assistant.ToolParams;
import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;

/**
 * The operations on the gradings that the assistant may call. A grading is an exam where members are promoted to a new
 * rank, with a registration period and a price per rank.
 */
@Component
public class GradingTools implements AssistantTools {
    private static final String ID = "Id of the grading";
    private final GradingService gradingService;

    public GradingTools(GradingService gradingService) {
        this.gradingService = gradingService;
    }

    @Tool(description = "Creates a grading")
    public GradingOutput createGrading(
        @ToolParam(description = "The grading to create") GradingInput.CreateUpdate input) {
        return GradingOutput.from(gradingService.createGrading(input));
    }

    @Tool(description = "Lists the gradings, one page at a time")
    public CommonOutput.PaginationOutput<GradingOutput> getGradings(
        @ToolParam(description = ToolParams.PAGE, required = false) Integer page,
        @ToolParam(description = ToolParams.PAGE_SIZE, required = false) Integer pageSize) {
        final CommonInput.PaginationInput paginationInput = new CommonInput.PaginationInput(page, pageSize);
        return CommonOutput.PaginationOutput.from(
            gradingService.getGradings(paginationInput),
            GradingOutput::from);
    }

    @Tool(description = "Gets a grading by its id")
    public GradingOutput getGrading(
        @ToolParam(description = ID) UUID id) {
        return GradingOutput.from(gradingService.getGrading(id));
    }

    @Tool(description = "Updates a grading, replacing all of its data")
    public GradingOutput updateGrading(
        @ToolParam(description = ID) UUID id,
        @ToolParam(description = "The new data of the grading") GradingInput.CreateUpdate input) {
        return GradingOutput.from(gradingService.updateGrading(id, input));
    }

    @Tool(description = "Deletes a grading")
    public GradingOutput deleteGrading(
        @ToolParam(description = ID) UUID id) {
        return GradingOutput.from(gradingService.deleteGrading(id));
    }
}
