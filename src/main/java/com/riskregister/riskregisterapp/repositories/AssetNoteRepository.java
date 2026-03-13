package com.riskregister.riskregisterapp.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.riskregister.riskregisterapp.entities.AssetNote;

@Repository
public interface AssetNoteRepository extends JpaRepository<AssetNote, Long> {
    List<AssetNote> findByAssetIdOrderByCreatedAtDesc(Long assetId);
}
