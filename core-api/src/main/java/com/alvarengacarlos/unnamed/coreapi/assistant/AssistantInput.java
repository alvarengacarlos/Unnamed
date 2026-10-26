package com.alvarengacarlos.unnamed.coreapi.assistant;

import java.util.List;

public record AssistantInput() {
    /**
     * The message of the user and the earlier exchanges of the conversation, oldest first. The server keeps no
     * conversation: the client sends it with each message.
     */
    public static record Chat(String message, List<Exchange> history) {
        public Chat {
            if (history == null) {
                history = List.of();
            }
        }
    }

    public static record Exchange(String message, String reply) {
    }
}
