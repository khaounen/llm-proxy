package com.almersoul.llmproxy.repository;

import com.almersoul.llmproxy.domain.ProxyUserSession;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProxyUserSessionRepository extends JpaRepository<ProxyUserSession, Long> {

    Optional<ProxyUserSession> findTopByUsernameOrderByLastPromptAtDesc(String username);

    Page<ProxyUserSession> findAllByOrderByLastPromptAtDesc(Pageable pageable);
}
