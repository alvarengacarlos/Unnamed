package com.alvarengacarlos.unnamed.coreapi.clazz;

import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.alvarengacarlos.unnamed.coreapi.assistant.AssistantTools;
import com.alvarengacarlos.unnamed.coreapi.assistant.ToolParams;
import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;

/**
 * The operations on the classes that the assistant may call. A class is a recurring martial arts class of the
 * organization, taught by a teacher.
 */
@Component
public class ClazzTools implements AssistantTools {
    private static final String ID = "Id of the class";
    private final ClazzService clazzService;

    public ClazzTools(ClazzService clazzService) {
        this.clazzService = clazzService;
    }

    @Tool(description = "Creates a class")
    public ClazzOutput createClazz(
        @ToolParam(description = "The class to create") ClazzInput.CreateUpdate input) {
        return ClazzOutput.from(clazzService.createClazz(input));
    }

    @Tool(description = "Lists the classes, one page at a time")
    public CommonOutput.PaginationOutput<ClazzOutput> getClazzes(
        @ToolParam(description = ToolParams.PAGE, required = false) Integer page,
        @ToolParam(description = ToolParams.PAGE_SIZE, required = false) Integer pageSize,
        @ToolParam(description = "Filters of the classes") ClazzInput.GetClazzes filterInput) {
        final CommonInput.PaginationInput paginationInput = new CommonInput.PaginationInput(page, pageSize);
        return CommonOutput.PaginationOutput.from(
            clazzService.getClazzes(paginationInput, filterInput),
            ClazzOutput::from);
    }

    @Tool(description = "Gets a class by its id")
    public ClazzOutput getClazz(
        @ToolParam(description = ID) UUID id) {
        return ClazzOutput.from(clazzService.getClazz(id));
    }

    @Tool(description = "Updates a class, replacing all of its data")
    public ClazzOutput updateClazz(
        @ToolParam(description = ID) UUID id,
        @ToolParam(description = "The new data of the class") ClazzInput.CreateUpdate input) {
        return ClazzOutput.from(clazzService.updateClazz(id, input));
    }

    @Tool(description = "Deletes a class")
    public ClazzOutput deleteClazz(
        @ToolParam(description = ID) UUID id) {
        return ClazzOutput.from(clazzService.deleteClazz(id));
    }
}
