package com.alvarengacarlos.unnamed.coreapi.member;

import java.util.UUID;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;
import com.alvarengacarlos.unnamed.coreapi.common.Locations;

@Tag(name = "Member")
@RestController
@RequestMapping("members")
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    public ResponseEntity<MemberOutput> createMember(
        @Validated @RequestBody MemberInput.CreateUpdate input) {
        final MemberOutput memberOutput = MemberOutput.from(memberService.createMember(input));
        return ResponseEntity.created(Locations.of(memberOutput.id()))
            .body(memberOutput);
    }

    @GetMapping
    public ResponseEntity<CommonOutput.PaginationOutput<MemberOutput>> getMembers(
        @Validated CommonInput.PaginationInput paginationInput,
        @Validated MemberInput.GetMembers filterInput) {
        return ResponseEntity.ok(CommonOutput.PaginationOutput.from(
            memberService.getMembers(paginationInput, filterInput),
            MemberOutput::from));
    }

    @GetMapping("{id}")
    public ResponseEntity<MemberOutput> getMember(
        @PathVariable UUID id) {
        final MemberOutput memberOutput = MemberOutput.from(memberService.getMember(id));
        return ResponseEntity.ok(memberOutput);
    }

    @PutMapping("{id}")
    public ResponseEntity<MemberOutput> updateMember(
        @PathVariable UUID id, 
        @Validated @RequestBody MemberInput.CreateUpdate input) {
        final MemberOutput memberOutput = MemberOutput.from(memberService.updateMember(id, input));
        return ResponseEntity.ok(memberOutput);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<MemberOutput> deleteMember(
        @PathVariable UUID id) {
        final MemberOutput memberOutput = MemberOutput.from(memberService.deleteMember(id));
        return ResponseEntity.ok(memberOutput);
    }
}
