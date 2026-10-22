import { notifications } from "@mantine/notifications"

// Confirms to the user that an action, such as saving or deleting, was done
export function showSuccessNotification(message: string): void {
    notifications.show({
        color: "green",
        title: "Pronto",
        message
    })
}
