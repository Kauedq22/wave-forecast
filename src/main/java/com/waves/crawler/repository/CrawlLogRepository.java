package com.waves.crawler.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.waves.crawler.model.CrawlLog;
import com.waves.crawler.model.CrawlLog.CrawlStatus;

@Repository
public interface CrawlLogRepository extends JpaRepository<CrawlLog, UUID> {

    List<CrawlLog> findByStatusOrderByStartedAtDesc(CrawlStatus status);

    List<CrawlLog> findByStartedAtAfterOrderByStartedAtDesc(LocalDateTime since);
}
