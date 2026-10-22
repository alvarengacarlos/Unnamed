import { DataList, Modal } from "@mantine/core";
import type { LabelValue } from "../common.type";
import type { ReactNode } from "react";

interface Props {
    dataTestId?: string
    isViewObjectModalOpen: boolean
    closeViewObjectModal: () => void
    labelValue: Array<LabelValue>
    children?: ReactNode
}

export default function ViewObjectModal(props: Props) {
    const dataListItems = props.labelValue.map(lv => {
        return (
            <DataList.Item key={lv.label}>
                <DataList.ItemLabel>{lv.label}</DataList.ItemLabel>
                <DataList.ItemValue>{lv.value}</DataList.ItemValue>            
            </DataList.Item>
        )
    })

    return (
        <Modal
            centered
            data-testid={props.dataTestId}
            opened={props.isViewObjectModalOpen} 
            onClose={props.closeViewObjectModal} 
        >
            <DataList withDivider orientation="vertical">
                {dataListItems}
            </DataList>
            {props.children}
        </Modal>
    )
}
