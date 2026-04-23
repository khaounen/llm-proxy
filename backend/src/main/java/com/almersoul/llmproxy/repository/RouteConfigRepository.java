package com.almersoul.llmproxy.repository;

import com.almersoul.llmproxy.domain.RouteConfig;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteConfigRepository extends JpaRepository<RouteConfig, Long> {

    List<RouteConfig> findByActiveTrue();
}
