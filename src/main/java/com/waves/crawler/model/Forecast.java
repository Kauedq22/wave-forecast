package com.waves.crawler.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "forecasts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"spot_id", "forecast_time"})
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Forecast {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id", nullable = false)
    private Spot spot;

    @Column(name = "forecast_time", nullable = false)
    private LocalDateTime forecastTime; 

    @Column(name = "captured_at")
    private LocalDateTime capturedAt = LocalDateTime.now();

    @Column(name = "wave_height", precision = 4, scale = 2)
    private BigDecimal waveHeight; 

    @Column(name = "wave_period")
    private Integer wavePeriod;

    @Column(name = "wind_speed", precision = 4, scale = 2)
    private BigDecimal windSpeed; 

    @Column(name = "wind_direction", length = 10)
    private String windDirection; 

    @Column(name = "wave_direction", length = 10)
    private String waveDirection; 

    private BigDecimal tideHeight;

    private String tideTrend;
}
