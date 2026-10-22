import { createContext, useContext } from "react"
import type { Session } from "./session.type"

export const SessionContext = createContext<Session | null>(null)

export function useSession(): Session {
    const session = useContext(SessionContext)
    if (session === null) {
        throw new Error("useSession must be used inside SessionContext")
    }

    return session
}
