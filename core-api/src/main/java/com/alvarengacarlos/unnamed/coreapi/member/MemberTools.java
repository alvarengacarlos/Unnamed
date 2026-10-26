package com.alvarengacarlos.unnamed.coreapi.member;

import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.alvarengacarlos.unnamed.coreapi.assistant.AssistantTools;
import com.alvarengacarlos.unnamed.coreapi.assistant.ToolParams;
import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;

/**
 * The operations on the members that the assistant may call. A member is a person of the organization, with personal
 * data and a rank; students and teachers are members.
 */
@Component
public class MemberTools implements AssistantTools {
    private static final String ID = "Id of the member";
    private final MemberService memberService;

    public MemberTools(MemberService memberService) {
        this.memberService = memberService;
    }

    @Tool(description = "Creates a member")
    public MemberOutput createMember(
        @ToolParam(description = "The member to create") MemberInput.CreateUpdate input) {
        return MemberOutput.from(memberService.createMember(input));
    }

    @Tool(description = "Lists the members, one page at a time")
    public CommonOutput.PaginationOutput<MemberOutput> getMembers(
        @ToolParam(description = ToolParams.PAGE, required = false) Integer page,
        @ToolParam(description = ToolParams.PAGE_SIZE, required = false) Integer pageSize,
        @ToolParam(description = "Filters of the members") MemberInput.GetMembers filterInput) {
        final CommonInput.PaginationInput paginationInput = new CommonInput.PaginationInput(page, pageSize);
        return CommonOutput.PaginationOutput.from(
            memberService.getMembers(paginationInput, filterInput),
            MemberOutput::from);
    }

    @Tool(description = "Gets a member by its id")
    public MemberOutput getMember(
        @ToolParam(description = ID) UUID id) {
        return MemberOutput.from(memberService.getMember(id));
    }

    @Tool(description = "Updates a member, replacing all of its data")
    public MemberOutput updateMember(
        @ToolParam(description = ID) UUID id,
        @ToolParam(description = "The new data of the member") MemberInput.CreateUpdate input) {
        return MemberOutput.from(memberService.updateMember(id, input));
    }

    @Tool(description = "Deletes a member")
    public MemberOutput deleteMember(
        @ToolParam(description = ID) UUID id) {
        return MemberOutput.from(memberService.deleteMember(id));
    }
}
