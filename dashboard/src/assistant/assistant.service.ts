import { RequestClient } from "../common/common.service"
import type { AssistantOutput, Exchange } from "./assistant.type"

// The earlier exchanges sent with each message, the last ones; the core-api keeps no conversation and uses as many
export const MAX_HISTORY = 10

export class AssistantService {
    constructor(private readonly requestClient: RequestClient) {}

    // The reply to the message, given the earlier exchanges of the conversation (oldest first)
    async chat(message: string, history: Array<Exchange>): Promise<string> {
        console.info("Sending a message to the assistant")
        const output = await this.requestClient.post<ChatInput, AssistantOutput>("/assistant", {
            message,
            history: history.slice(-MAX_HISTORY)
        })
        console.info("Success")
        return output.reply
    }
}

interface ChatInput {
    readonly message: string
    readonly history: Array<Exchange>
}
