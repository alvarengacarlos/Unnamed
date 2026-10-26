package com.alvarengacarlos.unnamed.coreapi.gradingcandidate;

import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.alvarengacarlos.unnamed.coreapi.assistant.AssistantTools;
import com.alvarengacarlos.unnamed.coreapi.assistant.ToolParams;
import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;

/**
 * The operations on the grading candidates that the assistant may call. A grading candidate is a member registered in a
 * grading to be promoted to a rank.
 */
@Component
public class GradingCandidateTools implements AssistantTools {
    private static final String ID = "Id of the grading candidate";
    private final GradingCandidateService gradingCandidateService;

    public GradingCandidateTools(GradingCandidateService gradingCandidateService) {
        this.gradingCandidateService = gradingCandidateService;
    }

    @Tool(description = "Creates a grading candidate")
    public GradingCandidateOutput createGradingCandidate(
        @ToolParam(description = "The grading candidate to create") GradingCandidateInput.Create input) {
        return GradingCandidateOutput.from(gradingCandidateService.createGradingCandidate(input));
    }

    @Tool(description = "Lists the grading candidates, one page at a time")
    public CommonOutput.PaginationOutput<GradingCandidateOutput> getGradingCandidates(
        @ToolParam(description = ToolParams.PAGE, required = false) Integer page,
        @ToolParam(description = ToolParams.PAGE_SIZE, required = false) Integer pageSize,
        @ToolParam(description = "Filters of the grading candidates")
            GradingCandidateInput.GetGradingCandidates filterInput) {
        final CommonInput.PaginationInput paginationInput = new CommonInput.PaginationInput(page, pageSize);
        return CommonOutput.PaginationOutput.from(
            gradingCandidateService.getGradingCandidates(paginationInput, filterInput),
            GradingCandidateOutput::from);
    }

    @Tool(description = "Gets a grading candidate by its id")
    public GradingCandidateOutput getGradingCandidate(
        @ToolParam(description = ID) UUID id) {
        return GradingCandidateOutput.from(gradingCandidateService.getGradingCandidate(id));
    }

    @Tool(description = "Updates a grading candidate, replacing all of its data")
    public GradingCandidateOutput updateGradingCandidate(
        @ToolParam(description = ID) UUID id,
        @ToolParam(description = "The new data of the grading candidate") GradingCandidateInput.Update input) {
        return GradingCandidateOutput.from(gradingCandidateService.updateGradingCandidate(id, input));
    }

    @Tool(description = "Deletes a grading candidate")
    public GradingCandidateOutput deleteGradingCandidate(
        @ToolParam(description = ID) UUID id) {
        return GradingCandidateOutput.from(gradingCandidateService.deleteGradingCandidate(id));
    }
}
