import { useEffect, useRef, useState } from "react"
import { convertErrorToView } from "../common.converter"

/**
 * The state of an action of a screen, such as saving or deleting: whether it is running and the error to show if it
 * failed. An action started while another one of the same hook is running is ignored, so a double click does not
 * repeat it.
 */
export function useRequest() {
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    // The state only changes in the next render, so it does not stop a second click made before it
    const isRunning = useRef(false)

    async function run(request: () => Promise<void>): Promise<void> {
        if (isRunning.current) {
            return
        }

        isRunning.current = true
        setIsLoading(true)
        setError(null)
        try {
            await request()
        } catch (e) {
            console.error("Request failed. Error: " + e)
            setError(convertErrorToView(e))
        } finally {
            isRunning.current = false
            setIsLoading(false)
        }
    }

    function clearError(): void {
        setError(null)
    }

    return { isLoading, error, run, clearError }
}

/**
 * The state of a load of data, such as a list or a search. Unlike an action, a new load is never ignored: it replaces
 * the running one, and only the result of the last load is applied, so a slow answer of an old load does not
 * overwrite a newer one.
 */
export function useLoadRequest() {
    const [isLoading, setIsLoading] = useState(false)
    // Whether a load has finished once: the first load shows a Loader, the next ones keep the data on the screen
    const [hasLoaded, setHasLoaded] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const lastLoadId = useRef(0)

    async function run<T>(load: () => Promise<T>, apply: (result: T) => void): Promise<void> {
        const loadId = ++lastLoadId.current
        setIsLoading(true)
        setError(null)
        try {
            const result = await load()
            if (loadId === lastLoadId.current) {
                apply(result)
            }
        } catch (e) {
            console.error("Load failed. Error: " + e)
            if (loadId === lastLoadId.current) {
                setError(convertErrorToView(e))
            }
        } finally {
            if (loadId === lastLoadId.current) {
                setIsLoading(false)
                setHasLoaded(true)
            }
        }
    }

    function clearError(): void {
        setError(null)
    }

    return { isLoading, hasLoaded, error, run, clearError }
}

// Runs the callback once, when the component mounts, as the loads of the data of a screen
export function useOnMount(callback: () => void): void {
    const callbackRef = useRef(callback)

    useEffect(() => {
        callbackRef.current()
    }, [])
}
