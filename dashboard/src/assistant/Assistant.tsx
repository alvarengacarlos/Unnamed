import { Button, Group, Loader, Paper, Stack, Text, Textarea, Title } from "@mantine/core"
import { PlusIcon } from "@phosphor-icons/react"
import { useState, type KeyboardEvent } from "react"
import { assistantService } from "../instance"
import { MAX_HISTORY } from "./assistant.service"
import { useRequest } from "../common/hook/useRequest.hook"
import RequestErrorAlert from "../common/component/RequestErrorAlert.component"
import Markdown from "../common/component/Markdown.component"
import type { Exchange } from "./assistant.type"

export default function Assistant() {
    const [message, setMessage] = useState("")
    // The conversation lives only on this screen: the last exchanges are sent with each message
    const [exchanges, setExchanges] = useState<Array<Exchange>>([])
    // The message being answered, shown before its reply arrives
    const [pendingMessage, setPendingMessage] = useState<string | null>(null)

    const sendMessageRequest = useRequest()

    function sendMessage() {
        const text = message.trim()
        if (!text) {
            return
        }

        sendMessageRequest.run(async () => {
            setPendingMessage(text)
            try {
                const reply = await assistantService.chat(text, exchanges)
                setExchanges(previous => [...previous, { message: text, reply }])
                setMessage("")
            } finally {
                setPendingMessage(null)
            }
        })
    }

    // The earlier exchanges are no longer sent; what is typed in the field stays, it may be the new subject
    function startNewConversation() {
        setExchanges([])
        sendMessageRequest.clearError()
    }

    // Enter sends, Shift+Enter breaks the line, as in the chats; not while an accent or a character is being composed
    function onKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
        if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing) {
            event.preventDefault()
            sendMessage()
        }
    }

    return (
        <>
            <Group justify="space-between">
                <Title order={2}>Assistente</Title>
                {/* Disabled while a reply is on its way, which would come back into the new conversation */}
                <Button
                    variant="default"
                    leftSection={<PlusIcon/>}
                    disabled={exchanges.length === 0 || sendMessageRequest.isLoading}
                    onClick={startNewConversation}
                >
                    Nova conversa
                </Button>
            </Group>
            <Text size="sm" c="dimmed">
                O assistente lembra das últimas {MAX_HISTORY} mensagens e respostas desta conversa. Para mudar de
                assunto, comece uma nova conversa. Ao sair da página ou recarregá-la, a conversa também recomeça.
            </Text>

            <Stack role="log" aria-label="Conversa" gap="sm" my="md">
                {exchanges.map((exchange, index) => (
                    <Stack key={index} gap="sm">
                        <UserMessage text={exchange.message}/>
                        <Paper role="article" aria-label="Assistente" withBorder p="sm" radius="md">
                            <Markdown>{exchange.reply}</Markdown>
                        </Paper>
                    </Stack>
                ))}
                {pendingMessage && (
                    <>
                        <UserMessage text={pendingMessage}/>
                        <Group>
                            <Loader size="sm"/>
                            <Text size="sm" c="dimmed">Pensando...</Text>
                        </Group>
                    </>
                )}
            </Stack>

            <RequestErrorAlert error={sendMessageRequest.error} onClose={sendMessageRequest.clearError}/>

            <form onSubmit={event => {
                event.preventDefault()
                sendMessage()
            }}>
                <Textarea
                    label="Mensagem"
                    placeholder="Quantos alunos temos?"
                    autosize
                    minRows={2}
                    maxRows={8}
                    value={message}
                    onChange={event => setMessage(event.currentTarget.value)}
                    onKeyDown={onKeyDown}
                />
                <Group justify="flex-end" mt="sm">
                    <Button
                        variant="filled"
                        type="submit"
                        loading={sendMessageRequest.isLoading}
                        disabled={!message.trim()}
                    >
                        Enviar
                    </Button>
                </Group>
            </form>
        </>
    )
}

function UserMessage(props: { text: string }) {
    return (
        <Group justify="flex-end">
            <Paper role="article" aria-label="Você" bg="var(--mantine-color-blue-light)" p="sm" radius="md" maw="80%">
                <Text style={{ whiteSpace: "pre-wrap" }}>{props.text}</Text>
            </Paper>
        </Group>
    )
}
