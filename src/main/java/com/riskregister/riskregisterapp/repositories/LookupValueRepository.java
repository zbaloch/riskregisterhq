package com.riskregister.riskregisterapp.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.riskregister.riskregisterapp.entities.LookupValue;

@Repository
public interface LookupValueRepository extends JpaRepository<LookupValue, Long> {

    List<LookupValue> findByOrganizationIdAndLookupTypeOrderBySortOrderAscNameAsc(
        Long organizationId, String lookupType);

    Optional<LookupValue> findByOrganizationIdAndLookupTypeAndCode(
        Long organizationId, String lookupType, String code);

    Optional<LookupValue> findByOrganizationIdAndId(Long organizationId, Long id);

    long countByOrganizationIdAndLookupType(Long organizationId, String lookupType);
}
