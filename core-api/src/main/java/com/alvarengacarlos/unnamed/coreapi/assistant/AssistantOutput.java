package com.alvarengacarlos.unnamed.coreapi.assistant;

import io.swagger.v3.oas.annotations.media.Schema;

public record AssistantOutput(
    @Schema(description = "The reply of the assistant in Markdown (GitHub Flavored), in the language of the message")
    String reply) {
}
