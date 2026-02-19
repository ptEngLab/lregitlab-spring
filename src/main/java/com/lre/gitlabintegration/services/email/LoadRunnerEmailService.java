package com.lre.gitlabintegration.services.email;

import com.yourcompany.loadrunner.model.EmailTemplateContext;
import com.yourcompany.loadrunner.model.EmailTemplateContext.*;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoadRunnerEmailService {

    private static final int TOP_N = 10;

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendReport(EmailTemplateContext ctx) throws MessagingException {

        // ── 1. Evaluate the two thresholds ────────────────────────────────────
        List<ThresholdBreach> breaches = evaluateThresholds(ctx);
        ctx.setThresholdBreaches(breaches);

        // ── 2. Derive status from breaches ────────────────────────────────────
        String status = breaches.isEmpty() ? "PASSED" : "FAILED";
        ctx.setStatus(status);

        // ── 3. Top 10 slowest transactions by avg response time ───────────────
        List<TransactionData> top10Tx = ctx.getTransactionData().stream()
                .sorted(Comparator.comparingLong(tx -> -parseMs(tx.getAvg())))
                .limit(TOP_N)
                .collect(Collectors.toList());

        // ── 4. Top 10 errors by count ─────────────────────────────────────────
        List<ErrorsData> top10Errors = ctx.getErrorsData().stream()
                .sorted(Comparator.comparingInt(ErrorsData::getCount).reversed())
                .limit(TOP_N)
                .collect(Collectors.toList());

        // ── 5. Build Thymeleaf context ────────────────────────────────────────
        Context thCtx = new Context();
        thCtx.setVariable("runId",              ctx.getRunId());
        thCtx.setVariable("testName",           ctx.getTestName());
        thCtx.setVariable("status",             status);
        thCtx.setVariable("startTime",          ctx.getStartTime());
        thCtx.setVariable("endTime",            ctx.getEndTime());
        thCtx.setVariable("duration",           ctx.getDuration());
        thCtx.setVariable("lgInfo",             ctx.getLgInfo());
        thCtx.setVariable("metrics",            ctx.getMetrics());
        thCtx.setVariable("transactions",       top10Tx);
        thCtx.setVariable("errors",             top10Errors);
        thCtx.setVariable("totalTransactions",  ctx.getTotalTransactions());
        thCtx.setVariable("totalErrors",        ctx.getTotalErrors());
        thCtx.setVariable("thresholdBreaches",  breaches);
        thCtx.setVariable("hasBreaches",        !breaches.isEmpty());
        thCtx.setVariable("attachmentFileName", ctx.getAttachmentFileName());
        thCtx.setVariable("logoUrl",            ctx.getLogoUrl());
        thCtx.setVariable("generatedAt",        ctx.getGeneratedAt());

        // ── 6. Render HTML ────────────────────────────────────────────────────
        String html = templateEngine.process("email/loadrunner-report", thCtx);

        // ── 7. Build MIME message ─────────────────────────────────────────────
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("noreply@yourcompany.com");
        helper.setTo(ctx.getRecipients().toArray(new String[0]));

        if (ctx.getCc() != null && !ctx.getCc().isEmpty()) {
            helper.setCc(ctx.getCc().toArray(new String[0]));
        }

        String subject = String.format("[LoadRunner] %s | %s | Run %s",
                "PASSED".equals(status) ? "✅ PASSED" : "❌ FAILED",
                ctx.getTestName(),
                ctx.getRunId());

        helper.setSubject(subject);
        helper.setText(html, true);

        // ── 8. Attach Excel results file ──────────────────────────────────────
        if (ctx.getAttachmentFilePath() != null && !ctx.getAttachmentFilePath().isBlank()) {
            File file = new File(ctx.getAttachmentFilePath());
            if (file.exists() && file.isFile()) {
                String displayName = ctx.getAttachmentFileName() != null
                        ? ctx.getAttachmentFileName()
                        : file.getName();
                helper.addAttachment(displayName, new FileSystemResource(file));
                log.info("Attached: {}", displayName);
            } else {
                log.warn("Attachment not found, skipping: {}", ctx.getAttachmentFilePath());
            }
        }

        mailSender.send(message);
        log.info("Email sent — runId={} status={} breaches={} to={}",
                ctx.getRunId(), status, breaches.size(), ctx.getRecipients());
    }


    // =========================================================================
    // Threshold evaluation — two checks only
    // =========================================================================

    private List<ThresholdBreach> evaluateThresholds(EmailTemplateContext ctx) {
        List<ThresholdBreach> breaches = new ArrayList<>();
        TestThresholds t = ctx.getThresholds();
        if (t == null) return breaches;

        // ── (a) Max failed transactions ───────────────────────────────────────
        if (t.getMaxFailedTransactions() != null) {
            int actual = ctx.getMetrics().getTransactionFailed();
            if (actual > t.getMaxFailedTransactions()) {
                breaches.add(ThresholdBreach.builder()
                        .thresholdName("Max Failed Transactions")
                        .configuredLimit(String.valueOf(t.getMaxFailedTransactions()))
                        .actualValue(String.valueOf(actual))
                        .sentence(String.format(
                                "The number of failed transactions (%d) exceeded the configured "
                                + "threshold of %d, breaching the acceptable failure limit for this run.",
                                actual, t.getMaxFailedTransactions()))
                        .build());
            }
        }

        // ── (b) Max total errors ──────────────────────────────────────────────
        if (t.getMaxTotalErrors() != null) {
            int actual = ctx.getTotalErrors();
            if (actual > t.getMaxTotalErrors()) {
                breaches.add(ThresholdBreach.builder()
                        .thresholdName("Max Total Errors")
                        .configuredLimit(String.valueOf(t.getMaxTotalErrors()))
                        .actualValue(String.valueOf(actual))
                        .sentence(String.format(
                                "The total error count (%d) exceeded the configured threshold of %d. "
                                + "Please review the error log for details on affected scripts and injectors.",
                                actual, t.getMaxTotalErrors()))
                        .build());
            }
        }

        return breaches;
    }


    // ── Helper ────────────────────────────────────────────────────────────────
    private long parseMs(String s) {
        if (s == null || s.isBlank()) return 0L;
        try { return Long.parseLong(s.replaceAll("[^0-9]", "")); }
        catch (NumberFormatException e) { return 0L; }
    }
}