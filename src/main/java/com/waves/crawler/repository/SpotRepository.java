package com.waves.crawler.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.waves.crawler.model.Spot;

@Repository
public interface SpotRepository extends JpaRepository<Spot, UUID> {

    Optional<Spot> findByName(String name);

    boolean existsByName(String name);
}
