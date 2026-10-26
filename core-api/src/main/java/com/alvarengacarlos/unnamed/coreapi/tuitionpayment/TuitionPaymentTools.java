package com.alvarengacarlos.unnamed.coreapi.tuitionpayment;

import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.alvarengacarlos.unnamed.coreapi.assistant.AssistantTools;
import com.alvarengacarlos.unnamed.coreapi.assistant.ToolParams;
import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;

/**
 * The operations on the tuition payments that the assistant may call. A tuition payment is the payment of the tuition
 * of one month of a student enrollment.
 */
@Component
public class TuitionPaymentTools implements AssistantTools {
    private static final String ID = "Id of the tuition payment";
    private final TuitionPaymentService tuitionPaymentService;

    public TuitionPaymentTools(TuitionPaymentService tuitionPaymentService) {
        this.tuitionPaymentService = tuitionPaymentService;
    }

    @Tool(description = "Creates a tuition payment")
    public TuitionPaymentOutput createTuitionPayment(
        @ToolParam(description = "The tuition payment to create") TuitionPaymentInput.Create input) {
        return TuitionPaymentOutput.from(tuitionPaymentService.createTuitionPayment(input));
    }

    @Tool(description = "Lists the tuition payments, one page at a time")
    public CommonOutput.PaginationOutput<TuitionPaymentOutput> getTuitionPayments(
        @ToolParam(description = ToolParams.PAGE, required = false) Integer page,
        @ToolParam(description = ToolParams.PAGE_SIZE, required = false) Integer pageSize,
        @ToolParam(description = "Filters of the tuition payments")
            TuitionPaymentInput.GetTuitionPayments filterInput) {
        final CommonInput.PaginationInput paginationInput = new CommonInput.PaginationInput(page, pageSize);
        return CommonOutput.PaginationOutput.from(
            tuitionPaymentService.getTuitionPayments(paginationInput, filterInput),
            TuitionPaymentOutput::from);
    }

    @Tool(description = "Gets a tuition payment by its id")
    public TuitionPaymentOutput getTuitionPayment(
        @ToolParam(description = ID) UUID id) {
        return TuitionPaymentOutput.from(tuitionPaymentService.getTuitionPayment(id));
    }

    @Tool(description = "Updates a tuition payment, replacing all of its data")
    public TuitionPaymentOutput updateTuitionPayment(
        @ToolParam(description = ID) UUID id,
        @ToolParam(description = "The new data of the tuition payment") TuitionPaymentInput.Update input) {
        return TuitionPaymentOutput.from(tuitionPaymentService.updateTuitionPayment(id, input));
    }

    @Tool(description = "Deletes a tuition payment")
    public TuitionPaymentOutput deleteTuitionPayment(
        @ToolParam(description = ID) UUID id) {
        return TuitionPaymentOutput.from(tuitionPaymentService.deleteTuitionPayment(id));
    }
}
