import { ActionIcon, Tooltip } from "@mantine/core"
import type { ReactNode } from "react"

interface Props {
    label: string
    icon: ReactNode
    onClick: () => void
}

// A button with only an icon: the label is its tooltip and the name screen readers announce
export default function ActionButton(props: Props) {
    return (
        <Tooltip label={props.label}>
            <ActionIcon variant="filled" aria-label={props.label} onClick={props.onClick}>
                {props.icon}
            </ActionIcon>
        </Tooltip>
    )
}
