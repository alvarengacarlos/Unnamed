// Values that no other test, or run, uses: against the real backend the data of every run stays in the same database

let sequence = 0

export function uniqueName(prefix: string): string {
    sequence++
    return `${prefix} ${Date.now().toString(36)}${sequence.toString(36)}`.toUpperCase()
}

// A valid CPF (with the check digits), as 000.000.000-00
export function uniqueCpf(): string {
    const digits = Array.from({ length: 9 }, () => Math.floor(Math.random() * 10))
    digits.push(cpfCheckDigit(digits))
    digits.push(cpfCheckDigit(digits))
    const cpf = digits.join("")
    return `${cpf.slice(0, 3)}.${cpf.slice(3, 6)}.${cpf.slice(6, 9)}-${cpf.slice(9)}`
}

export function isValidCpf(cpf: string): boolean {
    const digits = cpf.replace(/\D/g, "").split("").map(Number)
    if (digits.length !== 11 || digits.every(d => d === digits[0])) {
        return false
    }

    return cpfCheckDigit(digits.slice(0, 9)) === digits[9] && cpfCheckDigit(digits.slice(0, 10)) === digits[10]
}

function cpfCheckDigit(digits: Array<number>): number {
    const sum = digits.reduce((total, digit, index) => total + digit * (digits.length + 1 - index), 0)
    const rest = (sum * 10) % 11
    return rest === 10 ? 0 : rest
}
