package com.reviewdesk.repository;

import com.reviewdesk.domain.Assignment;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 课程作业的数据访问接口。
 */
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByClassroomIdInOrderByPublishedAtDesc(Collection<Long> classroomIds);
}

