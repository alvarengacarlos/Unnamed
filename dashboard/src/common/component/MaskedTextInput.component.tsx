import { InputBase, type InputBaseProps } from "@mantine/core"
import { IMaskInput } from "react-imask"

// The props of getInputProps of a form, which give the value and take the changes
interface Props extends InputBaseProps {
    mask: string | Array<{ mask: string }>
    placeholder?: string
    defaultValue?: string
    onChange?: (value: string) => void
    onFocus?: () => void
    onBlur?: () => void
}

/**
 * A text input that formats what is typed, such as a CPF or a phone. The form receives the formatted value, as the
 * API expects it.
 */
export default function MaskedTextInput({ mask, defaultValue, onChange, ...props }: Props) {
    return (
        <InputBase
            {...props}
            component={IMaskInput}
            mask={mask}
            defaultValue={defaultValue}
            onAccept={(value: string) => onChange?.(value)}
        />
    )
}
