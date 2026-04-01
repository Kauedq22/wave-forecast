package com.waves.crawler.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.waves.crawler.model.CrawlLog;
import com.waves.crawler.model.CrawlLog.CrawlStatus;
import com.waves.crawler.model.Forecast;
import com.waves.crawler.model.Spot;
import com.waves.crawler.repository.CrawlLogRepository;
import com.waves.crawler.repository.ForecastRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScraperService {

    private static final DateTimeFormatter OPEN_METEO_FMT = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm")
            .optionalStart().appendPattern(":ss").optionalEnd()
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter();

    private static final String MARINE_URL =
        "https://marine-api.open-meteo.com/v1/marine" +
        "?latitude=%s&longitude=%s" +
        "&hourly=wave_height,wave_period,wave_direction" +
        "&timezone=auto&forecast_days=3";

    private static final String WIND_URL =
        "https://api.open-meteo.com/v1/forecast" +
        "?latitude=%s&longitude=%s" +
        "&hourly=wind_speed_10m,wind_direction_10m" +
        "&wind_speed_unit=kmh&timezone=auto&forecast_days=3";

    private final ForecastRepository forecastRepository;
    private final CrawlLogRepository crawlLogRepository;
    private final RestTemplate restTemplate;

    @Transactional
    public int scrape(Spot spot) {
        if (spot.getLatitude() == null || spot.getLongitude() == null) {
            log.warn("Spot '{}' sem coordenadas. Pulando.", spot.getName());
            return -1;
        }

        try {
            List<Forecast> fetched = fetchFromOpenMeteo(spot);
            return upsertAll(fetched);
        } catch (Exception e) {
            log.error("Erro ao buscar previsão para spot '{}': {}", spot.getName(), e.getMessage(), e);
            return -1;
        }
    }


    @Transactional
    public void scrapeAll(List<Spot> spots) {
        CrawlLog crawlLog = new CrawlLog();
        crawlLog.setStartedAt(LocalDateTime.now());
        crawlLog.setStatus(CrawlStatus.RUNNING);
        crawlLogRepository.save(crawlLog);

        int totalSaved = 0;
        int errors = 0;
        List<String> errorMessages = new ArrayList<>();

        for (Spot spot : spots) {
            int result = scrape(spot);
            if (result >= 0) {
                totalSaved += result;
            } else {
                errors++;
                errorMessages.add("Falhou: " + spot.getName());
            }
        }

        crawlLog.setFinishedAt(LocalDateTime.now());
        crawlLog.setSpotsCrawled(spots.size());
        crawlLog.setRecordsSaved(totalSaved);
        crawlLog.setErrorCount(errors);
        crawlLog.setStatus(errors == 0 ? CrawlStatus.SUCCESS
                : totalSaved == 0 ? CrawlStatus.FAILURE
                : CrawlStatus.PARTIAL_FAILURE);

        if (!errorMessages.isEmpty()) {
            crawlLog.setErrorMessage(String.join("\n", errorMessages));
        }

        crawlLogRepository.save(crawlLog);
        log.info("Crawl finalizado — salvos: {}, erros: {}", totalSaved, errors);
    }


    @SuppressWarnings("unchecked")
    private List<Forecast> fetchFromOpenMeteo(Spot spot) {
        String lat = spot.getLatitude().toPlainString();
        String lon = spot.getLongitude().toPlainString();

        Map<String, Object> marine = restTemplate.getForObject(
                String.format(MARINE_URL, lat, lon), Map.class);
        Map<String, Object> wind = restTemplate.getForObject(
                String.format(WIND_URL, lat, lon), Map.class);

        Map<String, Object> marineHourly = (Map<String, Object>) marine.get("hourly");
        Map<String, Object> windHourly   = (Map<String, Object>) wind.get("hourly");

        List<String> times          = (List<String>) marineHourly.get("time");
        List<Number> waveHeights    = (List<Number>) marineHourly.get("wave_height");
        List<Number> wavePeriods    = (List<Number>) marineHourly.get("wave_period");
        List<Number> waveDirections = (List<Number>) marineHourly.get("wave_direction");
        List<Number> windSpeeds     = (List<Number>) windHourly.get("wind_speed_10m");
        List<Number> windDirs       = (List<Number>) windHourly.get("wind_direction_10m");

        LocalDateTime now = LocalDateTime.now();
        List<Forecast> forecasts = new ArrayList<>();

        for (int i = 0; i < times.size(); i++) {
            LocalDateTime forecastTime = LocalDateTime.parse(times.get(i), OPEN_METEO_FMT);

            if (forecastTime.isBefore(now)) continue;

            Forecast f = new Forecast();
            f.setSpot(spot);
            f.setForecastTime(forecastTime);
            f.setCapturedAt(now);
            f.setWaveHeight(toDecimal(waveHeights.get(i)));
            f.setWavePeriod(toInt(wavePeriods.get(i)));
            f.setWaveDirection(degreesToCardinal(waveDirections.get(i)));
            f.setWindSpeed(toDecimal(windSpeeds.get(i)));
            f.setWindDirection(degreesToCardinal(windDirs.get(i)));
            forecasts.add(f);
        }

        log.info("Open-Meteo retornou {} horas de previsão para '{}'", forecasts.size(), spot.getName());
        return forecasts;
    }

    private int upsertAll(List<Forecast> forecasts) {
        if (forecasts.isEmpty()) return 0;


        Spot spot = forecasts.get(0).getSpot();
        Map<LocalDateTime, Forecast> existing = forecastRepository
                .findBySpotOrderByForecastTimeAsc(spot)
                .stream()
                .collect(Collectors.toMap(Forecast::getForecastTime, f -> f));


        List<Forecast> toSave = new ArrayList<>(forecasts.size());
        for (Forecast f : forecasts) {
            Forecast target = existing.getOrDefault(f.getForecastTime(), f);
            target.setWaveHeight(f.getWaveHeight());
            target.setWavePeriod(f.getWavePeriod());
            target.setWaveDirection(f.getWaveDirection());
            target.setWindSpeed(f.getWindSpeed());
            target.setWindDirection(f.getWindDirection());
            target.setCapturedAt(f.getCapturedAt());
            toSave.add(target);
        }

        // 1 saveAll em vez de N saves individuais (Hibernate agrupa em batch)
        forecastRepository.saveAll(toSave);
        return toSave.size();
    }



    private BigDecimal toDecimal(Number val) {
        if (val == null) return null;
        return new BigDecimal(val.toString());
    }

    private Integer toInt(Number val) {
        if (val == null) return null;
        return val.intValue();
    }


    private String degreesToCardinal(Number degrees) {
        if (degrees == null) return null;
        String[] dirs = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int index = (int) Math.round(degrees.doubleValue() / 45) % 8;
        return dirs[index];
    }
}
