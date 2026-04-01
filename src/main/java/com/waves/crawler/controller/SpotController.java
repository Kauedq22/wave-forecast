package com.waves.crawler.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.waves.crawler.model.Forecast;
import com.waves.crawler.model.Spot;
import com.waves.crawler.repository.ForecastRepository;
import com.waves.crawler.repository.SpotRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/spots")
@RequiredArgsConstructor
public class SpotController {

    private final SpotRepository spotRepository;
    private final ForecastRepository forecastRepository;

    @GetMapping
    public List<Spot> listAll() {
        return spotRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Spot> findById(@PathVariable UUID id) {
        return spotRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Spot> create(@Valid @RequestBody Spot spot) {
        Spot saved = spotRepository.save(spot);
        return ResponseEntity.created(URI.create("/api/spots/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Spot> update(@PathVariable UUID id, @Valid @RequestBody Spot body) {
        return spotRepository.findById(id).map(existing -> {
            existing.setName(body.getName());
            existing.setLatitude(body.getLatitude());
            existing.setLongitude(body.getLongitude());
            existing.setRegion(body.getRegion());
            existing.setCoastOrientation(body.getCoastOrientation());
            existing.setSourceUrl(body.getSourceUrl());
            return ResponseEntity.ok(spotRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!spotRepository.existsById(id)) return ResponseEntity.notFound().build();
        spotRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/forecasts")
    public ResponseEntity<List<Forecast>> forecasts(@PathVariable UUID id) {
        return spotRepository.findById(id)
                .map(spot -> ResponseEntity.ok(forecastRepository.findBySpotOrderByForecastTimeAsc(spot)))
                .orElse(ResponseEntity.notFound().build());
    }
}
