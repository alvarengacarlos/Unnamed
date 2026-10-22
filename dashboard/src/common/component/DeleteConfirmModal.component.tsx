import { Button, Group, Modal, Text } from "@mantine/core"
import type { ReactNode } from "react"
import RequestErrorAlert from "./RequestErrorAlert.component"

interface Props {
    opened: boolean
    onClose: () => void
    title: string
    children: ReactNode
    isLoading: boolean
    error: string | null
    onCloseError: () => void
    onConfirm: () => void
    confirmButtonTestId?: string
}

export default function DeleteConfirmModal(props: Props) {
    return (
        <Modal 
            opened={props.opened} 
            onClose={() => {
                props.onClose()
                props.onCloseError()
            }} 
            title={props.title} 
            centered
        >
            <RequestErrorAlert error={props.error} onClose={props.onCloseError}/>
            <Text>{props.children}</Text>
            <Group justify="flex-end" mt="md">
                <Button 
                    variant="default" 
                    disabled={props.isLoading}
                    onClick={() => {
                        props.onClose()
                        props.onCloseError()
                    }}
                >Cancelar</Button>
                <Button 
                    data-testid={props.confirmButtonTestId}
                    color="red" 
                    loading={props.isLoading} 
                    onClick={props.onConfirm}
                >Deletar</Button>
            </Group>
        </Modal>
    )
}
