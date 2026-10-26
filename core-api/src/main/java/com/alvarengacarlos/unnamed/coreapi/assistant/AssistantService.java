package com.alvarengacarlos.unnamed.coreapi.assistant;

import java.time.LocalDate;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

/**
 * Answers a message of the user with the model, which calls the tools of the features to read and change the data
 * of the organization. The tools run in the thread of the request, so they see its organization like the
 * controllers do. The server keeps no conversation: the client sends the earlier exchanges with each message.
 */
@Service
public class AssistantService {
    // The earlier exchanges sent to the model, the last ones: each one costs tokens in every message after it
    public static final int MAX_HISTORY = 10;
    private static final String SYSTEM_PROMPT =
        """
        You are the assistant of a management system for martial arts organizations. You help the staff of one
        organization with its members, students, teachers, classes, ranks, gradings, tuitions and tournaments.

        - Read and change the data only through the tools. They act on the organization of the user.
        - Never make up ids. Find them with the tools that list or get, e.g. a member by name.
        - The earlier messages of the conversation come before the last one, without the results of the tools: look
          the ids up again when you need them.
        - If an operation needs data the user did not send, do not call it: ask for everything that is missing at
          once.
        - Always reply in the same language as the message of the user, even though these instructions and the
          results of the tools are in English.
        - Always format the reply in Markdown (GitHub Flavored), e.g. lists and tables for data. Do not use links,
          images or raw HTML: they are not shown.
        - Reply briefly. Do not show ids unless asked.

        Today is %s.
        """;
    private final ChatClient chatClient;

    public AssistantService(ChatClient.Builder chatClientBuilder, List<AssistantTools> tools) {
        this.chatClient = chatClientBuilder
            .defaultTools(tools.toArray())
            .build();
    }

    public String chat(String message, List<AssistantInput.Exchange> history) {
        return chatClient.prompt()
            .system(SYSTEM_PROMPT.formatted(LocalDate.now()))
            .messages(toMessages(history))
            .user(message)
            .call()
            .content();
    }

    // The last exchanges, each one as the message of the user and the reply of the assistant
    private static List<Message> toMessages(List<AssistantInput.Exchange> history) {
        return history.subList(Math.max(history.size() - MAX_HISTORY, 0), history.size())
            .stream()
            .<Message>mapMulti((exchange, messages) -> {
                messages.accept(new UserMessage(exchange.message()));
                messages.accept(new AssistantMessage(exchange.reply()));
            })
            .toList();
    }
}
