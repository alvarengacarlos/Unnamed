package com.alvarengacarlos.unnamed.coreapi.teacher;

import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.alvarengacarlos.unnamed.coreapi.assistant.AssistantTools;
import com.alvarengacarlos.unnamed.coreapi.assistant.ToolParams;
import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;

/**
 * The operations on the teachers that the assistant may call. A teacher is a member who teaches classes.
 */
@Component
public class TeacherTools implements AssistantTools {
    private static final String ID = "Id of the teacher";
    private final TeacherService teacherService;

    public TeacherTools(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @Tool(description = "Creates a teacher")
    public TeacherOutput createTeacher(
        @ToolParam(description = "The teacher to create") TeacherInput.Create input) {
        return TeacherOutput.from(teacherService.createTeacher(input));
    }

    @Tool(description = "Lists the teachers, one page at a time")
    public CommonOutput.PaginationOutput<TeacherOutput> getTeachers(
        @ToolParam(description = ToolParams.PAGE, required = false) Integer page,
        @ToolParam(description = ToolParams.PAGE_SIZE, required = false) Integer pageSize,
        @ToolParam(description = "Filters of the teachers") TeacherInput.GetTeachers filterInput) {
        final CommonInput.PaginationInput paginationInput = new CommonInput.PaginationInput(page, pageSize);
        return CommonOutput.PaginationOutput.from(
            teacherService.getTeachers(paginationInput, filterInput),
            TeacherOutput::from);
    }

    @Tool(description = "Gets a teacher by its id")
    public TeacherOutput getTeacher(
        @ToolParam(description = ID) UUID id) {
        return TeacherOutput.from(teacherService.getTeacher(id));
    }

    @Tool(description = "Deletes a teacher")
    public TeacherOutput deleteTeacher(
        @ToolParam(description = ID) UUID id) {
        return TeacherOutput.from(teacherService.deleteTeacher(id));
    }
}
