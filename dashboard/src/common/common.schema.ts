import z from "zod/v4"

// The texts typed by the users, such as a name or an address: the core-api accepts up to 200 characters
export const TEXT_MAX_LENGTH = 200

// A text the user must type, such as a name, within the limit of the core-api
export function requiredText(error: string) {
    return z.string({ error })
        .trim()
        .min(1, { error })
        .max(TEXT_MAX_LENGTH, { error: `Máximo de ${TEXT_MAX_LENGTH} caracteres` })
}
