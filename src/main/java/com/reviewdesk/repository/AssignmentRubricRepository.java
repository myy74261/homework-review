package com.reviewdesk.repository;

import com.reviewdesk.domain.AssignmentRubric;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 作业评分量规的数据访问接口。
 */
public interface AssignmentRubricRepository extends JpaRepository<AssignmentRubric, Long> {

    List<AssignmentRubric> findByAssignmentIdOrderByDisplayOrderAsc(Long assignmentId);
}

