package com.almersoul.llmproxy.web;

import com.almersoul.llmproxy.dto.RouteConfigRequest;
import com.almersoul.llmproxy.dto.RouteConfigResponse;
import com.almersoul.llmproxy.service.RouteConfigService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/routes")
public class AdminRouteController {

    private final RouteConfigService routeConfigService;

    public AdminRouteController(RouteConfigService routeConfigService) {
        this.routeConfigService = routeConfigService;
    }

    @GetMapping
    public List<RouteConfigResponse> list() {
        return routeConfigService.listAll().stream().map(RouteConfigResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RouteConfigResponse create(@Valid @RequestBody RouteConfigRequest request) {
        try {
            return RouteConfigResponse.from(routeConfigService.create(request));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Route name or prefix already exists", ex);
        }
    }

    @PutMapping("/{id}")
    public RouteConfigResponse update(@PathVariable Long id, @Valid @RequestBody RouteConfigRequest request) {
        try {
            return RouteConfigResponse.from(routeConfigService.update(id, request));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Route name or prefix already exists", ex);
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        routeConfigService.delete(id);
    }
}
