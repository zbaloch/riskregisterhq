package com.riskregister.riskregisterapp.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.riskregister.riskregisterapp.entities.Asset;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByDeletedAtIsNullOrderByCreatedAtDesc();
    Optional<Asset> findByIdAndDeletedAtIsNull(Long id);
    long countByDeletedAtIsNull();
}
