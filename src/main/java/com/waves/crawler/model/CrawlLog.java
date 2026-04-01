package com.waves.crawler.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "crawl_logs")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CrawlLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "spots_crawled")
    private Integer spotsCrawled = 0;

    @Column(name = "records_saved")
    private Integer recordsSaved = 0;

    @Column(name = "error_count")
    private Integer errorCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrawlStatus status;

    public enum CrawlStatus {
        RUNNING, SUCCESS, PARTIAL_FAILURE, FAILURE
    }
}
