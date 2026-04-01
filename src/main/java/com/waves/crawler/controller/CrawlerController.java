package com.waves.crawler.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.waves.crawler.model.CrawlLog;
import com.waves.crawler.model.Spot;
import com.waves.crawler.repository.CrawlLogRepository;
import com.waves.crawler.repository.SpotRepository;
import com.waves.crawler.service.ScraperService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
public class CrawlerController {

    private final SpotRepository spotRepository;
    private final ScraperService scraperService;
    private final CrawlLogRepository crawlLogRepository;
    private final RestTemplate restTemplate;


    @PostMapping("/run")
    public ResponseEntity<Map<String, String>> runAll() {
        List<Spot> spots = spotRepository.findAll();
        if (spots.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nenhum spot cadastrado."));
        }
        scraperService.scrapeAll(spots);
        return ResponseEntity.ok(Map.of("message", "Crawl iniciado para " + spots.size() + " spot(s)."));
    }


    @PostMapping("/run/{id}")
    public ResponseEntity<Map<String, String>> runOne(@PathVariable UUID id) {
        return spotRepository.findById(id).map(spot -> {
            int saved = scraperService.scrape(spot);
            String msg = saved >= 0
                    ? "Crawl concluído para: " + spot.getName() + " — " + saved + " previsão(ões) processada(s)."
                    : "Falha ao fazer crawl de: " + spot.getName();
            return ResponseEntity.ok(Map.of("message", msg));
        }).orElse(ResponseEntity.notFound().build());
    }


    @GetMapping("/debug/{id}")
    public ResponseEntity<Map<String, Object>> debug(@PathVariable UUID id) {
        return spotRepository.findById(id).map(spot -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("spot", spot.getName());
            result.put("latitude", spot.getLatitude());
            result.put("longitude", spot.getLongitude());
            try {
                String marineUrl = "https://marine-api.open-meteo.com/v1/marine"
                        + "?latitude=" + spot.getLatitude().toPlainString()
                        + "&longitude=" + spot.getLongitude().toPlainString()
                        + "&hourly=wave_height,wave_period,wave_direction"
                        + "&timezone=America%2FSao_Paulo&forecast_days=1";
                Object marine = restTemplate.getForObject(marineUrl, Object.class);
                result.put("marine_api_ok", true);
                result.put("marine_sample", marine);
            } catch (Exception e) {
                result.put("marine_api_ok", false);
                result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            return ResponseEntity.ok(result);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/logs")
    public List<CrawlLog> logs() {
        return crawlLogRepository.findAll();
    }
}
