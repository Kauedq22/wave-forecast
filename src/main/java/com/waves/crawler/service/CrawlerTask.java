package com.waves.crawler.service;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.waves.crawler.model.Spot;
import com.waves.crawler.repository.SpotRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerTask {

    private final SpotRepository spotRepository;
    private final ScraperService scraperService;

    @Scheduled(cron = "0 0 6,18 * * *")
    public void runDailyCrawl() {
        log.info("Iniciando crawl agendado...");

        List<Spot> spots = spotRepository.findAll();

        if (spots.isEmpty()) {
            log.warn("Nenhum spot cadastrado no banco. Crawl ignorado.");
            return;
        }

        scraperService.scrapeAll(spots);
    }
}
