package com.almersoul.llmproxy.web;

import com.almersoul.llmproxy.dto.AdminDashboardResponse;
import com.almersoul.llmproxy.dto.ProxyMetricResponse;
import com.almersoul.llmproxy.dto.ProxySessionResponse;
import com.almersoul.llmproxy.dto.CountByLabelResponse;
import com.almersoul.llmproxy.dto.PagedResponse;
import com.almersoul.llmproxy.service.ProxyMetricService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/metrics")
public class AdminMetricController {

    private final ProxyMetricService proxyMetricService;

    public AdminMetricController(ProxyMetricService proxyMetricService) {
        this.proxyMetricService = proxyMetricService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return proxyMetricService.dashboard();
    }

    @GetMapping("/recent")
    public List<ProxyMetricResponse> recent(@RequestParam(defaultValue = "100") int limit) {
        return proxyMetricService.findRecent(limit);
    }

    @GetMapping("/prompts")
    public List<ProxyMetricResponse> recentPrompts(@RequestParam(defaultValue = "100") int limit) {
        return proxyMetricService.findRecentPrompts(limit);
    }

    @GetMapping("/sessions")
    public List<ProxySessionResponse> recentSessions(@RequestParam(defaultValue = "100") int limit) {
        return proxyMetricService.findRecentSessions(limit);
    }

    @GetMapping("/recent/page")
    public PagedResponse<ProxyMetricResponse> recentPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return proxyMetricService.findRecentPage(page, size);
    }

    @GetMapping("/prompts/page")
    public PagedResponse<ProxyMetricResponse> promptPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return proxyMetricService.findPromptPage(page, size);
    }

    @GetMapping("/sessions/page")
    public PagedResponse<ProxySessionResponse> sessionPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return proxyMetricService.findSessionPage(page, size);
    }

    @GetMapping("/top/page")
    public PagedResponse<CountByLabelResponse> topPage(
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(defaultValue = "consumers") String dimension,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return proxyMetricService.findTopPage(period, dimension, page, size);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
