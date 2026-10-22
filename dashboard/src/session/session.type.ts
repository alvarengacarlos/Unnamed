import type { IdTokenClaims } from "oidc-client-ts"

export class Session {
    constructor(
        public readonly username: string,
        public readonly name: string
    ) {}

    // Not every provider gives preferred_username or name, so it falls back to the e-mail and then to the subject
    static from(claims: IdTokenClaims): Session {
        const username = claims.preferred_username ?? claims.email ?? claims.sub
        return new Session(username, claims.name ?? username)
    }
}
