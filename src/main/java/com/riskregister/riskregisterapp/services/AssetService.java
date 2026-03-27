package com.riskregister.riskregisterapp.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.riskregister.riskregisterapp.entities.Asset;
import com.riskregister.riskregisterapp.repositories.AssetRepository;

@Service
public class AssetService {
    @Autowired
    private AssetRepository assetRepository;

    public List<Asset> findAll(Long organizationId) {
        return assetRepository.findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId);
    }

    public Optional<Asset> findById(Long organizationId, Long id) {
        return assetRepository.findByOrganizationIdAndIdAndDeletedAtIsNull(organizationId, id);
    }

    public Asset save(Asset asset) {
        Instant now = Instant.now();
        if (asset.getId() == null) {
            asset.setCreatedAt(now);
        }
        asset.setUpdatedAt(now);
        return assetRepository.save(asset);
    }

    public void softDelete(Long organizationId, Long id) {
        assetRepository.findByOrganizationIdAndIdAndDeletedAtIsNull(organizationId, id).ifPresent(asset -> {
            asset.setDeletedAt(Instant.now());
            assetRepository.save(asset);
        });
    }
}
