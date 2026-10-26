package com.alvarengacarlos.unnamed.coreapi.tuition;

import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.alvarengacarlos.unnamed.coreapi.assistant.AssistantTools;
import com.alvarengacarlos.unnamed.coreapi.assistant.ToolParams;
import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;

/**
 * The operations on the tuitions that the assistant may call. A tuition is a monthly fee plan that student enrollments
 * are charged.
 */
@Component
public class TuitionTools implements AssistantTools {
    private static final String ID = "Id of the tuition";
    private final TuitionService tuitionService;

    public TuitionTools(TuitionService tuitionService) {
        this.tuitionService = tuitionService;
    }

    @Tool(description = "Creates a tuition")
    public TuitionOutput createTuition(
        @ToolParam(description = "The tuition to create") TuitionInput.CreateUpdate input) {
        return TuitionOutput.from(tuitionService.createTuition(input));
    }

    @Tool(description = "Lists the tuitions, one page at a time")
    public CommonOutput.PaginationOutput<TuitionOutput> getTuitions(
        @ToolParam(description = ToolParams.PAGE, required = false) Integer page,
        @ToolParam(description = ToolParams.PAGE_SIZE, required = false) Integer pageSize) {
        final CommonInput.PaginationInput paginationInput = new CommonInput.PaginationInput(page, pageSize);
        return CommonOutput.PaginationOutput.from(
            tuitionService.getTuitions(paginationInput),
            TuitionOutput::from);
    }

    @Tool(description = "Gets a tuition by its id")
    public TuitionOutput getTuition(
        @ToolParam(description = ID) UUID id) {
        return TuitionOutput.from(tuitionService.getTuition(id));
    }

    @Tool(description = "Updates a tuition, replacing all of its data")
    public TuitionOutput updateTuition(
        @ToolParam(description = ID) UUID id,
        @ToolParam(description = "The new data of the tuition") TuitionInput.CreateUpdate input) {
        return TuitionOutput.from(tuitionService.updateTuition(id, input));
    }

    @Tool(description = "Deletes a tuition")
    public TuitionOutput deleteTuition(
        @ToolParam(description = ID) UUID id) {
        return TuitionOutput.from(tuitionService.deleteTuition(id));
    }
}
