package com.waves.crawler.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.waves.crawler.model.Forecast;
import com.waves.crawler.model.Spot;

@Repository
public interface ForecastRepository extends JpaRepository<Forecast, UUID> {

    List<Forecast> findBySpotOrderByForecastTimeAsc(Spot spot);
}
