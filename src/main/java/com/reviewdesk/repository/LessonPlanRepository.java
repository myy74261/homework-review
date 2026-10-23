package com.reviewdesk.repository;

import com.reviewdesk.domain.LessonPlan;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 教师备课稿的数据访问接口。
 */
public interface LessonPlanRepository extends JpaRepository<LessonPlan, Long> {
}
