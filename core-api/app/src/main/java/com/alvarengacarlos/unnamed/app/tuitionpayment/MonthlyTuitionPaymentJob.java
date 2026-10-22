package com.alvarengacarlos.unnamed.app.tuitionpayment;

import java.time.YearMonth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * At the start of every month, adds the unpaid tuition payment of the month to each active enrollment.
 * The month is the one in the time zone of the server (America/Sao_Paulo, set by the Dockerfile).
 * It also runs when the API starts, so a month whose start the API missed (a deploy, a crash) is not left without its
 * payments. It is safe to run more than once, so more instances of the API may run it at the same time.
 */
@Component
public class MonthlyTuitionPaymentJob {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TuitionPaymentService tuitionPaymentService;
    private final boolean isEnabled;

    public MonthlyTuitionPaymentJob(
        TuitionPaymentService tuitionPaymentService,
        @Value("${unnamed.tuition-payments.monthly-job.cron}") String cron) {
        this.tuitionPaymentService = tuitionPaymentService;
        this.isEnabled = !Scheduled.CRON_DISABLED.equals(cron);
    }

    @Scheduled(cron = "${unnamed.tuition-payments.monthly-job.cron}")
    public void run() {
        tuitionPaymentService.createMonthlyTuitionPayments(YearMonth.now());
    }

    // A failure here does not stop the API: the next start or the next month tries again.
    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        if (!isEnabled) {
            return;
        }

        try {
            run();
        } catch (RuntimeException e) {
            logger.error("Cannot add the tuition payments of the month on startup", e);
        }
    }
}
