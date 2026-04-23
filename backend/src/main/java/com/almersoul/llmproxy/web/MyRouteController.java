package com.almersoul.llmproxy.web;

import com.almersoul.llmproxy.dto.MyRouteResponse;
import com.almersoul.llmproxy.service.RouteConfigService;
import java.util.Comparator;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/routes")
public class MyRouteController {

    private final RouteConfigService routeConfigService;

    public MyRouteController(RouteConfigService routeConfigService) {
        this.routeConfigService = routeConfigService;
    }

    @GetMapping
    public List<MyRouteResponse> listActive() {
        return routeConfigService.listAll().stream()
                .filter(route -> route.isActive())
                .sorted(Comparator.comparing(route -> route.getIncomingPrefix().length(), Comparator.reverseOrder()))
                .map(MyRouteResponse::from)
                .toList();
    }
}
