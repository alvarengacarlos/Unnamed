// @vitest-environment jsdom
import { act, renderHook } from "@testing-library/react"
import { describe, expect, it, vi } from "vitest"
import { HttpError } from "../common.service"
import { useLoadRequest, useRequest } from "./useRequest.hook"

// A promise resolved or rejected by the test, to control the order the requests finish in
function deferred<T>() {
    let resolve!: (value: T) => void
    let reject!: (reason: unknown) => void
    const promise = new Promise<T>((res, rej) => {
        resolve = res
        reject = rej
    })
    return { promise, resolve, reject }
}

describe("useRequest", () => {
    it("ignores an action started while another one is running, as a double click", async () => {
        const { result } = renderHook(() => useRequest())
        const first = deferred<void>()
        const request = vi.fn(() => first.promise)

        let firstRun!: Promise<void>
        act(() => {
            firstRun = result.current.run(request)
            void result.current.run(request)
        })
        expect(result.current.isLoading).toBe(true)

        await act(async () => {
            first.resolve()
            await firstRun
        })

        expect(request).toHaveBeenCalledTimes(1)
        expect(result.current.isLoading).toBe(false)
    })

    it("shows the error of a failed action and runs the next one", async () => {
        const { result } = renderHook(() => useRequest())

        await act(() => result.current.run(async () => {
            throw new HttpError(409, { type: "urn:unnamed:problem:member-exists" })
        }))

        expect(result.current.error).toBe("Já existe um membro com este CPF.")
        expect(result.current.isLoading).toBe(false)

        const request = vi.fn(async () => {})
        await act(() => result.current.run(request))

        expect(request).toHaveBeenCalledTimes(1)
        expect(result.current.error).toBeNull()
    })
})

describe("useLoadRequest", () => {
    it("applies only the result of the last load, even when an older one answers after it", async () => {
        const { result } = renderHook(() => useLoadRequest())
        const older = deferred<string>()
        const newer = deferred<string>()
        const apply = vi.fn()

        let olderRun!: Promise<void>
        let newerRun!: Promise<void>
        act(() => {
            olderRun = result.current.run(() => older.promise, apply)
            newerRun = result.current.run(() => newer.promise, apply)
        })

        await act(async () => {
            newer.resolve("newer")
            await newerRun
        })
        await act(async () => {
            older.resolve("older")
            await olderRun
        })

        expect(apply).toHaveBeenCalledExactlyOnceWith("newer")
        expect(result.current.isLoading).toBe(false)
        expect(result.current.hasLoaded).toBe(true)
    })

    it("ignores the error of an older load", async () => {
        const { result } = renderHook(() => useLoadRequest())
        const older = deferred<string>()
        const apply = vi.fn()

        let olderRun!: Promise<void>
        act(() => {
            olderRun = result.current.run(() => older.promise, apply)
        })
        await act(() => result.current.run(async () => "newer", apply))
        await act(async () => {
            older.reject(new TypeError("Failed to fetch"))
            await olderRun
        })

        expect(result.current.error).toBeNull()
        expect(apply).toHaveBeenCalledExactlyOnceWith("newer")
    })

    it("shows the error of the last load", async () => {
        const { result } = renderHook(() => useLoadRequest())

        await act(() => result.current.run(async () => {
            throw new TypeError("Failed to fetch")
        }, vi.fn()))

        expect(result.current.error)
            .toBe("Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.")
        expect(result.current.hasLoaded).toBe(true)
    })
})
