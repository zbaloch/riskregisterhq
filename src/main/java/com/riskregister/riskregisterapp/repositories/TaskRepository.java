package com.riskregister.riskregisterapp.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.riskregister.riskregisterapp.entities.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Soft-delete aware queries
    List<Task> findByRiskIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long riskId);
    Optional<Task> findByIdAndDeletedAtIsNull(Long id);
    List<Task> findByDeletedAtIsNullOrderByCreatedAtDesc();
    List<Task> findByAssigneeIdAndDeletedAtIsNullOrderByCreatedAtDesc(String assigneeId);
    List<Task> findByAssigneeId(String assigneeId);

    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.risk WHERE t.assigneeId = :assigneeId AND t.deletedAt IS NULL ORDER BY t.createdAt DESC")
    List<Task> findByAssigneeIdWithRiskAndDeletedAtIsNullOrderByCreatedAtDesc(@Param("assigneeId") String assigneeId);

    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.risk WHERE t.deletedAt IS NULL ORDER BY t.createdAt DESC")
    List<Task> findAllWithRiskAndDeletedAtIsNull();

    // Organization-scoped queries
    List<Task> findByOrganizationIdAndRiskIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long organizationId, Long riskId);
    Optional<Task> findByOrganizationIdAndIdAndDeletedAtIsNull(Long organizationId, Long id);

    @Query("""
        SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.risk
        WHERE t.organizationId = :organizationId AND t.deletedAt IS NULL
        ORDER BY t.createdAt DESC
        """)
    List<Task> findAllWithRiskByOrganizationIdAndDeletedAtIsNull(@Param("organizationId") Long organizationId);

    @Query("""
        SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.risk
        WHERE t.assigneeId = :assigneeId AND t.organizationId = :organizationId AND t.deletedAt IS NULL
        ORDER BY t.createdAt DESC
        """)
    List<Task> findByAssigneeIdAndOrganizationIdWithRiskAndDeletedAtIsNull(
        @Param("assigneeId") String assigneeId, @Param("organizationId") Long organizationId);
}
