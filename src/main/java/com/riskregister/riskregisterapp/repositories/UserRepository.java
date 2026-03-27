package com.riskregister.riskregisterapp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.riskregister.riskregisterapp.entities.User;

public interface UserRepository extends JpaRepository<User, String>{
    public User findByEmail(String email);
    public User findByEmailAndToken(String email, String token);
    public java.util.List<User> findByApprovedTrueOrderByFirstNameAscLastNameAsc();

    // Organization-scoped queries
    public java.util.List<User> findByOrganizationId(Long organizationId);
    public java.util.List<User> findByOrganizationIdOrderByFirstNameAscLastNameAsc(Long organizationId);
    public java.util.List<User> findByOrganizationIdAndApprovedTrueOrderByFirstNameAscLastNameAsc(Long organizationId);
    public long countByOrganizationId(Long organizationId);
}