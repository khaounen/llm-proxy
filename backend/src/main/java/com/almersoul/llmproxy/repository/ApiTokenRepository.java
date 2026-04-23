package com.almersoul.llmproxy.repository;

import com.almersoul.llmproxy.domain.ApiToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {

    Optional<ApiToken> findByTokenHash(String tokenHash);

    List<ApiToken> findByIssuerAndSubjectOrderByCreatedAtDesc(String issuer, String subject);

    Optional<ApiToken> findByIdAndIssuerAndSubject(Long id, String issuer, String subject);
}
