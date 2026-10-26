package com.alvarengacarlos.unnamed.coreapi.assistant;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Assistant")
@RestController
@RequestMapping("assistant")
public class AssistantController {
    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping
    public ResponseEntity<AssistantOutput> chat(@RequestBody AssistantInput.Chat input) {
        return ResponseEntity.ok(new AssistantOutput(assistantService.chat(input.message(), input.history())));
    }
}
