package com.almersoul.llmproxy.repository;

import com.almersoul.llmproxy.domain.ProxyMetric;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProxyMetricRepository extends JpaRepository<ProxyMetric, Long> {

    @Query("select count(m) from ProxyMetric m where m.createdAt >= :from and m.createdAt < :to")
    long countRequestsBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(distinct m.username) from ProxyMetric m where m.createdAt >= :from and m.createdAt < :to")
    long countDistinctUsersBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select count(distinct m.session.id)
            from ProxyMetric m
            where m.createdAt >= :from and m.createdAt < :to
            """)
    long countDistinctSessionsBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(m) from ProxyMetric m where m.createdAt >= :from and m.createdAt < :to and m.statusCode >= 400")
    long countErrorsBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select coalesce(avg(m.latencyMs), 0) from ProxyMetric m where m.createdAt >= :from and m.createdAt < :to")
    Double averageLatencyBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select m.username as label, count(m) as total
            from ProxyMetric m
            where m.createdAt >= :from and m.createdAt < :to
            group by m.username
            order by count(m) desc
            """)
    List<LabelCountView> topUsersBetween(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query("""
            select m.routeName as label, count(m) as total
            from ProxyMetric m
            where m.createdAt >= :from and m.createdAt < :to
            group by m.routeName
            order by count(m) desc
            """)
    List<LabelCountView> topRoutesBetween(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query(
            value = """
                    select m.username as label, count(m) as total
                    from ProxyMetric m
                    where m.createdAt >= :from and m.createdAt < :to
                    group by m.username
                    order by count(m) desc
                    """,
            countQuery = """
                    select count(distinct m.username)
                    from ProxyMetric m
                    where m.createdAt >= :from and m.createdAt < :to
                    """)
    Page<LabelCountView> pageTopUsersBetween(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query(
            value = """
                    select m.routeName as label, count(m) as total
                    from ProxyMetric m
                    where m.createdAt >= :from and m.createdAt < :to
                    group by m.routeName
                    order by count(m) desc
                    """,
            countQuery = """
                    select count(distinct m.routeName)
                    from ProxyMetric m
                    where m.createdAt >= :from and m.createdAt < :to
                    """)
    Page<LabelCountView> pageTopRoutesBetween(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    Page<ProxyMetric> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ProxyMetric> findByPromptPreviewIsNotNullOrderByCreatedAtDesc(Pageable pageable);

    interface LabelCountView {
        String getLabel();
        Long getTotal();
    }
}
