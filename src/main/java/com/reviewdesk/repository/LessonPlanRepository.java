package com.reviewdesk.repository;

import com.reviewdesk.domain.LessonPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonPlanRepository extends JpaRepository<LessonPlan, Long> {
}
