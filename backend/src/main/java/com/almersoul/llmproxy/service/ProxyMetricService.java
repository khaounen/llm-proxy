package com.almersoul.llmproxy.service;

import com.almersoul.llmproxy.domain.ProxyMetric;
import com.almersoul.llmproxy.domain.ProxyUserSession;
import com.almersoul.llmproxy.dto.ActivityPeriodResponse;
import com.almersoul.llmproxy.dto.AdminDashboardResponse;
import com.almersoul.llmproxy.dto.CountByLabelResponse;
import com.almersoul.llmproxy.dto.PagedResponse;
import com.almersoul.llmproxy.dto.ProxyMetricResponse;
import com.almersoul.llmproxy.dto.ProxySessionResponse;
import com.almersoul.llmproxy.repository.ProxyMetricRepository;
import com.almersoul.llmproxy.repository.ProxyUserSessionRepository;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProxyMetricService {

    private static final Duration SESSION_IDLE_GAP = Duration.ofMinutes(15);
    private static final int MAX_PAGE_SIZE = 200;

    private final ProxyMetricRepository proxyMetricRepository;
    private final ProxyUserSessionRepository proxyUserSessionRepository;

    public ProxyMetricService(
            ProxyMetricRepository proxyMetricRepository,
            ProxyUserSessionRepository proxyUserSessionRepository) {
        this.proxyMetricRepository = proxyMetricRepository;
        this.proxyUserSessionRepository = proxyUserSessionRepository;
    }

    @Async("metricsExecutor")
    @Transactional
    public void recordAsync(
            String username,
            String routeName,
            String incomingPath,
            URI targetUri,
            String method,
            int statusCode,
            long latencyMs,
            String errorMessage,
            String promptPreview) {
        Instant now = Instant.now();
        ProxyUserSession session = resolveSession(username, now);
        session.registerPrompt(now);
        proxyUserSessionRepository.save(session);

        ProxyMetric metric = new ProxyMetric();
        metric.setUsername(username);
        metric.setSession(session);
        metric.setRouteName(routeName);
        metric.setIncomingPath(incomingPath);
        metric.setTargetUrl(targetUri.toString());
        metric.setMethod(method);
        metric.setStatusCode(statusCode);
        metric.setLatencyMs(latencyMs);
        metric.setPromptPreview(promptPreview);
        metric.setErrorMessage(errorMessage);
        metric.setCreatedAt(now);
        proxyMetricRepository.save(metric);
    }

    @Transactional(readOnly = true)
    public List<ProxyMetricResponse> findRecent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 1000));
        return proxyMetricRepository
                .findAll(PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(ProxyMetricResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProxyMetricResponse> findRecentPrompts(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 1000));
        return proxyMetricRepository
                .findByPromptPreviewIsNotNullOrderByCreatedAtDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(ProxyMetricResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProxySessionResponse> findRecentSessions(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 1000));
        return proxyUserSessionRepository
                .findAll(PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "lastPromptAt")))
                .stream()
                .map(ProxySessionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProxyMetricResponse> findRecentPage(int page, int size) {
        PageRequest pageable = buildPage(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ProxyMetricResponse> pageData = proxyMetricRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(ProxyMetricResponse::from);
        return PagedResponse.from(pageData);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProxyMetricResponse> findPromptPage(int page, int size) {
        PageRequest pageable = buildPage(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ProxyMetricResponse> pageData = proxyMetricRepository
                .findByPromptPreviewIsNotNullOrderByCreatedAtDesc(pageable)
                .map(ProxyMetricResponse::from);
        return PagedResponse.from(pageData);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProxySessionResponse> findSessionPage(int page, int size) {
        PageRequest pageable = buildPage(page, size, Sort.by(Sort.Direction.DESC, "lastPromptAt"));
        Page<ProxySessionResponse> pageData = proxyUserSessionRepository
                .findAllByOrderByLastPromptAtDesc(pageable)
                .map(ProxySessionResponse::from);
        return PagedResponse.from(pageData);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CountByLabelResponse> findTopPage(String period, String dimension, int page, int size) {
        Instant now = Instant.now();
        Instant periodStart = resolvePeriodStart(period, now);
        PageRequest pageable = buildPage(page, size, Sort.unsorted());

        Page<ProxyMetricRepository.LabelCountView> pageData;
        if ("consumers".equalsIgnoreCase(dimension)) {
            pageData = proxyMetricRepository.pageTopUsersBetween(periodStart, now, pageable);
        } else if ("routes".equalsIgnoreCase(dimension)) {
            pageData = proxyMetricRepository.pageTopRoutesBetween(periodStart, now, pageable);
        } else {
            throw new IllegalArgumentException("Unsupported top dimension: " + dimension);
        }

        Page<CountByLabelResponse> mapped = pageData
                .map(row -> new CountByLabelResponse(row.getLabel(), row.getTotal() == null ? 0L : row.getTotal()));
        return PagedResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        Instant now = Instant.now();
        ZonedDateTime utcNow = now.atZone(ZoneOffset.UTC);

        Instant dailyStart = LocalDate.from(utcNow).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant monthlyStart = utcNow.withDayOfMonth(1).toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC);

        ActivityPeriodResponse daily = buildPeriod(dailyStart, now);
        ActivityPeriodResponse monthly = buildPeriod(monthlyStart, now);

        return new AdminDashboardResponse(now, daily, monthly);
    }

    private ActivityPeriodResponse buildPeriod(Instant periodStart, Instant periodEnd) {
        long total = proxyMetricRepository.countRequestsBetween(periodStart, periodEnd);
        long uniqueUsers = proxyMetricRepository.countDistinctUsersBetween(periodStart, periodEnd);
        long totalSessions = proxyMetricRepository.countDistinctSessionsBetween(periodStart, periodEnd);
        long errors = proxyMetricRepository.countErrorsBetween(periodStart, periodEnd);
        Double avgLatency = proxyMetricRepository.averageLatencyBetween(periodStart, periodEnd);

        double errorRate = total == 0 ? 0d : (errors * 100d / total);
        double safeLatency = avgLatency == null ? 0d : avgLatency;

        List<CountByLabelResponse> topConsumers = proxyMetricRepository
                .topUsersBetween(periodStart, periodEnd, PageRequest.of(0, 10))
                .stream()
                .map(row -> new CountByLabelResponse(row.getLabel(), row.getTotal() == null ? 0L : row.getTotal()))
                .toList();

        List<CountByLabelResponse> topRoutes = proxyMetricRepository
                .topRoutesBetween(periodStart, periodEnd, PageRequest.of(0, 10))
                .stream()
                .map(row -> new CountByLabelResponse(row.getLabel(), row.getTotal() == null ? 0L : row.getTotal()))
                .toList();

        return new ActivityPeriodResponse(
                periodStart,
                periodEnd,
                total,
                uniqueUsers,
                totalSessions,
                errors,
                errorRate,
                safeLatency,
                topConsumers,
                topRoutes);
    }

    private ProxyUserSession resolveSession(String username, Instant at) {
        ProxyUserSession latestSession = proxyUserSessionRepository
                .findTopByUsernameOrderByLastPromptAtDesc(username)
                .orElse(null);

        if (latestSession == null || latestSession.getLastPromptAt().plus(SESSION_IDLE_GAP).isBefore(at)) {
            ProxyUserSession session = new ProxyUserSession();
            session.start(username, at);
            return session;
        }

        return latestSession;
    }

    private PageRequest buildPage(int page, int size, Sort sort) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        return PageRequest.of(safePage, safeSize, sort);
    }

    private Instant resolvePeriodStart(String period, Instant now) {
        ZonedDateTime utcNow = now.atZone(ZoneOffset.UTC);
        if ("monthly".equalsIgnoreCase(period)) {
            return utcNow.withDayOfMonth(1).toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC);
        }
        if ("daily".equalsIgnoreCase(period)) {
            return LocalDate.from(utcNow).atStartOfDay().toInstant(ZoneOffset.UTC);
        }
        throw new IllegalArgumentException("Unsupported top period: " + period.toLowerCase(Locale.ROOT));
    }
}
