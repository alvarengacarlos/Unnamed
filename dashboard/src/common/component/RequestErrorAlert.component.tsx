import { Alert } from "@mantine/core"
import { WarningCircleIcon } from "@phosphor-icons/react"

interface Props {
    error: string | null
    onClose: () => void
}

export default function RequestErrorAlert(props: Props) {
    if (!props.error) {
        return null
    }

    return (
        <Alert
            variant="light"
            color="red"
            title="Erro"
            icon={<WarningCircleIcon/>}
            withCloseButton
            closeButtonLabel="Fechar"
            onClose={props.onClose}
            my="sm"
        >
            {props.error}
        </Alert>
    )
}
