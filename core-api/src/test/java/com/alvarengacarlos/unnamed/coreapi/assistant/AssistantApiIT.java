package com.alvarengacarlos.unnamed.coreapi.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.alvarengacarlos.unnamed.coreapi.testsupport.ApiClient;
import com.alvarengacarlos.unnamed.coreapi.testsupport.E2ETest;

/**
 * The model is a mock: these tests check the endpoint and the tools offered to the model, not its answers.
 */
@E2ETest
public class AssistantApiIT {
    private static final String PATH = "/assistant";
    private static final String MESSAGE = "Quantos alunos temos?";

    @Autowired
    private ApiClient api;

    @Autowired
    private List<AssistantTools> tools;

    @MockitoBean
    private ChatModel chatModel;

    @Test
    void shouldAnswerTheMessageWithTheModel() {
        final String answer = "Vocês têm 3 alunos.";
        when(chatModel.call(any(Prompt.class)))
            .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(answer)))));

        api.client(UUID.randomUUID()).post()
            .uri(PATH)
            .body(new AssistantInput.Chat(MESSAGE, List.of()))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AssistantOutput.class)
            .isEqualTo(new AssistantOutput(answer));

        final ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        assertThat(prompt.getValue().getUserMessage().getText()).isEqualTo(MESSAGE);
    }

    @Test
    void shouldSendTheLastExchangesBeforeTheMessage() {
        when(chatModel.call(any(Prompt.class)))
            .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Ok")))));
        final int exchanges = AssistantService.MAX_HISTORY + 2;
        final List<AssistantInput.Exchange> history = IntStream.range(0, exchanges)
            .mapToObj(i -> new AssistantInput.Exchange("message " + i, "reply " + i))
            .toList();

        api.client(UUID.randomUUID()).post()
            .uri(PATH)
            .body(new AssistantInput.Chat(MESSAGE, history))
            .exchange()
            .expectStatus()
            .isOk();

        final ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        final List<Message> messages = prompt.getValue().getInstructions();
        // The instructions, the last exchanges (the 2 oldest are left out), oldest first, and the message
        assertThat(messages.getFirst().getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(messages.subList(1, messages.size()))
            .extracting(Message::getMessageType, Message::getText)
            .startsWith(tuple(MessageType.USER, "message 2"), tuple(MessageType.ASSISTANT, "reply 2"))
            .endsWith(tuple(MessageType.ASSISTANT, "reply " + (exchanges - 1)), tuple(MessageType.USER, MESSAGE))
            .hasSize(AssistantService.MAX_HISTORY * 2 + 1);
    }

    @Test
    void shouldReturnUnauthorizedWithoutAToken() {
        api.client().post()
            .uri(PATH)
            .body(new AssistantInput.Chat(MESSAGE, List.of()))
            .exchange()
            .expectStatus()
            .isUnauthorized();
    }

    // Building the callbacks generates the JSON schema of the parameters of every tool and fails on repeated names.
    @Test
    void shouldOfferTheToolsOfEveryFeature() {
        final List<String> names = Arrays.stream(ToolCallbacks.from(tools.toArray()))
            .map(toolCallback -> toolCallback.getToolDefinition().name())
            .toList();

        assertThat(tools).hasSize(14);
        assertThat(names)
            .doesNotHaveDuplicates()
            .contains("createMember", "getStudents", "createTuitionPayment", "getPossibleCompetitors");
    }
}
