package com.reviewdesk.repository;

import com.reviewdesk.domain.Assignment;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findAllByOrderByPublishedAtDesc();

    List<Assignment> findByClassroomIdInOrderByPublishedAtDesc(Collection<Long> classroomIds);

    List<Assignment> findByClassroomIdOrderByPublishedAtDesc(Long classroomId);
}

