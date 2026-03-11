package com.riskregister.riskregisterapp.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.riskregister.riskregisterapp.entities.TaskUpdate;

@Repository
public interface TaskUpdateRepository extends JpaRepository<TaskUpdate, Long> {

    List<TaskUpdate> findByTaskIdOrderByCreatedAtAsc(Long taskId);
}
