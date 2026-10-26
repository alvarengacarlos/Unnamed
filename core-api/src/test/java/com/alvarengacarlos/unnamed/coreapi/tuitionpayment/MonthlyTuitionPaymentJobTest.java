package com.alvarengacarlos.unnamed.coreapi.tuitionpayment;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

public class MonthlyTuitionPaymentJobTest {
    private static final String CRON = "0 0 0 1 * *";
    private final TuitionPaymentService tuitionPaymentService = mock(TuitionPaymentService.class);

    @Test
    void shouldAddThePaymentsOfTheCurrentMonthOfTheServerOnStartup() {
        new MonthlyTuitionPaymentJob(tuitionPaymentService, CRON).runOnStartup();

        verify(tuitionPaymentService).createMonthlyTuitionPayments(YearMonth.now());
    }

    @Test
    void shouldNotRunOnStartupWhenTheJobIsTurnedOff() {
        new MonthlyTuitionPaymentJob(tuitionPaymentService, Scheduled.CRON_DISABLED).runOnStartup();

        verify(tuitionPaymentService, never()).createMonthlyTuitionPayments(any());
    }

    @Test
    void shouldNotStopTheStartupWhenItFails() {
        when(tuitionPaymentService.createMonthlyTuitionPayments(any())).thenThrow(new IllegalStateException("db down"));
        final MonthlyTuitionPaymentJob job = new MonthlyTuitionPaymentJob(tuitionPaymentService, CRON);

        assertThatNoException().isThrownBy(job::runOnStartup);
    }
}
