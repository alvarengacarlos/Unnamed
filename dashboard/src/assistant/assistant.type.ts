export interface AssistantOutput {
    // Markdown (GitHub Flavored), in the language of the message
    readonly reply: string
}

// A message of the user and the reply of the assistant to it
export interface Exchange {
    readonly message: string
    readonly reply: string
}
