import { Month } from "../common/common.type";
import { convertServiceToMonth } from "../common/common.converter";
import { StudentEnrollment, type StudentEnrollmentOutput } from "../student-enrollment/student-enrollment.type";

export class TuitionPayment {
    constructor(
        public readonly id: string,
        public readonly month: Month,
        public readonly year: number,
        public readonly isPaid: boolean,
        public readonly createdAt: Date,
        public readonly updatedAt: Date,
        public readonly studentEnrollment: StudentEnrollment,
        public readonly organizationId: string
    ) {}

    static from(tuitionPayment: TuitionPaymentOutput): TuitionPayment {
        return new TuitionPayment(
            tuitionPayment.id,
            convertServiceToMonth(tuitionPayment.month),
            tuitionPayment.year,
            tuitionPayment.isPaid,
            new Date(tuitionPayment.createdAt),
            new Date(tuitionPayment.updatedAt),
            StudentEnrollment.from(tuitionPayment.studentEnrollment),
            tuitionPayment.organizationId
        )
    }
}

export interface TuitionPaymentOutput {
    readonly id: string
    // The number of the month, January being 1
    readonly month: number
    readonly year: number
    readonly isPaid: boolean
    readonly createdAt: string
    readonly updatedAt: string
    readonly studentEnrollment: StudentEnrollmentOutput
    readonly organizationId: string
}
