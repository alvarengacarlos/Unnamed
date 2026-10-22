import { Box, Center, Loader, LoadingOverlay } from "@mantine/core"
import type { ReactNode } from "react"

interface Props {
    isLoading: boolean
    hasLoaded: boolean
    children: ReactNode
}

/**
 * The first load shows a Loader in place of the content. The next ones keep the content, with an overlay on top, so
 * the screen does not blink nor lose its scroll.
 */
export default function RequestLoader(props: Props) {
    if (!props.hasLoaded && props.isLoading) {
        return (
            <Center my="md">
                <Loader/>
            </Center>
        )
    }

    return (
        <Box pos="relative">
            <LoadingOverlay visible={props.isLoading} overlayProps={{ blur: 1 }}/>
            {props.children}
        </Box>
    )
}
